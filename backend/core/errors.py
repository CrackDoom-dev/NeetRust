from fastapi import Request
from fastapi.responses import JSONResponse
import logging

logger = logging.getLogger("core.errors")

class AppError(Exception):
    def __init__(self, message: str, code: str = "ERROR", status_code: int = 400, detail: dict = None):
        self.message = message
        self.code = code
        self.status_code = status_code
        self.detail = detail or {}
        super().__init__(self.message)

class NotFoundError(AppError):
    def __init__(self, message: str = "Data tidak ditemukan"):
        super().__init__(message=message, code="NOT_FOUND", status_code=404)

class UnauthorizedError(AppError):
    def __init__(self, message: str = "Autentikasi gagal atau token tidak valid"):
        super().__init__(message=message, code="UNAUTHORIZED", status_code=401)

class ScrapingError(AppError):
    def __init__(self, message: str = "Gagal mengambil data dari penyedia sumber"):
        super().__init__(message=message, code="SCRAPING_FAILED", status_code=502)

async def app_error_handler(request: Request, exc: AppError) -> JSONResponse:
    logger.warning(f"Handled error: {exc.code} - {exc.message} on {request.url.path}")
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": exc.message,
            "code": exc.code,
            "detail": exc.detail
        }
    )

async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.exception(f"Unhandled exception on {request.url.path}: {str(exc)}")
    return JSONResponse(
        status_code=500,
        content={
            "error": "Terjadi kesalahan internal pada server",
            "code": "INTERNAL_SERVER_ERROR",
            "detail": {"message": str(exc)}
        }
    )
