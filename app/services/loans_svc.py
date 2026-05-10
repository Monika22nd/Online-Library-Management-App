from sqlalchemy import select, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models import Loan, Book, Member, User, BookAuthor, Author


class MaxLoansExceeded(Exception):
    """Raised when the trg_loans_check_max trigger aborts an INSERT."""


def _cover_url_for(book: Book | None) -> str | None:
    if not book or not book.cover_ids:
        return None
    cover_ids = book.cover_ids
    if isinstance(cover_ids, list) and cover_ids:
        return f"https://covers.openlibrary.org/b/id/{cover_ids[0]}-M.jpg"
    return None


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


def _loan_to_dict(loan: Loan, book: Book | None = None,
                  member: Member | None = None,
                  authors: list[str] | None = None) -> dict:
    return {
        "id": loan.loan_id,
        "member_id": loan.member_id,
        "copy_id": loan.copy_id,
        "requested_book_id": loan.requested_book_id,
        "request_date": loan.request_date.isoformat() if loan.request_date else None,
        "borrow_date": loan.borrow_date.isoformat() if loan.borrow_date else None,
        "due_date": loan.due_date.isoformat() if loan.due_date else None,
        "return_date": loan.return_date.isoformat() if loan.return_date else None,
        "status": loan.approval_status,
        "loan_status": loan.loan_status,
        "approved_by_id": loan.approved_by_id,
        "notes": loan.notes,
        "title": book.title if book else None,
        "cover_url": _cover_url_for(book),
        "author": (authors[0] if authors else None),
        "authors": authors or [],
        "username": None,  # populated for admin views below
    }


def create_loan_request(session: Session, member_id: int, book_id: int) -> int:
    """Insert a PENDING loan tied to a requested book. The trg_loans_check_max
    trigger may abort this; we translate the IntegrityError into a domain
    exception the route can return as a 400."""
    loan = Loan(member_id=member_id, requested_book_id=book_id)
    session.add(loan)
    try:
        session.commit()
    except IntegrityError as e:
        session.rollback()
        if "maximum active loans" in str(e.orig).lower():
            raise MaxLoansExceeded() from e
        raise
    session.refresh(loan)
    return loan.loan_id


def get_user_loans(session: Session, member_id: int, status: str | None = None) -> list[dict]:
    stmt = (
        select(Loan, Book)
        .outerjoin(Book, Book.book_id == Loan.requested_book_id)
        .where(Loan.member_id == member_id)
        .order_by(Loan.request_date.desc())
    )
    if status:
        stmt = stmt.where(Loan.approval_status == status)
    rows = session.execute(stmt).all()
    book_ids = [book.book_id for _, book in rows if book]
    authors_by_book = _authors_for(session, book_ids)
    return [
        _loan_to_dict(loan, book, authors=authors_by_book.get(book.book_id) if book else None)
        for loan, book in rows
    ]


def get_all_loans(session: Session, status: str | None = None) -> list[dict]:
    stmt = (
        select(Loan, Book, Member, User)
        .outerjoin(Book, Book.book_id == Loan.requested_book_id)
        .join(Member, Member.member_id == Loan.member_id)
        .outerjoin(User, User.member_id == Member.member_id)
        .order_by(Loan.request_date.desc())
    )
    if status:
        stmt = stmt.where(Loan.approval_status == status)
    rows = session.execute(stmt).all()
    book_ids = [book.book_id for _, book, _, _ in rows if book]
    authors_by_book = _authors_for(session, book_ids)
    out = []
    for loan, book, member, user in rows:
        d = _loan_to_dict(
            loan, book, member,
            authors=authors_by_book.get(book.book_id) if book else None,
        )
        d["username"] = user.username if user else member.member_name
        out.append(d)
    return out


def approve_loan(session: Session, loan_id: int, admin_id: int) -> bool:
    session.execute(
        text("CALL sp_approve_loan(:loan_id, :admin_id)"),
        {"loan_id": loan_id, "admin_id": admin_id},
    )
    session.commit()
    loan = session.get(Loan, loan_id)
    return bool(loan and loan.approval_status == "APPROVED")


def return_loan(session: Session, loan_id: int) -> bool:
    session.execute(text("CALL sp_return_loan(:loan_id)"), {"loan_id": loan_id})
    session.commit()
    loan = session.get(Loan, loan_id)
    return bool(loan and loan.approval_status == "RETURNED")


def cancel_loan(session: Session, loan_id: int) -> bool:
    session.execute(text("CALL sp_cancel_loan(:loan_id)"), {"loan_id": loan_id})
    session.commit()
    loan = session.get(Loan, loan_id)
    return bool(loan and loan.approval_status == "CANCELLED")


def bulk_approve_loans(session: Session, loan_ids: list[int], admin_id: int) -> int:
    if not loan_ids:
        return 0
    csv = ",".join(str(int(i)) for i in loan_ids)
    session.execute(
        text("CALL sp_bulk_approve_loans(:ids, :admin_id)"),
        {"ids": csv, "admin_id": admin_id},
    )
    session.commit()
    rows = session.execute(
        select(Loan.loan_id).where(
            Loan.loan_id.in_(loan_ids), Loan.approval_status == "APPROVED"
        )
    ).all()
    return len(rows)


def transfer_loan(session: Session, loan_id: int, new_member_id: int) -> bool:
    loan = session.get(Loan, loan_id)
    if not loan or loan.approval_status != "PENDING":
        return False
    new_member = session.get(Member, new_member_id)
    if not new_member:
        return False
    loan.member_id = new_member_id
    session.commit()
    return True
