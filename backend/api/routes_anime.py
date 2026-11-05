from typing import Optional

from fastapi import APIRouter, Query

from core.failover import run_op
from models.anime import AnimeDetail, AnimeListResponse, HomeData
from models.schedule import ScheduleResponse

router = APIRouter(tags=["Anime"])


@router.get("/home", response_model=HomeData)
async def get_home(source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"), strict: bool = Query(False, description="Hanya pakai sumber terpilih")):
    """Beranda: banner, episode terbaru, anime populer. Otomatis pindah sumber bila gagal."""
    return await run_op("home", source=source, strict=strict)


@router.get("/anime/{slug}", response_model=AnimeDetail)
async def get_anime_detail(slug: str, source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"), strict: bool = Query(False, description="Hanya pakai sumber terpilih")):
    """Detail anime (sinopsis, metadata, daftar episode)."""
    return await run_op("detail", slug, source=source, strict=strict)


@router.get("/list", response_model=AnimeListResponse)
async def get_anime_catalog(
    page: int = Query(1, ge=1),
    genre: Optional[str] = Query(None, description="Slug genre, mis. action"),
    source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"),
    strict: bool = Query(False, description="Hanya pakai sumber terpilih"),
):
    return await run_op("list", page=page, genre=genre, source=source, strict=strict)


@router.get("/ongoing", response_model=AnimeListResponse)
async def get_ongoing(page: int = Query(1, ge=1), source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"), strict: bool = Query(False, description="Hanya pakai sumber terpilih")):
    """Anime yang sedang tayang."""
    return await run_op("ongoing", page=page, source=source, strict=strict)


@router.get("/movies", response_model=AnimeListResponse)
async def get_movies(page: int = Query(1, ge=1), source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"), strict: bool = Query(False, description="Hanya pakai sumber terpilih")):
    """Daftar film anime."""
    return await run_op("movies", page=page, source=source, strict=strict)


@router.get("/schedule", response_model=ScheduleResponse)
async def get_schedule(source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"), strict: bool = Query(False, description="Hanya pakai sumber terpilih")):
    """Jadwal tayang per hari."""
    return await run_op("schedule", source=source, strict=strict)
