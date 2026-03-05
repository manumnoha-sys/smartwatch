from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.dialects.postgresql import insert as pg_insert
from app.models.watch_reading import WatchReading
from app.schemas.watch import WatchIngestRequest


async def ingest_watch_readings(db: AsyncSession, payload: WatchIngestRequest) -> dict:
    rows = [
        {
            "device_id": payload.device_id,
            "recorded_at": r.recorded_at,
            "heart_rate_bpm": r.heart_rate_bpm,
            "heart_rate_accuracy": r.heart_rate_accuracy,
            "spo2_percent": r.spo2_percent,
            "spo2_accuracy": r.spo2_accuracy,
            "steps_total": r.steps_total,
            "calories_kcal": r.calories_kcal,
            "accel_x": r.accel_x,
            "accel_y": r.accel_y,
            "accel_z": r.accel_z,
        }
        for r in payload.readings
    ]
    stmt = pg_insert(WatchReading).values(rows)
    stmt = stmt.on_conflict_do_nothing(constraint="uq_watch_device_time")
    result = await db.execute(stmt)
    await db.commit()
    accepted = result.rowcount
    return {"accepted": accepted, "duplicate_skipped": len(rows) - accepted}
