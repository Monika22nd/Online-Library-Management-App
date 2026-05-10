from datetime import datetime
from sqlalchemy import ForeignKey, String, JSON, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT, INTEGER
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .base import Base


class Edition(Base):
    __tablename__ = "editions"

    edition_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    openlibrary_edition_key: Mapped[str] = mapped_column(String(32), nullable=False)
    book_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("books.book_id", ondelete="CASCADE"),
        nullable=False,
    )
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    subtitle: Mapped[str | None] = mapped_column(String(500))
    by_statement: Mapped[str | None] = mapped_column(String(500))
    publish_date: Mapped[str | None] = mapped_column(String(64))
    number_of_pages: Mapped[int | None] = mapped_column(INTEGER(unsigned=True))
    languages: Mapped[dict | None] = mapped_column(JSON)
    publishers: Mapped[dict | None] = mapped_column(JSON)
    isbn_10: Mapped[str | None] = mapped_column(String(20))
    isbn_13: Mapped[str | None] = mapped_column(String(20))
    lccn: Mapped[str | None] = mapped_column(String(64))
    oclc: Mapped[str | None] = mapped_column(String(64))
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP,
        nullable=False,
        server_default=text("CURRENT_TIMESTAMP"),
        server_onupdate=text("CURRENT_TIMESTAMP"),
    )

    book: Mapped["Book"] = relationship(back_populates="editions")
    copies: Mapped[list["Copy"]] = relationship(back_populates="edition", cascade="all, delete-orphan")
