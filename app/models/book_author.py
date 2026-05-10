from sqlalchemy import ForeignKey, String
from sqlalchemy.dialects.mysql import BIGINT, SMALLINT
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .base import Base


class BookAuthor(Base):
    __tablename__ = "book_authors"

    book_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("books.book_id", ondelete="CASCADE"),
        primary_key=True,
    )
    author_id: Mapped[int] = mapped_column(
        BIGINT(unsigned=True),
        ForeignKey("authors.author_id", ondelete="RESTRICT"),
        primary_key=True,
    )
    author_order: Mapped[int | None] = mapped_column(SMALLINT(unsigned=True))
    role_name: Mapped[str | None] = mapped_column(String(128))

    book: Mapped["Book"] = relationship(back_populates="authors")
    author: Mapped["Author"] = relationship(back_populates="books")
