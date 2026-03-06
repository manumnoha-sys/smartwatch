"""Add sleep_sessions table

Revision ID: 0002
Revises: 0001
Create Date: 2026-03-06 00:00:00.000000
"""
from typing import Sequence, Union
import sqlalchemy as sa
from alembic import op

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "sleep_sessions",
        sa.Column("id", sa.BigInteger(), autoincrement=True, nullable=False),
        sa.Column("external_id", sa.String(128), nullable=False),
        sa.Column("start_time", sa.DateTime(timezone=True), nullable=False),
        sa.Column("end_time", sa.DateTime(timezone=True), nullable=False),
        sa.Column("inserted_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("duration_minutes", sa.Integer(), nullable=True),
        sa.Column("total_sleep_minutes", sa.Integer(), nullable=True),
        sa.Column("deep_sleep_minutes", sa.Integer(), nullable=True),
        sa.Column("light_sleep_minutes", sa.Integer(), nullable=True),
        sa.Column("rem_sleep_minutes", sa.Integer(), nullable=True),
        sa.Column("awake_minutes", sa.Integer(), nullable=True),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("external_id"),
    )
    op.create_index("ix_sleep_start", "sleep_sessions", ["start_time"])


def downgrade() -> None:
    op.drop_index("ix_sleep_start", table_name="sleep_sessions")
    op.drop_table("sleep_sessions")
