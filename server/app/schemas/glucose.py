from datetime import datetime
from typing import Literal, Optional
from pydantic import BaseModel


class GlucoseReadingIn(BaseModel):
    external_id: str
    source: Literal["nightscout", "dexcom_share"]
    recorded_at: datetime
    glucose_mgdl: float
    trend: Optional[str] = None
    trend_rate_mgdl_per_min: Optional[float] = None
    device: Optional[str] = None
    noise: Optional[int] = None
    raw_sgv: Optional[int] = None


class GlucoseIngestRequest(BaseModel):
    readings: list[GlucoseReadingIn]


class GlucoseIngestResponse(BaseModel):
    accepted: int
    duplicate_skipped: int
