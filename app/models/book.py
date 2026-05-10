from datetime import datetime
from sqlalchemy import String, Text, JSON, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT, SMALLINT
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .base import Base


class Book(Base):
    __tablename__ = "books"

    book_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    openlibrary_work_key: Mapped[str] = mapped_column(String(32), nullable=False)
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    subtitle: Mapped[str | None] = mapped_column(String(500))
    description: Mapped[str | None] = mapped_column(Text)
    first_publish_year: Mapped[int | None] = mapped_column(SMALLINT(unsigned=True))
    first_publish_date: Mapped[str | None] = mapped_column(String(64))
    subjects: Mapped[dict | None] = mapped_column(JSON)
    places: Mapped[dict | None] = mapped_column(JSON)
    series: Mapped[dict | None] = mapped_column(JSON)
    links: Mapped[dict | None] = mapped_column(JSON)
    excerpts: Mapped[dict | None] = mapped_column(JSON)
    cover_ids: Mapped[dict | None] = mapped_column(JSON)
    cover_edition_key: Mapped[str | None] = mapped_column(String(32))
    work_raw: Mapped[dict | None] = mapped_column(JSON)
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP,
        nullable=False,
        server_default=text("CURRENT_TIMESTAMP"),
        server_onupdate=text("CURRENT_TIMESTAMP"),
    )

    editions: Mapped[list["Edition"]] = relationship(back_populates="book", cascade="all, delete-orphan")
    authors: Mapped[list["BookAuthor"]] = relationship(back_populates="book", cascade="all, delete-orphan")
