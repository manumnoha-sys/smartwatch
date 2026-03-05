from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.dialects.postgresql import insert as pg_insert
from app.models.glucose_reading import GlucoseReading
from app.schemas.glucose import GlucoseIngestRequest


async def ingest_glucose(db: AsyncSession, payload: GlucoseIngestRequest) -> dict:
    rows = [r.model_dump() for r in payload.readings]
    stmt = pg_insert(GlucoseReading).values(rows)
    stmt = stmt.on_conflict_do_nothing(index_elements=["external_id"])
    result = await db.execute(stmt)
    await db.commit()
    accepted = result.rowcount
    return {"accepted": accepted, "duplicate_skipped": len(rows) - accepted}
