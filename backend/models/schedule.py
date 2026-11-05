from typing import List, Optional
from pydantic import BaseModel

class ScheduleItem(BaseModel):
    title: str
    slug: str
    poster: str = ""
    time: Optional[str] = None
    episode: Optional[str] = None
    type: Optional[str] = None
    score: Optional[str] = None

class ScheduleDay(BaseModel):
    day: str
    items: List[ScheduleItem] = []

class ScheduleResponse(BaseModel):
    days: List[ScheduleDay] = []
    source: Optional[str] = None
