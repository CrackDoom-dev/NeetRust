from fastapi import APIRouter
from models.episode import EpisodeStreamDetail
from scrapers.samehadaku.episode import scrape_episode_stream

router = APIRouter(tags=["Episode"])

@router.get("/episode/{slug}", response_model=EpisodeStreamDetail)
async def get_episode_stream(slug: str):
    """
    Mengambil data pemutar video episode: daftar server streaming,
    URL pemutar embed / direct, opsi kualitas, tautan unduhan, dan navigasi episode sebelumnya/berikutnya.
    """
    return await scrape_episode_stream(slug)
