from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.dialects.postgresql import insert as pg_insert
from app.models.sleep_session import SleepSession
from app.schemas.sleep import SleepIngestRequest


async def ingest_sleep(db: AsyncSession, payload: SleepIngestRequest) -> dict:
    rows = [s.model_dump() for s in payload.sessions]
    stmt = pg_insert(SleepSession).values(rows)
    stmt = stmt.on_conflict_do_update(
        index_elements=["external_id"],
        set_={
            "end_time": stmt.excluded.end_time,
            "duration_minutes": stmt.excluded.duration_minutes,
            "total_sleep_minutes": stmt.excluded.total_sleep_minutes,
            "deep_sleep_minutes": stmt.excluded.deep_sleep_minutes,
            "light_sleep_minutes": stmt.excluded.light_sleep_minutes,
            "rem_sleep_minutes": stmt.excluded.rem_sleep_minutes,
            "awake_minutes": stmt.excluded.awake_minutes,
            "notes": stmt.excluded.notes,
            "updated_at": stmt.excluded.updated_at,
        },
    )
    result = await db.execute(stmt)
    await db.commit()
    return {"accepted": len(rows), "updated": result.rowcount}
