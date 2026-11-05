# [KATALOG] users.models
from typing import Optional
from pydantic import BaseModel, EmailStr

class GoogleAuthRequest(BaseModel):
    id_token: str

class TokenRefreshRequest(BaseModel):
    refresh_token: str

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    user: "UserProfile"

class UserProfile(BaseModel):
    id: str
    email: str
    name: str
    bio: str = ""
    avatar_url: str = ""

class UserProfileUpdate(BaseModel):
    name: Optional[str] = None
    bio: Optional[str] = None
    avatar_url: Optional[str] = None

class FavoriteRequest(BaseModel):
    anime_slug: str
    title: str
    poster: Optional[str] = ""
    score: Optional[str] = "N/A"
    status: Optional[str] = "Ongoing"

class FavoriteItem(BaseModel):
    id: int
    anime_slug: str
    title: str
    poster: Optional[str] = ""
    score: Optional[str] = "N/A"
    status: Optional[str] = "Ongoing"
    created_at: str

class HistoryRequest(BaseModel):
    anime_slug: str
    anime_title: str
    episode_slug: str
    episode_number: str
    poster: Optional[str] = ""
    last_position_seconds: int = 0
    duration_seconds: int = 0

class HistoryItem(BaseModel):
    id: int
    anime_slug: str
    anime_title: str
    episode_slug: str
    episode_number: str
    poster: Optional[str] = ""
    last_position_seconds: int
    duration_seconds: int
    updated_at: str

TokenResponse.model_rebuild()
