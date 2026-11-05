from typing import List, Optional
from pydantic import BaseModel

class VideoServer(BaseModel):
    server_id: str
    server_name: str
    stream_url: str
    quality: str = "720p"
    is_embed: bool = True

class DownloadLink(BaseModel):
    quality: str
    host_name: str
    url: str

class EpisodeStreamDetail(BaseModel):
    title: str
    episode_slug: str
    anime_slug: str
    anime_title: Optional[str] = None
    episode_number: str
    default_stream_url: Optional[str] = None
    servers: List[VideoServer] = []
    download_links: List[DownloadLink] = []
    prev_episode_slug: Optional[str] = None
    next_episode_slug: Optional[str] = None
    source: Optional[str] = None
