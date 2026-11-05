from typing import Optional

from fastapi import APIRouter, Query

from core.failover import run_op
from models.anime import AnimeCard

router = APIRouter(tags=["Search"])


@router.get("/search", response_model=list[AnimeCard])
async def search_anime_endpoint(
    q: str = Query(..., min_length=1, description="Kata kunci pencarian anime"),
    source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"),
    strict: bool = Query(False, description="Hanya pakai sumber yang dipilih"),
):
    """Cari anime berdasarkan judul."""
    return await run_op("search", q, source=source, strict=strict)
