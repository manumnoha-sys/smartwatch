from datetime import date, datetime
from typing import Optional
from pydantic import BaseModel
from app.schemas.workout import WorkoutIn


class HealthSnapshot(BaseModel):
    as_of: datetime
    window_minutes: int
    latest_heart_rate_bpm: Optional[float]
    latest_spo2_percent: Optional[float]
    steps_in_window: Optional[int]
    calories_in_window: Optional[float]
    latest_glucose_mgdl: Optional[float]
    glucose_trend: Optional[str]
    last_workout_performed_at: Optional[datetime]
    last_workout_result: Optional[str]


class DailySummary(BaseModel):
    date: date
    timezone: str
    avg_heart_rate_bpm: Optional[float]
    min_heart_rate_bpm: Optional[float]
    max_heart_rate_bpm: Optional[float]
    avg_spo2_percent: Optional[float]
    min_spo2_percent: Optional[float]
    total_steps: Optional[int]
    total_calories_kcal: Optional[float]
    avg_glucose_mgdl: Optional[float]
    min_glucose_mgdl: Optional[float]
    max_glucose_mgdl: Optional[float]
    time_in_range_percent: Optional[float]  # 70-180 mg/dL
    workout_count: int
    workouts: list[WorkoutIn]
