from datetime import date
from sqlalchemy import select, func
from sqlalchemy.orm import Session, selectinload

from app.models import Book, Edition, Copy, BookAuthor, Author


def _book_to_dict(
    book: Book,
    available: int,
    total: int,
    authors: list[str] | None = None,
    editions: list[dict] | None = None,
) -> dict:
    out = {
        "id": book.book_id,
        "openlibrary_key": book.openlibrary_work_key,
        "title": book.title,
        "subtitle": book.subtitle,
        "description": book.description,
        "cover_url": _first_cover_url(book),
        "publish_year": book.first_publish_year,
        "subjects": book.subjects,
        "available_copies": available,
        "total_copies": total,
        "author": (authors[0] if authors else None),
        "authors": authors or [],
    }
    if editions is not None:
        out["editions"] = editions
    return out


def _first_cover_url(book: Book) -> str | None:
    cover_ids = book.cover_ids or []
    if isinstance(cover_ids, list) and cover_ids:
        return f"https://covers.openlibrary.org/b/id/{cover_ids[0]}-M.jpg"
    return None


def _availability_for(session: Session, book_ids: list[int]) -> dict[int, tuple[int, int]]:
    """Returns {book_id: (available_count, total_count)} via one grouped query."""
    if not book_ids:
        return {}
    rows = session.execute(
        select(
            Edition.book_id,
            func.count(Copy.copy_id).label("total"),
            func.sum(
                func.if_(Copy.circulation_status == "available", 1, 0)
            ).label("available"),
        )
        .join(Copy, Copy.edition_id == Edition.edition_id)
        .where(Edition.book_id.in_(book_ids))
        .group_by(Edition.book_id)
    ).all()
    return {row.book_id: (int(row.available or 0), int(row.total or 0)) for row in rows}


def _authors_for(session: Session, book_ids: list[int]) -> dict[int, list[str]]:
    if not book_ids:
        return {}
    rows = session.execute(
        select(BookAuthor.book_id, Author.author_name)
        .join(Author, Author.author_id == BookAuthor.author_id)
        .where(BookAuthor.book_id.in_(book_ids))
        .order_by(BookAuthor.book_id, BookAuthor.author_order.asc().nulls_last())
    ).all()
    out: dict[int, list[str]] = {}
    for book_id, name in rows:
        out.setdefault(book_id, []).append(name)
    return out


def _editions_payload(book: Book) -> list[dict]:
    """Serialize editions[].copies[] for the detail endpoint."""
    payload: list[dict] = []
    for ed in book.editions:
        payload.append({
            "edition_id": ed.edition_id,
            "title": ed.title,
            "isbn_13": ed.isbn_13,
            "isbn_10": ed.isbn_10,
            "publish_date": ed.publish_date,
            "number_of_pages": ed.number_of_pages,
            "copies": [
                {
                    "copy_id": c.copy_id,
                    "barcode": c.barcode,
                    "circulation_status": c.circulation_status,
                    "condition_status": c.condition_status,
                    "shelf_location": c.shelf_location,
                }
                for c in ed.copies
            ],
        })
    return payload


def list_books(session: Session, limit: int = 50, offset: int = 0) -> dict:
    books = session.scalars(
        select(Book).order_by(Book.book_id).limit(limit).offset(offset)
    ).all()
    book_ids = [b.book_id for b in books]
    avail = _availability_for(session, book_ids)
    authors_by_book = _authors_for(session, book_ids)
    total = session.scalar(select(func.count(Book.book_id)))
    return {
        "books": [
            _book_to_dict(
                b, *avail.get(b.book_id, (0, 0)),
                authors=authors_by_book.get(b.book_id),
            )
            for b in books
        ],
        "total": total or 0,
    }


def get_book(session: Session, book_id: int) -> dict | None:
    book = session.scalar(
        select(Book)
        .where(Book.book_id == book_id)
        .options(selectinload(Book.editions).selectinload(Edition.copies))
    )
    if not book:
        return None
    available, total = _availability_for(session, [book.book_id]).get(book.book_id, (0, 0))
    authors = _authors_for(session, [book.book_id]).get(book.book_id)
    return _book_to_dict(
        book, available, total,
        authors=authors,
        editions=_editions_payload(book),
    )


def get_book_by_openlibrary_key(session: Session, key: str) -> Book | None:
    return session.scalar(select(Book).where(Book.openlibrary_work_key == key))


def _upsert_author(session: Session, ol_key: str, name: str) -> Author:
    existing = session.scalar(
        select(Author).where(Author.openlibrary_author_key == ol_key)
    )
    if existing:
        return existing
    a = Author(openlibrary_author_key=ol_key, author_name=name)
    session.add(a)
    session.flush()
    return a


def _link_authors(session: Session, book: Book, authors: list[dict]) -> None:
    """authors: [{"key": "/authors/OL..A" or "OL..A", "name": str}, ...]"""
    for order, a in enumerate(authors, start=1):
        key, name = a.get("key"), a.get("name")
        if not key or not name:
            continue
        author = _upsert_author(session, key, name)
        existing_link = session.scalar(
            select(BookAuthor).where(
                BookAuthor.book_id == book.book_id,
                BookAuthor.author_id == author.author_id,
            )
        )
        if not existing_link:
            session.add(BookAuthor(
                book_id=book.book_id,
                author_id=author.author_id,
                author_order=order,
            ))


def import_book_with_copies(
    session: Session,
    openlibrary_key: str,
    title: str,
    cover_id: int | None = None,
    publish_year: int | None = None,
    subjects: list[str] | None = None,
    isbn: str | None = None,
    copies_to_create: int = 3,
    authors: list[dict] | None = None,
) -> Book | None:
    """Insert a book + 1 edition + N copies in one transaction. No-op if the
    work key already exists. `authors` is optional [{key, name}, ...]."""
    existing = get_book_by_openlibrary_key(session, openlibrary_key)
    if existing:
        return existing

    book = Book(
        openlibrary_work_key=openlibrary_key,
        title=title,
        first_publish_year=publish_year,
        subjects=subjects,
        cover_ids=[cover_id] if cover_id else None,
    )
    session.add(book)
    session.flush()

    edition = Edition(
        openlibrary_edition_key=f"{openlibrary_key}-ed1",
        book_id=book.book_id,
        title=title,
        isbn_13=isbn if isbn and len(isbn) == 13 else None,
        isbn_10=isbn if isbn and len(isbn) == 10 else None,
    )
    session.add(edition)
    session.flush()

    for n in range(copies_to_create):
        session.add(Copy(
            edition_id=edition.edition_id,
            barcode=f"BC-{book.book_id}-{n + 1}",
            acquisition_date=date.today(),
        ))

    if authors:
        _link_authors(session, book, authors)

    session.commit()
    session.refresh(book)
    return book
