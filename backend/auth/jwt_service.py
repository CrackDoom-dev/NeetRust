import datetime
from typing import Optional
from jose import JWTError, jwt
from core.config import settings
from core.errors import UnauthorizedError

def create_access_token(user_id: str, email: str) -> str:
    expires = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    payload = {
        "sub": user_id,
        "email": email,
        "type": "access",
        "exp": expires
    }
    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)

def create_refresh_token(user_id: str, email: str) -> str:
    expires = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    payload = {
        "sub": user_id,
        "email": email,
        "type": "refresh",
        "exp": expires
    }
    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)

def verify_token(token: str, expected_type: str = "access") -> dict:
    try:
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
        token_type = payload.get("type")
        if token_type != expected_type:
            raise UnauthorizedError("Tipe token tidak sesuai")
        return payload
    except JWTError as e:
        raise UnauthorizedError(f"Token tidak valid atau telah kedaluwarsa: {str(e)}")
