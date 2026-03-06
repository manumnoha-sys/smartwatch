"""Add wellness_snapshots table and new columns to watch_readings

Revision ID: 0003
Revises: 0002
Create Date: 2026-03-06 00:00:00.000000
"""
from typing import Sequence, Union
import sqlalchemy as sa
from alembic import op

revision: str = "0003"
down_revision: Union[str, None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # New columns on watch_readings
    op.add_column("watch_readings", sa.Column("active_calories_kcal", sa.Float(), nullable=True))
    op.add_column("watch_readings", sa.Column("distance_meters", sa.Float(), nullable=True))
    op.add_column("watch_readings", sa.Column("floors_climbed", sa.Float(), nullable=True))

    # New wellness_snapshots table
    op.create_table(
        "wellness_snapshots",
        sa.Column("id", sa.BigInteger(), autoincrement=True, nullable=False),
        sa.Column("device_id", sa.String(64), nullable=False),
        sa.Column("recorded_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("inserted_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("resting_hr_bpm", sa.Float(), nullable=True),
        sa.Column("hrv_rmssd_ms", sa.Float(), nullable=True),
        sa.Column("vo2_max", sa.Float(), nullable=True),
        sa.Column("respiratory_rate_brpm", sa.Float(), nullable=True),
        sa.Column("skin_temp_celsius", sa.Float(), nullable=True),
        sa.Column("weight_kg", sa.Float(), nullable=True),
        sa.Column("body_fat_percent", sa.Float(), nullable=True),
        sa.Column("bmr_kcal", sa.Float(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("device_id", "recorded_at", name="uq_wellness_device_time"),
    )
    op.create_index("ix_wellness_device_recorded", "wellness_snapshots", ["device_id", "recorded_at"])


def downgrade() -> None:
    op.drop_index("ix_wellness_device_recorded", table_name="wellness_snapshots")
    op.drop_table("wellness_snapshots")
    op.drop_column("watch_readings", "floors_climbed")
    op.drop_column("watch_readings", "distance_meters")
    op.drop_column("watch_readings", "active_calories_kcal")
