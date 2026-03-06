from datetime import datetime, timedelta, timezone
from typing import Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy import Float, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.security import require_api_key
from app.models.watch_reading import WatchReading
from app.models.glucose_reading import GlucoseReading
from app.models.workout import Workout
from app.models.sleep_session import SleepSession
from app.models.wellness_snapshot import WellnessSnapshot
from app.schemas.query import HealthSnapshot, DailySummary
from app.schemas.workout import WorkoutIn
from app.schemas.sleep import SleepSessionOut
from app.schemas.wellness import WellnessSnapshotOut

router = APIRouter(
    prefix="/health",
    tags=["query"],
    dependencies=[Depends(require_api_key)],
)


@router.get("/wellness", response_model=list[WellnessSnapshotOut])
async def get_wellness(
    days: int = Query(default=7, ge=1, le=90),
    device_id: str = Query(default="samsung-health-phone"),
    db: AsyncSession = Depends(get_db),
):
    since = datetime.now(timezone.utc) - timedelta(days=days)
    result = await db.execute(
        select(WellnessSnapshot)
        .where(WellnessSnapshot.device_id == device_id, WellnessSnapshot.recorded_at >= since)
        .order_by(WellnessSnapshot.recorded_at.desc())
    )
    return [WellnessSnapshotOut.model_validate(s.__dict__) for s in result.scalars().all()]


@router.get("/sleep", response_model=list[SleepSessionOut])
async def get_sleep(
    days: int = Query(default=7, ge=1, le=90),
    db: AsyncSession = Depends(get_db),
):
    since = datetime.now(timezone.utc) - timedelta(days=days)
    result = await db.execute(
        select(SleepSession)
        .where(SleepSession.start_time >= since)
        .order_by(SleepSession.start_time.desc())
    )
    return [SleepSessionOut.model_validate(s.__dict__) for s in result.scalars().all()]


@router.get("/snapshot", response_model=HealthSnapshot)
async def get_snapshot(
    window_minutes: int = Query(default=60, ge=5, le=1440),
    db: AsyncSession = Depends(get_db),
):
    now = datetime.now(timezone.utc)
    since = now - timedelta(minutes=window_minutes)

    # Latest heart rate
    hr_row = await db.execute(
        select(WatchReading.heart_rate_bpm)
        .where(WatchReading.recorded_at >= since, WatchReading.heart_rate_bpm.isnot(None))
        .order_by(WatchReading.recorded_at.desc())
        .limit(1)
    )
    latest_hr = hr_row.scalar()

    # Latest SpO2
    spo2_row = await db.execute(
        select(WatchReading.spo2_percent)
        .where(WatchReading.recorded_at >= since, WatchReading.spo2_percent.isnot(None))
        .order_by(WatchReading.recorded_at.desc())
        .limit(1)
    )
    latest_spo2 = spo2_row.scalar()

    # Steps in window (max - min of cumulative counter)
    steps_row = await db.execute(
        select(
            func.max(WatchReading.steps_total) - func.min(WatchReading.steps_total)
        ).where(WatchReading.recorded_at >= since, WatchReading.steps_total.isnot(None))
    )
    steps_in_window = steps_row.scalar()

    # Calories in window
    cal_row = await db.execute(
        select(
            func.max(WatchReading.calories_kcal) - func.min(WatchReading.calories_kcal)
        ).where(WatchReading.recorded_at >= since, WatchReading.calories_kcal.isnot(None))
    )
    cal_in_window = cal_row.scalar()

    # Latest glucose
    glucose_row = await db.execute(
        select(GlucoseReading.glucose_mgdl, GlucoseReading.trend)
        .where(GlucoseReading.recorded_at >= since)
        .order_by(GlucoseReading.recorded_at.desc())
        .limit(1)
    )
    glucose_data = glucose_row.first()

    # Last workout
    workout_row = await db.execute(
        select(Workout.performed_at, Workout.result)
        .order_by(Workout.performed_at.desc())
        .limit(1)
    )
    workout_data = workout_row.first()

    return HealthSnapshot(
        as_of=now,
        window_minutes=window_minutes,
        latest_heart_rate_bpm=latest_hr,
        latest_spo2_percent=latest_spo2,
        steps_in_window=steps_in_window,
        calories_in_window=cal_in_window,
        latest_glucose_mgdl=glucose_data.glucose_mgdl if glucose_data else None,
        glucose_trend=glucose_data.trend if glucose_data else None,
        last_workout_performed_at=workout_data.performed_at if workout_data else None,
        last_workout_result=workout_data.result if workout_data else None,
    )


