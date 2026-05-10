from datetime import datetime, date
from decimal import Decimal
from sqlalchemy import ForeignKey, String, Enum, Date, DECIMAL, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT
from sqlalchemy.orm import Mapped, mapped_column
from .base import Base


FINE_TYPE_VALUES = ("late_return", "lost_copy", "damage", "other")
PAID_STATUS_VALUES = ("unpaid", "partial", "paid", "waived")


class Fine(Base):
    __tablename__ = "fines"

    fine_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    loan_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("loans.loan_id", ondelete="CASCADE"),
        nullable=False,
    )
    member_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("members.member_id", ondelete="RESTRICT"),
        nullable=False,
    )
    fine_type: Mapped[str] = mapped_column(
        Enum(*FINE_TYPE_VALUES, name="fine_type"), nullable=False
    )
    fine_amount: Mapped[Decimal] = mapped_column(DECIMAL(12, 2), nullable=False)
    assessed_date: Mapped[date] = mapped_column(Date, nullable=False)
    paid_status: Mapped[str] = mapped_column(
        Enum(*PAID_STATUS_VALUES, name="fine_paid_status"),
        nullable=False,
        server_default=text("'unpaid'"),
    )
    note: Mapped[str | None] = mapped_column(String(500))
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP,
        nullable=False,
        server_default=text("CURRENT_TIMESTAMP"),
        server_onupdate=text("CURRENT_TIMESTAMP"),
    )
