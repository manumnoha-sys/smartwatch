from datetime import datetime
from typing import Optional
from sqlalchemy import BigInteger, DateTime, Index, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column
from app.models.base import Base


class SleepSession(Base):
    __tablename__ = "sleep_sessions"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    external_id: Mapped[str] = mapped_column(String(128), nullable=False, unique=True)
    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    inserted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    duration_minutes: Mapped[Optional[int]] = mapped_column(Integer)
    total_sleep_minutes: Mapped[Optional[int]] = mapped_column(Integer)
    deep_sleep_minutes: Mapped[Optional[int]] = mapped_column(Integer)
    light_sleep_minutes: Mapped[Optional[int]] = mapped_column(Integer)
    rem_sleep_minutes: Mapped[Optional[int]] = mapped_column(Integer)
    awake_minutes: Mapped[Optional[int]] = mapped_column(Integer)
    notes: Mapped[Optional[str]] = mapped_column(Text)

    __table_args__ = (
        Index("ix_sleep_start", "start_time"),
    )
