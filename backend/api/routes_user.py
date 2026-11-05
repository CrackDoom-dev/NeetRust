# [KATALOG] api.routes_user
from fastapi import APIRouter, Depends, Header
import aiosqlite
import logging
from db.database import get_db
from auth.jwt_service import verify_token
from core.errors import UnauthorizedError
from users.models import (
    UserProfile, UserProfileUpdate,
    FavoriteRequest, FavoriteItem,
    HistoryRequest, HistoryItem
)
from users import service as user_service

logger = logging.getLogger("api.routes_user")
router = APIRouter(prefix="/users", tags=["Users"])

async def get_current_user_id(authorization: str = Header(None)) -> str:
    if not authorization or not authorization.startswith("Bearer "):
        raise UnauthorizedError("Header Authorization (Bearer token) diperlukan")
    token = authorization.split(" ")[1]
    payload = verify_token(token, expected_type="access")
    return payload["sub"]

@router.get("/me", response_model=UserProfile)
async def get_my_profile(
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Ambil informasi profil pengguna yang sedang login."""
    return await user_service.get_user_by_id(db, user_id)

@router.put("/me", response_model=UserProfile)
async def update_my_profile(
    update_data: UserProfileUpdate,
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Perbarui nama, bio, atau avatar pengguna."""
    return await user_service.update_user_profile(db, user_id, update_data)

@router.get("/me/favorites", response_model=list[FavoriteItem])
async def list_favorites(
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Ambil daftar anime yang difavoritkan pengguna."""
    return await user_service.get_favorites(db, user_id)

@router.post("/me/favorites")
async def add_to_favorites(
    req: FavoriteRequest,
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Tambahkan anime ke daftar favorit."""
    return await user_service.add_favorite(db, user_id, req)

@router.delete("/me/favorites/{anime_slug}")
async def remove_from_favorites(
    anime_slug: str,
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Hapus anime dari daftar favorit."""
    return await user_service.remove_favorite(db, user_id, anime_slug)

@router.get("/me/history", response_model=list[HistoryItem])
async def list_history(
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Ambil riwayat tontonan pengguna beserta posisi tonton terakhir."""
    return await user_service.get_history(db, user_id)

@router.post("/me/history")
async def save_watch_history(
    req: HistoryRequest,
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Simpan atau perbarui posisi tontonan episode."""
    return await user_service.save_history(db, user_id, req)

@router.delete("/me/history")
async def clear_all_history(
    user_id: str = Depends(get_current_user_id),
    db: aiosqlite.Connection = Depends(get_db)
):
    """Bersihkan seluruh riwayat menonton pengguna."""
    return await user_service.clear_history(db, user_id)
