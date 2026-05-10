from datetime import datetime, date
from sqlalchemy import ForeignKey, String, Enum, Date, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT
from sqlalchemy.orm import Mapped, mapped_column
from .base import Base


RESERVATION_STATUS_VALUES = ("pending", "ready", "fulfilled", "cancelled", "expired")


class Reservation(Base):
    __tablename__ = "reservations"

    reservation_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    member_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("members.member_id", ondelete="RESTRICT"),
        nullable=False,
    )
    book_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("books.book_id", ondelete="RESTRICT"),
        nullable=False,
    )
    request_date: Mapped[date] = mapped_column(Date, nullable=False)
    expiry_date: Mapped[date | None] = mapped_column(Date)
    reservation_status: Mapped[str] = mapped_column(
        Enum(*RESERVATION_STATUS_VALUES, name="reservation_status"),
        nullable=False,
        server_default=text("'pending'"),
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
