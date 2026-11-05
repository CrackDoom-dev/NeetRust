from __future__ import annotations

from abc import ABC, abstractmethod
from models.anime import AnimeDetail, AnimeListResponse, HomeData, AnimeCard
from models.episode import EpisodeStreamDetail
from models.schedule import ScheduleResponse

class BaseSource(ABC):
    """Kontrak yang harus dipenuhi setiap sumber website."""
    id: str
    name: str

    @abstractmethod
    async def home(self) -> HomeData: ...

    @abstractmethod
    async def search(self, query: str) -> list[AnimeCard]: ...

    @abstractmethod
    async def list(self, page: int = 1, genre: str | None = None) -> AnimeListResponse: ...

    @abstractmethod
    async def detail(self, slug: str) -> AnimeDetail: ...

    @abstractmethod
    async def episode(self, slug: str) -> EpisodeStreamDetail: ...

    @abstractmethod
    async def schedule(self) -> ScheduleResponse: ...

    @abstractmethod
    async def ongoing(self, page: int = 1) -> AnimeListResponse: ...

    @abstractmethod
    async def movies(self, page: int = 1) -> AnimeListResponse: ...
