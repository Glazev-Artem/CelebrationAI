import os
import httpx
import asyncio
import firebase_admin
import pyotp
import jwt
import uuid
import time
import hashlib
from datetime import datetime, timedelta, timezone
from fastapi import FastAPI, Header, HTTPException, Request, Depends, Form, Response
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from firebase_admin import credentials, auth
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, String, Boolean, Integer
from sqlalchemy.orm import sessionmaker, declarative_base
from passlib.context import CryptContext
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from config import settings

# --- DEVSECOPS: SETUP & RATE LIMITING ---
API_SECRET = "CelebrationAI_Secure_Salt_2026"
limiter = Limiter(key_func=get_remote_address)
app = FastAPI()
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

templates = Jinja2Templates(directory="templates")
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# --- DEVSECOPS: DATABASE (Anti-SQLi ORM) ---
DB_URL = "sqlite:///./celebration.db"
engine = create_engine(DB_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()

class DBConfig(Base):
    __tablename__ = "server_configs"
    key = Column(String, primary_key=True)
    value = Column(String)

class DBUser(Base):
    __tablename__ = "users"
    user_id = Column(String, primary_key=True)
    is_premium = Column(Boolean, default=False)

Base.metadata.create_all(bind=engine)

def get_config(key, default):
    with SessionLocal() as db:
        conf = db.query(DBConfig).filter_by(key=key).first()
        if not conf:
            db.add(DBConfig(key=key, value=default))
            db.commit()
            return default
        return conf.value

def set_config(key, value):
    with SessionLocal() as db:
        conf = db.query(DBConfig).filter_by(key=key).first()
        if conf:
            conf.value = value
        else:
            db.add(DBConfig(key=key, value=value))
        db.commit()

DEFAULT_PROMPT = """Ты — гениальный копирайтер и эксперт по подаркам с 20-летним стажем. Твоя единственная цель — создавать персонализированные, живые и захватывающие поздравления.

КРИТИЧЕСКИЕ ПРАВИЛА:
1. АНАЛИЗ ВХОДНЫХ ДАННЫХ: Внимательно изучи профессию, хобби, возраст и имя человека из запроса. Вплетай их в текст органично, без банальностей.
2. 100% ПЕРСОНАЛИЗАЦИЯ: Запрещено использовать клише («желаю счастья, здоровья», «пусть сбудутся все мечты»). Каждое поздравление должно быть уникальным и строиться вокруг увлечений и профессии именинника.
3. ТОНАЛЬНОСТЬ: Строго соблюдай запрошенный тон (романтичный, дерзкий, деловой, черный юмор).
4. ФОРМАТИРОВАНИЕ: Отвечай строго в соответствии с инструкциями пользователя.
5. ИДЕИ ДЛЯ ПОДАРКОВ: Если пользователь просит идеи для подарка, предлагай только то, что связано с его хобби или профессией. Никаких носков или кружек. Предлагай 3 конкретные, креативные идеи.

ОБЯЗАТЕЛЬНО соблюдай все эти правила при генерации!
"""
get_config("system_prompt", DEFAULT_PROMPT)

if os.path.exists("firebase_key.json"):
    cred = credentials.Certificate("firebase_key.json")
    firebase_admin.initialize_app(cred)
else:
    print("WARNING: firebase_key.json not found. Firebase Auth will fail if used.")

# --- DEVSECOPS: AUTH & SESSIONS ---
def create_jwt_token(data: dict):
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(hours=24)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.JWT_SECRET.get_secret_value(), algorithm="HS256")

def verify_jwt_token(token: str):
    try:
        payload = jwt.decode(token, settings.JWT_SECRET.get_secret_value(), algorithms=["HS256"])
        return payload
    except jwt.PyJWTError:
        return None

def verify_admin_session(request: Request):
    token = request.cookies.get("admin_token")
    if not token:
        raise HTTPException(status_code=401, detail="Unauthorized")
    payload = verify_jwt_token(token)
    if not payload or payload.get("sub") != "admin":
        raise HTTPException(status_code=401, detail="Unauthorized")
    return True

