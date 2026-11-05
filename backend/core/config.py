import os
from pydantic import BaseModel
from dotenv import load_dotenv

load_dotenv()

class Settings(BaseModel):

    SAMEHADAKU_BASE_URL: str = os.getenv("SAMEHADAKU_BASE_URL", "https://samehadaku.email")

    JWT_SECRET_KEY: str = os.getenv("JWT_SECRET_KEY", "neetrust-ultra-secure-secret-key-change-in-production-2026")
    JWT_ALGORITHM: str = os.getenv("JWT_ALGORITHM", "HS256")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "60"))
    REFRESH_TOKEN_EXPIRE_DAYS: int = int(os.getenv("REFRESH_TOKEN_EXPIRE_DAYS", "30"))

    GOOGLE_CLIENT_ID: str = os.getenv("GOOGLE_CLIENT_ID", "")

    SQLITE_DB_PATH: str = os.getenv("SQLITE_DB_PATH", "neetrust.db")

    CACHE_DEFAULT_TTL: int = int(os.getenv("CACHE_DEFAULT_TTL", "300"))

settings = Settings()
