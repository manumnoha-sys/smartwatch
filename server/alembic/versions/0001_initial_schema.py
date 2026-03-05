"""Initial schema: watch_readings, glucose_readings, workouts

Revision ID: 0001
Revises:
Create Date: 2025-08-01 00:00:00.000000
"""
from typing import Sequence, Union
import sqlalchemy as sa
from alembic import op

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # --- watch_readings ---
    op.create_table(
        "watch_readings",
        sa.Column("id", sa.BigInteger(), autoincrement=True, nullable=False),
        sa.Column("device_id", sa.String(64), nullable=False),
        sa.Column("recorded_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("inserted_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("heart_rate_bpm", sa.Float(), nullable=True),
        sa.Column("heart_rate_accuracy", sa.SmallInteger(), nullable=True),
        sa.Column("spo2_percent", sa.Float(), nullable=True),
        sa.Column("spo2_accuracy", sa.SmallInteger(), nullable=True),
        sa.Column("steps_total", sa.Integer(), nullable=True),
        sa.Column("calories_kcal", sa.Float(), nullable=True),
        sa.Column("accel_x", sa.Float(), nullable=True),
        sa.Column("accel_y", sa.Float(), nullable=True),
        sa.Column("accel_z", sa.Float(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("device_id", "recorded_at", name="uq_watch_device_time"),
    )
    op.create_index("ix_watch_device_recorded", "watch_readings", ["device_id", "recorded_at"])

    # --- glucose_readings ---
    # Use VARCHAR for source — enum validation handled at the Python/Pydantic layer
    op.create_table(
        "glucose_readings",
        sa.Column("id", sa.BigInteger(), autoincrement=True, nullable=False),
        sa.Column("external_id", sa.String(128), nullable=False),
        sa.Column("source", sa.String(32), nullable=False),
        sa.Column("recorded_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("inserted_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("glucose_mgdl", sa.Float(), nullable=False),
        sa.Column("trend", sa.String(32), nullable=True),
        sa.Column("trend_rate_mgdl_per_min", sa.Float(), nullable=True),
        sa.Column("device", sa.String(64), nullable=True),
        sa.Column("noise", sa.SmallInteger(), nullable=True),
        sa.Column("raw_sgv", sa.Integer(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("external_id"),
    )
    op.create_index("ix_glucose_recorded", "glucose_readings", ["recorded_at"])

    # --- workouts ---
    op.create_table(
        "workouts",
        sa.Column("id", sa.BigInteger(), autoincrement=True, nullable=False),
        sa.Column("external_id", sa.String(128), nullable=False),
        sa.Column("performed_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("inserted_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("class_name", sa.String(256), nullable=True),
        sa.Column("wod_name", sa.String(256), nullable=True),
        sa.Column("result", sa.String(256), nullable=True),
        sa.Column("result_type", sa.String(64), nullable=True),
        sa.Column("score_by_type", sa.Float(), nullable=True),
        sa.Column("rx", sa.Boolean(), nullable=False),
        sa.Column("coach", sa.String(128), nullable=True),
        sa.Column("location", sa.String(128), nullable=True),
        sa.Column("duration_minutes", sa.Integer(), nullable=True),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("external_id"),
    )
    op.create_index("ix_workout_performed", "workouts", ["performed_at"])


def downgrade() -> None:
    op.drop_index("ix_workout_performed", table_name="workouts")
    op.drop_table("workouts")

    op.drop_index("ix_glucose_recorded", table_name="glucose_readings")
    op.drop_table("glucose_readings")

    op.drop_index("ix_watch_device_recorded", table_name="watch_readings")
    op.drop_table("watch_readings")
