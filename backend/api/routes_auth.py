# [KATALOG] api.routes_auth
from fastapi import APIRouter, Depends
import aiosqlite
import logging
from db.database import get_db
from auth.google_verify import verify_google_token
from auth.jwt_service import create_access_token, create_refresh_token, verify_token
from users.models import GoogleAuthRequest, TokenRefreshRequest, TokenResponse
from users.service import get_or_create_user, get_user_by_id

logger = logging.getLogger("api.routes_auth")
router = APIRouter(prefix="/auth", tags=["Auth"])

@router.post("/google", response_model=TokenResponse)
async def auth_google(payload: GoogleAuthRequest, db: aiosqlite.Connection = Depends(get_db)):
    """Verifikasi Google ID token, daftarkan atau login pengguna, dan kembalikan token JWT."""
    user_info = await verify_google_token(payload.id_token)
    user = await get_or_create_user(db, user_info)
    
    access_token = create_access_token(user.id, user.email)
    refresh_token = create_refresh_token(user.id, user.email)
    
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="Bearer",
        user=user
    )

@router.post("/refresh")
async def auth_refresh(payload: TokenRefreshRequest, db: aiosqlite.Connection = Depends(get_db)):
    """Perbarui access token menggunakan refresh token yang valid."""
    decoded = verify_token(payload.refresh_token, expected_type="refresh")
    user_id = decoded["sub"]
    user = await get_user_by_id(db, user_id)
    
    new_access_token = create_access_token(user.id, user.email)
    return {
        "access_token": new_access_token,
        "token_type": "Bearer"
    }
