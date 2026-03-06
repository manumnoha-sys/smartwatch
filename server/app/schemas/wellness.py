from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel


class WellnessSnapshotIn(BaseModel):
    recorded_at: datetime
    resting_hr_bpm: Optional[float] = None
    hrv_rmssd_ms: Optional[float] = None
    vo2_max: Optional[float] = None
    respiratory_rate_brpm: Optional[float] = None
    skin_temp_celsius: Optional[float] = None
    weight_kg: Optional[float] = None
    body_fat_percent: Optional[float] = None
    bmr_kcal: Optional[float] = None


class WellnessIngestRequest(BaseModel):
    device_id: str
    snapshots: List[WellnessSnapshotIn]


class WellnessIngestResponse(BaseModel):
    accepted: int
    duplicate_skipped: int


class WellnessSnapshotOut(BaseModel):
    recorded_at: datetime
    resting_hr_bpm: Optional[float]
    hrv_rmssd_ms: Optional[float]
    vo2_max: Optional[float]
    respiratory_rate_brpm: Optional[float]
    skin_temp_celsius: Optional[float]
    weight_kg: Optional[float]
    body_fat_percent: Optional[float]
    bmr_kcal: Optional[float]

    model_config = {"from_attributes": True}
