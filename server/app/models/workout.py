from datetime import datetime
from typing import Optional
from sqlalchemy import BigInteger, Boolean, DateTime, Float, Index, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column
from app.models.base import Base


class Workout(Base):
    __tablename__ = "workouts"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    external_id: Mapped[str] = mapped_column(String(128), nullable=False, unique=True)
    performed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    inserted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    class_name: Mapped[Optional[str]] = mapped_column(String(256))
    wod_name: Mapped[Optional[str]] = mapped_column(String(256))
    result: Mapped[Optional[str]] = mapped_column(String(256))
    result_type: Mapped[Optional[str]] = mapped_column(String(64))
    score_by_type: Mapped[Optional[float]] = mapped_column(Float)
    rx: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    coach: Mapped[Optional[str]] = mapped_column(String(128))
    location: Mapped[Optional[str]] = mapped_column(String(128))
    duration_minutes: Mapped[Optional[int]] = mapped_column(Integer)
    notes: Mapped[Optional[str]] = mapped_column(Text)

    # HR zone analysis (computed from Health Connect samples during the session)
    avg_hr_bpm: Mapped[Optional[float]] = mapped_column(Float)
    max_hr_bpm: Mapped[Optional[float]] = mapped_column(Float)
    calories_active_kcal: Mapped[Optional[float]] = mapped_column(Float)
    zone1_minutes: Mapped[Optional[int]] = mapped_column(Integer)  # <60% max HR — recovery
    zone2_minutes: Mapped[Optional[int]] = mapped_column(Integer)  # 60-70% — fat burn
    zone3_minutes: Mapped[Optional[int]] = mapped_column(Integer)  # 70-80% — aerobic
    zone4_minutes: Mapped[Optional[int]] = mapped_column(Integer)  # 80-90% — threshold
    zone5_minutes: Mapped[Optional[int]] = mapped_column(Integer)  # ≥90%   — maximum

    __table_args__ = (
        Index("ix_workout_performed", "performed_at"),
    )
