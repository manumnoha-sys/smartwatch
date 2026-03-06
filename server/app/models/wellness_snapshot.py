from datetime import datetime
from typing import Optional
from sqlalchemy import BigInteger, DateTime, Float, Index, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column
from app.models.base import Base


class WellnessSnapshot(Base):
    __tablename__ = "wellness_snapshots"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(String(64), nullable=False)
    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    inserted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    resting_hr_bpm: Mapped[Optional[float]] = mapped_column(Float)
    hrv_rmssd_ms: Mapped[Optional[float]] = mapped_column(Float)
    vo2_max: Mapped[Optional[float]] = mapped_column(Float)
    respiratory_rate_brpm: Mapped[Optional[float]] = mapped_column(Float)
    skin_temp_celsius: Mapped[Optional[float]] = mapped_column(Float)
    weight_kg: Mapped[Optional[float]] = mapped_column(Float)
    body_fat_percent: Mapped[Optional[float]] = mapped_column(Float)
    bmr_kcal: Mapped[Optional[float]] = mapped_column(Float)

    __table_args__ = (
        UniqueConstraint("device_id", "recorded_at", name="uq_wellness_device_time"),
        Index("ix_wellness_device_recorded", "device_id", "recorded_at"),
    )
