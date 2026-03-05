from datetime import datetime
from typing import Optional
from sqlalchemy import BigInteger, DateTime, Float, Index, Integer, SmallInteger, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column
from app.models.base import Base


class WatchReading(Base):
    __tablename__ = "watch_readings"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(String(64), nullable=False)
    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    inserted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    heart_rate_bpm: Mapped[Optional[float]] = mapped_column(Float)
    heart_rate_accuracy: Mapped[Optional[int]] = mapped_column(SmallInteger)
    spo2_percent: Mapped[Optional[float]] = mapped_column(Float)
    spo2_accuracy: Mapped[Optional[int]] = mapped_column(SmallInteger)
    steps_total: Mapped[Optional[int]] = mapped_column(Integer)
    calories_kcal: Mapped[Optional[float]] = mapped_column(Float)
    accel_x: Mapped[Optional[float]] = mapped_column(Float)
    accel_y: Mapped[Optional[float]] = mapped_column(Float)
    accel_z: Mapped[Optional[float]] = mapped_column(Float)

    __table_args__ = (
        UniqueConstraint("device_id", "recorded_at", name="uq_watch_device_time"),
        Index("ix_watch_device_recorded", "device_id", "recorded_at"),
    )
