from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.dialects.postgresql import insert as pg_insert
from app.models.workout import Workout
from app.schemas.workout import WorkoutIngestRequest


async def ingest_workouts(db: AsyncSession, payload: WorkoutIngestRequest) -> dict:
    rows = [w.model_dump() for w in payload.workouts]
    stmt = pg_insert(Workout).values(rows)
    stmt = stmt.on_conflict_do_update(
        index_elements=["external_id"],
        set_={
            "result": stmt.excluded.result,
            "result_type": stmt.excluded.result_type,
            "score_by_type": stmt.excluded.score_by_type,
            "rx": stmt.excluded.rx,
            "notes": stmt.excluded.notes,
            "updated_at": stmt.excluded.updated_at,
        },
    )
    result = await db.execute(stmt)
    await db.commit()
    # rowcount = inserts + updates; we can't distinguish without a second query
    return {"accepted": len(rows), "updated": result.rowcount, "duplicate_skipped": 0}
