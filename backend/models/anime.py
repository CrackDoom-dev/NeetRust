from typing import List, Optional
from pydantic import BaseModel
from models.common import PaginationMeta

class AnimeCard(BaseModel):
    title: str
    slug: str
    poster: str
    type: Optional[str] = "TV"
    status: Optional[str] = "Ongoing"
    score: Optional[str] = "N/A"
    latest_episode: Optional[str] = None
    release_date: Optional[str] = None

class HomeBanner(BaseModel):
    title: str
    slug: str
    poster: str
    genres: List[str] = []
    synopsis: Optional[str] = None

class LatestEpisodeCard(BaseModel):
    title: str
    episode_slug: str
    anime_slug: Optional[str] = None
    poster: str
    episode_number: str
    release_time: str

class HomeData(BaseModel):
    banners: List[HomeBanner] = []
    latest_episodes: List[LatestEpisodeCard] = []
    popular_anime: List[AnimeCard] = []

class EpisodeListItem(BaseModel):
    title: str
    slug: str
    number: str
    uploaded_at: Optional[str] = None

class AnimeDetail(BaseModel):
    title: str
    slug: str
    poster: str
    synopsis: str
    japanese_title: Optional[str] = None
    type: Optional[str] = "TV"
    status: Optional[str] = "Ongoing"
    score: Optional[str] = "N/A"
    total_episodes: Optional[str] = "?"
    duration: Optional[str] = "24 min"
    season: Optional[str] = None
    studios: Optional[str] = None
    genres: List[str] = []
    episodes: List[EpisodeListItem] = []

class AnimeListResponse(BaseModel):
    items: List[AnimeCard]
    pagination: PaginationMeta
