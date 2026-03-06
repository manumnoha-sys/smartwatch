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
    avg_hr_bpm: Optional[float] = None
    max_hr_bpm: Optional[float] = None
    calories_active_kcal: Optional[float] = None
    zone1_minutes: Optional[int] = None
    zone2_minutes: Optional[int] = None
    zone3_minutes: Optional[int] = None
    zone4_minutes: Optional[int] = None
    zone5_minutes: Optional[int] = None


class WorkoutIngestRequest(BaseModel):
    workouts: list[WorkoutIn]


class WorkoutIngestResponse(BaseModel):
    accepted: int
    updated: int
    duplicate_skipped: int
