from typing import Optional

from fastapi import APIRouter, Query

from core.failover import run_op
from models.episode import EpisodeStreamDetail

router = APIRouter(tags=["Episode"])


@router.get("/episode/{slug}", response_model=EpisodeStreamDetail)
async def get_episode_stream(
    slug: str,
    source: Optional[str] = Query(None, description="ID sumber yang diprioritaskan"),
    strict: bool = Query(False, description="Hanya pakai sumber yang dipilih"),
):
    """
    Pemutar episode: server streaming, URL embed, tautan unduhan, dan navigasi episode.
    Bila sumber utama gagal, episode yang sama dicari di sumber lain lewat judul + nomor episode.
    """
    return await run_op("episode", slug, source=source, strict=strict)
