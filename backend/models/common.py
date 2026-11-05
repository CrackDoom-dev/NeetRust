from typing import Any, Generic, List, Optional, TypeVar
from pydantic import BaseModel

T = TypeVar("T")

class BaseResponse(BaseModel, Generic[T]):
    success: bool = True
    message: str = "Sukses"
    data: Optional[T] = None

class ErrorResponse(BaseModel):
    error: str
    code: str
    detail: Optional[dict[str, Any]] = None

class PaginationMeta(BaseModel):
    current_page: int = 1
    has_next: bool = False
    total_pages: Optional[int] = None
