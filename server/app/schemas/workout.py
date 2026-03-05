from datetime import datetime
from typing import Optional
from pydantic import BaseModel


class WorkoutIn(BaseModel):
    external_id: str
    performed_at: datetime
    class_name: Optional[str] = None
    wod_name: Optional[str] = None
    result: Optional[str] = None
    result_type: Optional[str] = None
    score_by_type: Optional[float] = None
    rx: bool = False
    coach: Optional[str] = None
    location: Optional[str] = None
    duration_minutes: Optional[int] = None
    notes: Optional[str] = None


class WorkoutIngestRequest(BaseModel):
    workouts: list[WorkoutIn]


class WorkoutIngestResponse(BaseModel):
    accepted: int
    updated: int
    duplicate_skipped: int