async def verify_user(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        return "anonymous_no_token"
    
    token = authorization.split("Bearer ")[1]
    try:
        decoded_token = auth.verify_id_token(token)
        return decoded_token["uid"]
    except Exception as e:
        print(f"Auth Exception: {e}")
        return "anonymous_invalid_token"

# --- DEVSECOPS: INPUT VALIDATION ---
class GenerateRequest(BaseModel):
    user_prompt: str
    app_user_id: str
    invoice_id: str | None = None
    timestamp: int | None = None
    signature: str | None = None

async def verify_rustore_subscription(invoice_id: str) -> bool:
    if not invoice_id:
        return False
    try:
        timestamp = datetime.now(timezone.utc)
        payload = {
            "iss": settings.RUSTORE_COMPANY_ID,
            "sub": settings.RUSTORE_COMPANY_ID,
            "iat": int(timestamp.timestamp()),
            "exp": int((timestamp + timedelta(minutes=5)).timestamp()),
            "jti": str(uuid.uuid4())
        }
        private_key = settings.RUSTORE_PRIVATE_KEY.get_secret_value().replace("\\n", "\n")
        
        token = jwt.encode(payload, private_key, algorithm="RS512", headers={"kid": settings.RUSTORE_KEY_ID.get_secret_value()})
        
        url = f"https://public-api.rustore.ru/public/v2/subscriptions/{invoice_id}"
        headers = {"Authorization": f"Bearer {token}"}
        
        async with httpx.AsyncClient() as client:
            response = await client.get(url, headers=headers, timeout=10.0)
            if response.status_code == 200:
                data = response.json()
                return data.get("body", {}).get("state") == "ACTIVE"
    except Exception as e:
        print(f"RuStore Error: {e}")
    return False

# --- API ENDPOINTS ---
@app.post("/api/generate")
async def generate_celebration(req: GenerateRequest, uid: str = Depends(verify_user)):
    if req.invoice_id:
        is_active = await verify_rustore_subscription(req.invoice_id)
        if not is_active:
            raise HTTPException(status_code=403, detail="Подписка не найдена или неактивна")
    else:
        if not req.timestamp or not req.signature:
            raise HTTPException(status_code=403, detail="Требуется подписка или просмотр рекламы (отсутствует подпись)")
        
        current_time = int(time.time())
        if abs(current_time - req.timestamp) > 120:
            raise HTTPException(status_code=403, detail="Запрос устарел (защита от replay-атак)")
            
        expected_sig = hashlib.sha256(f"{req.app_user_id}_{req.timestamp}_{API_SECRET}".encode()).hexdigest()
        if req.signature != expected_sig:
            raise HTTPException(status_code=403, detail="Неверная подпись")

    prompt = get_config("system_prompt", DEFAULT_PROMPT)
    model = get_config("ai_model", "gemini-2.5-flash")

    full_prompt = f"{prompt}\nПользователь запросил: {req.user_prompt}"

    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={settings.GOOGLE_API_KEY.get_secret_value()}"
    headers = {"Content-Type": "application/json"}
    payload = {
        "contents": [{"parts": [{"text": full_prompt}]}]
    }

    proxy = settings.PROXY_URL if settings.PROXY_URL else None
    async with httpx.AsyncClient(proxy=proxy) as client:
        try:
            response = await client.post(url, json=payload, headers=headers, timeout=30.0)
            if response.status_code == 200:
                data = response.json()
                text = data.get("candidates", [{}])[0].get("content", {}).get("parts", [{}])[0].get("text", "")
                return {"result": text}
            else:
                error_msg = f"Gemini API Error {response.status_code}: {response.text}"
                print(error_msg) # Выводим в лог сервера
                raise HTTPException(status_code=500, detail=f"Ошибка API Gemini: {response.status_code}")
        except httpx.RequestError as e:
            print(f"Network error while connecting to Gemini: {str(e)}")
            raise HTTPException(status_code=500, detail="Ошибка сети при обращении к ИИ")

@app.post("/api/verify_subscription")
async def verify_subscription(uid: str = Depends(verify_user)):
    # Здесь логика проверки в RuStore (оставлена как заглушка, так как RuStore API S2S требует сложной логики)
    # В реальном приложении нужно делать запрос к RuStore API
    with SessionLocal() as db:
        user = db.query(DBUser).filter_by(user_id=uid).first()
        if not user:
            user = DBUser(user_id=uid, is_premium=False)
            db.add(user)
        else:
            user.is_premium = True
        db.commit()
    return {"status": "ok", "is_premium": True}

# --- DEVSECOPS: ADMIN PANEL ---
@app.get("/admin", response_class=HTMLResponse)
def admin_page(request: Request):
    token = request.cookies.get("admin_token")
    if not token or not verify_jwt_token(token):
        return templates.TemplateResponse("login.html", {"request": request})
    
    with SessionLocal() as db:
        total_users = db.query(DBUser).count()
        premium_users = db.query(DBUser).filter_by(is_premium=True).count()

    prompt = get_config("system_prompt", DEFAULT_PROMPT)
    model = get_config("ai_model", "gemini-2.5-flash")
    
    stats = {
        "total_users": total_users,
        "premium_users": premium_users,
        "total_celebrations": 0 # Заглушка до внедрения синхронизации SQLite в Android
    }
    
    return templates.TemplateResponse("admin.html", {
        "request": request, 
        "prompt": prompt, 
        "model": model,
        "stats": stats
    })

@app.post("/admin/login")
@limiter.limit("5/minute")
async def admin_login(request: Request, response: Response, password: str = Form(...), totp: str = Form(...)):
    # Проверка пароля через passlib
    if not pwd_context.verify(password, settings.ADMIN_PASSWORD_HASH):
        await asyncio.sleep(2.0) # Anti-bruteforce delay
        return templates.TemplateResponse("login.html", {"request": request, "error": "Неверный пароль или код"})

    # Проверка TOTP
    totp_verifier = pyotp.TOTP(settings.TOTP_SECRET.get_secret_value())
    if not totp_verifier.verify(totp):
        await asyncio.sleep(2.0) # Anti-bruteforce delay
        return templates.TemplateResponse("login.html", {"request": request, "error": "Неверный код 2FA"})

    # Успешный логин -> Создание защищенной сессии
    token = create_jwt_token({"sub": "admin"})
    response = RedirectResponse(url="/admin", status_code=303)
    response.set_cookie(
        key="admin_token",
        value=token,
        httponly=True,
        secure=True,
        samesite="strict",
        max_age=86400 # 24 часа
    )
    return response

@app.get("/admin/logout")
def admin_logout(response: Response):
    response = RedirectResponse(url="/admin", status_code=303)
    response.delete_cookie("admin_token")
    return response

@app.post("/admin/save")
def admin_save(request: Request, prompt: str = Form(...), model: str = Form(...)):
    verify_admin_session(request)
    set_config("system_prompt", prompt)
    set_config("ai_model", model)
    return RedirectResponse(url="/admin", status_code=303)
