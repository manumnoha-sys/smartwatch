from datetime import datetime
from typing import Optional
from pydantic import BaseModel, field_validator


class SensorReadingIn(BaseModel):
    recorded_at: datetime
    heart_rate_bpm: Optional[float] = None
    heart_rate_accuracy: Optional[int] = None
    spo2_percent: Optional[float] = None
    spo2_accuracy: Optional[int] = None
    steps_total: Optional[int] = None
    calories_kcal: Optional[float] = None
    active_calories_kcal: Optional[float] = None
    distance_meters: Optional[float] = None
    floors_climbed: Optional[float] = None
    accel_x: Optional[float] = None
    accel_y: Optional[float] = None
    accel_z: Optional[float] = None


class WatchIngestRequest(BaseModel):
    device_id: str
    readings: list[SensorReadingIn]

    @field_validator("readings")
    @classmethod
    def max_batch(cls, v: list) -> list:
        if len(v) > 500:
            raise ValueError("batch too large, max 500 readings per request")
        return v


class WatchIngestResponse(BaseModel):
    accepted: int
    duplicate_skipped: int
