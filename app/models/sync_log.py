from datetime import datetime
from sqlalchemy import String, Text, Enum, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT, INTEGER, SMALLINT
from sqlalchemy.orm import Mapped, mapped_column
from .base import Base


SYNC_STATUS_VALUES = ("running", "success", "failed", "partial")


class SyncLog(Base):
    __tablename__ = "sync_logs"

    sync_log_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    source_name: Mapped[str] = mapped_column(
        String(64), nullable=False, server_default=text("'openlibrary'")
    )
    endpoint_name: Mapped[str] = mapped_column(String(128), nullable=False)
    source_key: Mapped[str | None] = mapped_column(String(128))
    request_url: Mapped[str] = mapped_column(String(1000), nullable=False)
    response_status: Mapped[int | None] = mapped_column(SMALLINT(unsigned=True))
    rows_upserted: Mapped[int] = mapped_column(
        INTEGER(unsigned=True), nullable=False, server_default=text("0")
    )
    sync_status: Mapped[str] = mapped_column(
        Enum(*SYNC_STATUS_VALUES, name="sync_status"),
        nullable=False,
        server_default=text("'running'"),
    )
    error_message: Mapped[str | None] = mapped_column(Text)
    started_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    finished_at: Mapped[datetime | None] = mapped_column(TIMESTAMP)
