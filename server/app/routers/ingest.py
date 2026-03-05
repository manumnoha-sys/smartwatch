from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.security import require_api_key
from app.schemas.watch import WatchIngestRequest, WatchIngestResponse
from app.schemas.glucose import GlucoseIngestRequest, GlucoseIngestResponse
from app.schemas.workout import WorkoutIngestRequest, WorkoutIngestResponse
from app.services import watch_service, glucose_service, workout_service

router = APIRouter(
    prefix="/ingest",
    tags=["ingest"],
    dependencies=[Depends(require_api_key)],
)


@router.post("/watch", response_model=WatchIngestResponse)
async def ingest_watch(payload: WatchIngestRequest, db: AsyncSession = Depends(get_db)):
    result = await watch_service.ingest_watch_readings(db, payload)
    return WatchIngestResponse(**result)


@router.post("/glucose", response_model=GlucoseIngestResponse)
async def ingest_glucose(payload: GlucoseIngestRequest, db: AsyncSession = Depends(get_db)):
    result = await glucose_service.ingest_glucose(db, payload)
    return GlucoseIngestResponse(**result)


@router.post("/workout", response_model=WorkoutIngestResponse)
async def ingest_workout(payload: WorkoutIngestRequest, db: AsyncSession = Depends(get_db)):
    result = await workout_service.ingest_workouts(db, payload)
    return WorkoutIngestResponse(**result)
