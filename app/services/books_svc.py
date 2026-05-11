from datetime import date
from sqlalchemy import select, func, or_, String, cast
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
        # MySQL doesn't accept `NULLS LAST`; emulate with `IS NULL` ordering.
        .order_by(
            BookAuthor.book_id,
            BookAuthor.author_order.is_(None),
            BookAuthor.author_order.asc(),
        )
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
    first_publish_date: str | None = None,
    description: str | None = None,
    subjects: list[str] | None = None,
    links: list | None = None,
    excerpts: list | None = None,
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
        description=description,
        first_publish_year=publish_year,
        first_publish_date=first_publish_date,
        subjects=subjects,
        links=links,
        excerpts=excerpts,
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


# ─── Local catalog queries (MySQL-first read paths) ───────────────────────

def _normalize_work_key(work_id_or_key: str) -> str:
    """Accept 'OL...W' or '/works/OL...W' and return canonical '/works/OL...W'."""
    key = (work_id_or_key or "").strip()
    if not key:
        return key
    if not key.startswith("/works/"):
        key = f"/works/{key.lstrip('/')}"
    return key


def _search_card_dict(book: Book, authors: list[str] | None) -> dict:
    """Shape that matches the OpenLibrary search response card."""
    return {
        "id": book.book_id,
        "openlibrary_key": book.openlibrary_work_key,
        "title": book.title,
        "author": (authors[0] if authors else "Unknown"),
        "authors": authors or ["Unknown"],
        "cover_url": _first_cover_url(book),
        "publish_year": book.first_publish_year,
        "subjects": book.subjects or [],
    }


def search_books_local(
    session: Session, q: str, limit: int = 20, page: int = 1
) -> dict:
    """Title/author search against the local catalog. Returns the same
    `{books, total, page, limit}` shape the OL search proxy returns."""
    q = (q or "").strip()
    if not q:
        return {"books": [], "total": 0, "page": page, "limit": limit}

    pattern = f"%{q}%"
    author_subq = (
        select(BookAuthor.book_id)
        .join(Author, Author.author_id == BookAuthor.author_id)
        .where(Author.author_name.ilike(pattern))
    )
    where = or_(Book.title.ilike(pattern), Book.book_id.in_(author_subq))

    total = session.scalar(select(func.count(Book.book_id)).where(where)) or 0
    offset = max(0, (page - 1) * limit)
    books = session.scalars(
        select(Book).where(where).order_by(Book.book_id).limit(limit).offset(offset)
    ).all()
    book_ids = [b.book_id for b in books]
    authors_by_book = _authors_for(session, book_ids)
    return {
        "books": [_search_card_dict(b, authors_by_book.get(b.book_id)) for b in books],
        "total": int(total),
        "page": page,
        "limit": limit,
    }


def _subject_where(subject_slug: str):
    """Match any book whose `subjects` JSON contains the human-readable
    form of the slug as a substring (case-insensitive)."""
    needle = (subject_slug or "").replace("_", " ").lower()
    return func.lower(cast(Book.subjects, String)).like(f"%{needle}%")


def list_books_by_subject(
    session: Session, subject_slug: str, limit: int = 20, offset: int = 0
) -> dict:
    """Return books from the local catalog whose subjects include the slug.
    Shape matches the OL `/subjects/{slug}.json` proxy: {books, total, subject}."""
    where = _subject_where(subject_slug)
    total = session.scalar(select(func.count(Book.book_id)).where(where)) or 0
    books = session.scalars(
        select(Book).where(where).order_by(Book.book_id).limit(limit).offset(offset)
    ).all()
    book_ids = [b.book_id for b in books]
    authors_by_book = _authors_for(session, book_ids)
    display = subject_slug.replace("_", " ")
    return {
        "books": [_search_card_dict(b, authors_by_book.get(b.book_id)) for b in books],
        "total": int(total),
        "subject": display,
    }


def get_trending_local(session: Session, limit: int = 20) -> list[dict]:
    """Cheap 'trending' from the local catalog. Sorts by most-recently-added
    so newly seeded titles surface. Returns a flat list like OL's trending."""
    books = session.scalars(
        select(Book).order_by(Book.book_id.desc()).limit(limit)
    ).all()
    book_ids = [b.book_id for b in books]
    authors_by_book = _authors_for(session, book_ids)
    return [_search_card_dict(b, authors_by_book.get(b.book_id)) for b in books]


def get_work_dict_by_openlibrary_key(
    session: Session, work_id_or_key: str
) -> dict | None:
    """Return a detail dict shaped like `openlibrary.get_book_details`
    (so the frontend BookDetail page can consume it unchanged), enriched
    with local catalog fields (`id`, `available_copies`, `editions`)."""
    key = _normalize_work_key(work_id_or_key)
    book = session.scalar(
        select(Book)
        .where(Book.openlibrary_work_key == key)
        .options(selectinload(Book.editions).selectinload(Edition.copies))
    )
    if not book:
        return None

    available, total = _availability_for(session, [book.book_id]).get(book.book_id, (0, 0))
    authors = _authors_for(session, [book.book_id]).get(book.book_id) or []
    cover_url = _first_cover_url(book)
    cover_id = None
    if isinstance(book.cover_ids, list) and book.cover_ids:
        cover_id = book.cover_ids[0]
    subject_list = book.subjects if isinstance(book.subjects, list) else []
    subjects_str = ", ".join(subject_list[:10]) if subject_list else ""
    return {
        "id": book.book_id,
        "openlibrary_key": book.openlibrary_work_key,
        "title": book.title,
        "description": book.description or "No description available.",
        "subjects": subjects_str,
        "subject_list": subject_list[:20],
        "cover_url": cover_url,
        "cover_id": cover_id,
        "authors": authors or ["Unknown"],
        "first_publish_date": book.first_publish_date,
        "first_publish_year": book.first_publish_year,
        "created": book.created_at.isoformat() if book.created_at else None,
        "links": book.links or [],
        "excerpts": book.excerpts or [],
        "available_copies": available,
        "total_copies": total,
        "editions": _editions_payload(book),
    }
