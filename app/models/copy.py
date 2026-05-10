from datetime import datetime, date
from sqlalchemy import ForeignKey, String, Enum, Date, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .base import Base


CONDITION_VALUES = ("new", "good", "fair", "damaged", "lost")
CIRCULATION_VALUES = ("available", "on_loan", "reserved", "lost", "repair")


class Copy(Base):
    __tablename__ = "copies"

    copy_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    edition_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("editions.edition_id", ondelete="RESTRICT"),
        nullable=False,
    )
    barcode: Mapped[str] = mapped_column(String(64), nullable=False)
    call_number: Mapped[str | None] = mapped_column(String(128))
    acquisition_date: Mapped[date | None] = mapped_column(Date)
    condition_status: Mapped[str] = mapped_column(
        Enum(*CONDITION_VALUES, name="copy_condition"),
        nullable=False,
        server_default=text("'good'"),
    )
    circulation_status: Mapped[str] = mapped_column(
        Enum(*CIRCULATION_VALUES, name="copy_circulation"),
        nullable=False,
        server_default=text("'available'"),
    )
    shelf_location: Mapped[str | None] = mapped_column(String(128))
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

    edition: Mapped["Edition"] = relationship(back_populates="copies")
