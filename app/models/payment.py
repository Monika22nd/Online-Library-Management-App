from datetime import datetime, date
from decimal import Decimal
from sqlalchemy import ForeignKey, String, Enum, Date, DECIMAL, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT
from sqlalchemy.orm import Mapped, mapped_column
from .base import Base


PAYMENT_METHOD_VALUES = ("cash", "card", "bank_transfer", "e_wallet", "other")


class Payment(Base):
    __tablename__ = "payments"

    payment_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    fine_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("fines.fine_id", ondelete="CASCADE"),
        nullable=False,
    )
    payment_date: Mapped[date] = mapped_column(Date, nullable=False)
    amount: Mapped[Decimal] = mapped_column(DECIMAL(12, 2), nullable=False)
    payment_method: Mapped[str] = mapped_column(
        Enum(*PAYMENT_METHOD_VALUES, name="payment_method"),
        nullable=False,
        server_default=text("'cash'"),
    )
    transaction_ref: Mapped[str | None] = mapped_column(String(128))
    note: Mapped[str | None] = mapped_column(String(500))
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
