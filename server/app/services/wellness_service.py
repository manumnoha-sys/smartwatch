from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.dialects.postgresql import insert as pg_insert
from app.models.wellness_snapshot import WellnessSnapshot
from app.schemas.wellness import WellnessIngestRequest


async def ingest_wellness(db: AsyncSession, payload: WellnessIngestRequest) -> dict:
    rows = [{"device_id": payload.device_id, **s.model_dump()} for s in payload.snapshots]
    stmt = pg_insert(WellnessSnapshot).values(rows)
    stmt = stmt.on_conflict_do_nothing(constraint="uq_wellness_device_time")
    result = await db.execute(stmt)
    await db.commit()
    accepted = result.rowcount
    return {"accepted": accepted, "duplicate_skipped": len(rows) - accepted}
