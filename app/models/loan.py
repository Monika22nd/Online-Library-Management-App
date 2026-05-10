from datetime import datetime, date
from decimal import Decimal
from sqlalchemy import ForeignKey, String, Enum, Date, DECIMAL, TIMESTAMP, Index, text
from sqlalchemy.dialects.mysql import BIGINT, TINYINT
from sqlalchemy.orm import Mapped, mapped_column
from .base import Base


APPROVAL_STATUS_VALUES = ("PENDING", "APPROVED", "REJECTED", "RETURNED", "CANCELLED")
LOAN_STATUS_VALUES = ("open", "returned", "overdue", "lost")


class Loan(Base):
    __tablename__ = "loans"
    __table_args__ = (
        Index("idx_loans_member", "member_id"),
        Index("idx_loans_copy", "copy_id"),
        Index("idx_loans_requested_book", "requested_book_id"),
        Index("idx_loans_approval_status", "approval_status"),
    )

    loan_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    member_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("members.member_id", ondelete="RESTRICT"),
        nullable=False,
    )
    copy_id: Mapped[int | None] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("copies.copy_id", ondelete="RESTRICT"),
    )
    requested_book_id: Mapped[int | None] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("books.book_id", ondelete="SET NULL"),
    )
    request_date: Mapped[date] = mapped_column(
        Date, nullable=False, server_default=text("(CURRENT_DATE)")
    )
    borrow_date: Mapped[date | None] = mapped_column(Date)
    due_date: Mapped[date | None] = mapped_column(Date)
    return_date: Mapped[date | None] = mapped_column(Date)
    renew_count: Mapped[int] = mapped_column(
        TINYINT(unsigned=True), nullable=False, server_default=text("0")
    )
    approval_status: Mapped[str] = mapped_column(
        Enum(*APPROVAL_STATUS_VALUES, name="loan_approval_status"),
        nullable=False,
        server_default=text("'PENDING'"),
    )
    loan_status: Mapped[str] = mapped_column(
        Enum(*LOAN_STATUS_VALUES, name="loan_physical_status"),
        nullable=False,
        server_default=text("'open'"),
    )
    fine_amount: Mapped[Decimal] = mapped_column(
        DECIMAL(12, 2), nullable=False, server_default=text("0.00")
    )
    approved_by_id: Mapped[int | None] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("users.user_id", ondelete="SET NULL"),
    )
    notes: Mapped[str | None] = mapped_column(String(500))
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP,
        nullable=False,
        server_default=text("CURRENT_TIMESTAMP"),
        server_onupdate=text("CURRENT_TIMESTAMP"),
    )
