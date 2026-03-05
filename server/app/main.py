from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.core.config import settings
from app.core.database import engine
from app.models import Base
from app.routers import ingest, query


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Create tables on startup if they don't exist (alembic is the canonical path)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    await engine.dispose()


app = FastAPI(
    title="Health Dashboard API",
    version="1.0.0",
    description="Ingests and queries Galaxy Watch, CGM glucose, and CrossFit workout data.",
    lifespan=lifespan,
)

app.include_router(ingest.router)
app.include_router(query.router)


@app.get("/healthz", tags=["ops"], include_in_schema=False)
async def healthz():
    return {"status": "ok"}
