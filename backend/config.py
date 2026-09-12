from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import SecretStr

class Settings(BaseSettings):
    # API Keys
    GOOGLE_API_KEY: SecretStr
    RUSTORE_KEY_ID: SecretStr
    RUSTORE_COMPANY_ID: str
    RUSTORE_PRIVATE_KEY: SecretStr
    
    # Security
    ADMIN_PASSWORD_HASH: str
    JWT_SECRET: SecretStr
    TOTP_SECRET: SecretStr
    
    # Proxy Configuration
    PROXY_URL: str | None = None
    
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

# Инициализируем настройки при старте
settings = Settings()
