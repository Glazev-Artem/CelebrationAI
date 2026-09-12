import secrets
import pyotp
from passlib.context import CryptContext
import os

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def append_to_env():
    # Прочитаем текущий .env чтобы не дублировать
    env_content = ""
    if os.path.exists(".env"):
        with open(".env", "r", encoding="utf-8") as f:
            env_content = f.read()

    # Генерируем секреты
    if "JWT_SECRET=" not in env_content:
        jwt_secret = secrets.token_hex(32)
        env_content += f"\nJWT_SECRET={jwt_secret}\n"
        
    if "TOTP_SECRET=" not in env_content:
        totp_secret = pyotp.random_base32()
        env_content += f"TOTP_SECRET={totp_secret}\n"
        
    if "ADMIN_PASSWORD_HASH=" not in env_content:
        # Пароль по умолчанию
        hashed = pwd_context.hash("ChangeMe_AdminPassword_2026!")
        env_content += f"ADMIN_PASSWORD_HASH={hashed}\n"

    with open(".env", "w", encoding="utf-8") as f:
        f.write(env_content)
        
    print("Secrets generated successfully!")

if __name__ == "__main__":
    append_to_env()