@router.get("/summary/daily", response_model=DailySummary)
async def get_daily_summary(
    date: str = Query(description="YYYY-MM-DD"),
    tz: str = Query(default="UTC"),
    db: AsyncSession = Depends(get_db),
):
    from zoneinfo import ZoneInfo
    from datetime import date as date_type

    local_tz = ZoneInfo(tz)
    day = date_type.fromisoformat(date)
    day_start = datetime(day.year, day.month, day.day, tzinfo=local_tz)
    day_end = day_start + timedelta(days=1)

    # Heart rate stats
    hr = await db.execute(
        select(
            func.avg(WatchReading.heart_rate_bpm),
            func.min(WatchReading.heart_rate_bpm),
            func.max(WatchReading.heart_rate_bpm),
        ).where(
            WatchReading.recorded_at >= day_start,
            WatchReading.recorded_at < day_end,
            WatchReading.heart_rate_bpm.isnot(None),
        )
    )
    hr_avg, hr_min, hr_max = hr.first()

    # SpO2 stats
    spo2 = await db.execute(
        select(
            func.avg(WatchReading.spo2_percent),
            func.min(WatchReading.spo2_percent),
        ).where(
            WatchReading.recorded_at >= day_start,
            WatchReading.recorded_at < day_end,
            WatchReading.spo2_percent.isnot(None),
        )
    )
    spo2_avg, spo2_min = spo2.first()

    # Steps (cumulative counter delta)
    steps = await db.execute(
        select(
            func.max(WatchReading.steps_total) - func.min(WatchReading.steps_total)
        ).where(
            WatchReading.recorded_at >= day_start,
            WatchReading.recorded_at < day_end,
            WatchReading.steps_total.isnot(None),
        )
    )
    total_steps = steps.scalar()

    # Calories
    cals = await db.execute(
        select(
            func.max(WatchReading.calories_kcal) - func.min(WatchReading.calories_kcal)
        ).where(
            WatchReading.recorded_at >= day_start,
            WatchReading.recorded_at < day_end,
            WatchReading.calories_kcal.isnot(None),
        )
    )
    total_cals = cals.scalar()

    # Glucose stats
    gluc = await db.execute(
        select(
            func.avg(GlucoseReading.glucose_mgdl),
            func.min(GlucoseReading.glucose_mgdl),
            func.max(GlucoseReading.glucose_mgdl),
            func.count().filter(
                GlucoseReading.glucose_mgdl.between(70, 180)
            ).cast(Float) / func.nullif(func.count(), 0) * 100,
        ).where(
            GlucoseReading.recorded_at >= day_start,
            GlucoseReading.recorded_at < day_end,
        )
    )
    g_avg, g_min, g_max, tir = gluc.first()

    # Workouts
    workouts_result = await db.execute(
        select(Workout).where(
            Workout.performed_at >= day_start,
            Workout.performed_at < day_end,
        ).order_by(Workout.performed_at)
    )
    workouts = workouts_result.scalars().all()

    # Sleep sessions for the day
    sleep_result = await db.execute(
        select(SleepSession).where(
            SleepSession.start_time >= day_start,
            SleepSession.start_time < day_end,
        ).order_by(SleepSession.start_time)
    )
    sleep_sessions = sleep_result.scalars().all()

    return DailySummary(
        date=day,
        timezone=tz,
        avg_heart_rate_bpm=round(hr_avg, 1) if hr_avg else None,
        min_heart_rate_bpm=hr_min,
        max_heart_rate_bpm=hr_max,
        avg_spo2_percent=round(spo2_avg, 1) if spo2_avg else None,
        min_spo2_percent=spo2_min,
        total_steps=total_steps,
        total_calories_kcal=round(total_cals, 1) if total_cals else None,
        avg_glucose_mgdl=round(g_avg, 1) if g_avg else None,
        min_glucose_mgdl=g_min,
        max_glucose_mgdl=g_max,
        time_in_range_percent=round(tir, 1) if tir else None,
        workout_count=len(workouts),
        workouts=[WorkoutIn.model_validate(w.__dict__) for w in workouts],
        sleep_sessions=[SleepSessionOut.model_validate(s.__dict__) for s in sleep_sessions],
    )
