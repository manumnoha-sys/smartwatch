import enum
from datetime import datetime
from typing import Optional
from sqlalchemy import BigInteger, DateTime, Float, Index, Integer, SmallInteger, String, func
from sqlalchemy.orm import Mapped, mapped_column
from app.models.base import Base


class GlucoseSource(str, enum.Enum):
    nightscout = "nightscout"
    dexcom_share = "dexcom_share"


class GlucoseReading(Base):
    __tablename__ = "glucose_readings"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    external_id: Mapped[str] = mapped_column(String(128), nullable=False, unique=True)
    source: Mapped[str] = mapped_column(String(32), nullable=False)
    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    inserted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    glucose_mgdl: Mapped[float] = mapped_column(Float, nullable=False)
    trend: Mapped[Optional[str]] = mapped_column(String(32))
    trend_rate_mgdl_per_min: Mapped[Optional[float]] = mapped_column(Float)
    device: Mapped[Optional[str]] = mapped_column(String(64))
    noise: Mapped[Optional[int]] = mapped_column(SmallInteger)
    raw_sgv: Mapped[Optional[int]] = mapped_column(Integer)

    __table_args__ = (
        Index("ix_glucose_recorded", "recorded_at"),
    )
