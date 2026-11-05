# [KATALOG] main
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from core.errors import AppError, app_error_handler, generic_exception_handler
from db.database import init_db
from api.routes_anime import router as anime_router
from api.routes_episode import router as episode_router
from api.routes_search import router as search_router
from api.routes_auth import router as auth_router
from api.routes_user import router as user_router

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger("main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting up NeetRust Backend Service...")
    await init_db()
    yield
    logger.info("Shutting down NeetRust Backend Service...")

app = FastAPI(
    title="NeetRust Anime API",
    description="Backend REST API Samehadaku Scraper & Auth Service untuk aplikasi NeetRust",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register uniform error handlers
app.add_exception_handler(AppError, app_error_handler)
app.add_exception_handler(Exception, generic_exception_handler)

# Mount API Routers
app.include_router(anime_router)
app.include_router(episode_router)
app.include_router(search_router)
app.include_router(auth_router)
app.include_router(user_router)

@app.get("/", tags=["Health"])
async def root():
    return {
        "app": "NeetRust Backend API",
        "status": "online",
        "docs": "/docs"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
