from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel


class SleepSessionIn(BaseModel):
    external_id: str
    start_time: datetime
    end_time: datetime
    duration_minutes: Optional[int] = None
    total_sleep_minutes: Optional[int] = None
    deep_sleep_minutes: Optional[int] = None
    light_sleep_minutes: Optional[int] = None
    rem_sleep_minutes: Optional[int] = None
    awake_minutes: Optional[int] = None
    notes: Optional[str] = None


class SleepIngestRequest(BaseModel):
    sessions: List[SleepSessionIn]


class SleepIngestResponse(BaseModel):
    accepted: int
    updated: int


class SleepSessionOut(BaseModel):
    external_id: str
    start_time: datetime
    end_time: datetime
    duration_minutes: Optional[int]
    total_sleep_minutes: Optional[int]
    deep_sleep_minutes: Optional[int]
    light_sleep_minutes: Optional[int]
    rem_sleep_minutes: Optional[int]
    awake_minutes: Optional[int]
    notes: Optional[str]

    model_config = {"from_attributes": True}
