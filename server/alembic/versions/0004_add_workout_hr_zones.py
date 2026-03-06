"""Add HR zone columns and calories to workouts

Revision ID: 0004
Revises: 0003
Create Date: 2026-03-06 00:00:00.000000
"""
from typing import Sequence, Union
import sqlalchemy as sa
from alembic import op

revision: str = "0004"
down_revision: Union[str, None] = "0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("workouts", sa.Column("avg_hr_bpm", sa.Float(), nullable=True))
    op.add_column("workouts", sa.Column("max_hr_bpm", sa.Float(), nullable=True))
    op.add_column("workouts", sa.Column("calories_active_kcal", sa.Float(), nullable=True))
    op.add_column("workouts", sa.Column("zone1_minutes", sa.Integer(), nullable=True))
    op.add_column("workouts", sa.Column("zone2_minutes", sa.Integer(), nullable=True))
    op.add_column("workouts", sa.Column("zone3_minutes", sa.Integer(), nullable=True))
    op.add_column("workouts", sa.Column("zone4_minutes", sa.Integer(), nullable=True))
    op.add_column("workouts", sa.Column("zone5_minutes", sa.Integer(), nullable=True))


def downgrade() -> None:
    for col in ["zone5_minutes", "zone4_minutes", "zone3_minutes", "zone2_minutes",
                "zone1_minutes", "calories_active_kcal", "max_hr_bpm", "avg_hr_bpm"]:
        op.drop_column("workouts", col)
