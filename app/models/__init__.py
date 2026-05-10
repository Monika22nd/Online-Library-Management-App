"""SQLAlchemy ORM models for the MySQL library schema.

Importing this package registers every model with `Base.metadata`, which
Alembic's `env.py` uses for autogenerate. Add new models here when created.
"""
from .base import Base, engine, SessionLocal, get_db_session, MYSQL_URL
from .author import Author
from .book import Book
from .book_author import BookAuthor
from .edition import Edition
from .copy import Copy
from .member import Member
from .user import User
from .loan import Loan
from .reservation import Reservation
from .fine import Fine
from .payment import Payment
from .audit_log import AuditLog
from .sync_log import SyncLog

__all__ = [
    "Base",
    "engine",
    "SessionLocal",
    "get_db_session",
    "MYSQL_URL",
    "Author",
    "Book",
    "BookAuthor",
    "Edition",
    "Copy",
    "Member",
    "User",
    "Loan",
    "Reservation",
    "Fine",
    "Payment",
    "AuditLog",
    "SyncLog",
]
