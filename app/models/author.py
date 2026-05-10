from datetime import datetime
from sqlalchemy import BigInteger, String, Text, Integer, JSON, TIMESTAMP, text
from sqlalchemy.dialects.mysql import BIGINT, INTEGER
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .base import Base


class Author(Base):
    __tablename__ = "authors"

    author_id: Mapped[int] = mapped_column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    openlibrary_author_key: Mapped[str] = mapped_column(String(32), nullable=False)
    author_name: Mapped[str] = mapped_column(String(255), nullable=False)
    slug: Mapped[str | None] = mapped_column(String(255))
    birth_date: Mapped[str | None] = mapped_column(String(64))
    bio: Mapped[str | None] = mapped_column(Text)
    alternate_names: Mapped[dict | None] = mapped_column(JSON)
    top_subjects: Mapped[dict | None] = mapped_column(JSON)
    work_count: Mapped[int | None] = mapped_column(INTEGER(unsigned=True))
    author_raw: Mapped[dict | None] = mapped_column(JSON)
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP, nullable=False, server_default=text("CURRENT_TIMESTAMP")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP,
        nullable=False,
        server_default=text("CURRENT_TIMESTAMP"),
        server_onupdate=text("CURRENT_TIMESTAMP"),
    )

    books: Mapped[list["BookAuthor"]] = relationship(back_populates="author")
