import os
from datetime import datetime
from typing import Optional, List

from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.models import get_db_session
import app.services.openlibrary as openlibrary
import app.services.redis_service as redis_svc
import app.services.auth_svc as auth_svc
import app.services.books_svc as books_svc
import app.services.loans_svc as loans_svc
import app.services.audit_svc as audit_svc

app = FastAPI(title="Online Library API", version="2.0.0")

# Browsers reject Access-Control-Allow-Origin: * combined with credentials.
# Read explicit origins from env (comma-separated); fall back to dev defaults.
_origins_env = os.environ.get("CORS_ALLOW_ORIGINS", "")
_default_origins = [
    "http://localhost:3000",
    "http://127.0.0.1:3000",
    "http://localhost:5173",
]
_origins = [o.strip() for o in _origins_env.split(",") if o.strip()] or _default_origins

app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class UserCreate(BaseModel):
    username: str
    password: str
    email: str
    role: Optional[str] = "USER"


class UserLogin(BaseModel):
    username: str
    password: str


class Token(BaseModel):
    access_token: str
    token_type: str
    user: dict


class LoanAction(BaseModel):
    loan_id: int
    user_id: Optional[int] = None


class BulkApprove(BaseModel):
    loan_ids: List[int]


class ImportOne(BaseModel):
    openlibrary_key: str


# Demo-grade in-memory token store. Same as the legacy app.
fake_users_db: dict[str, dict] = {}


@app.on_event("startup")
async def _startup_seed() -> None:
    """Auto-seed demo users on startup. Non-fatal: if MySQL isn't reachable
    or alembic hasn't been run yet, log a warning and continue."""
    try:
        from app.scripts.seed import seed_users
        n = seed_users()
        if n:
            print(f"[seed] Created {n} demo user(s).")
    except Exception as e:
        print(f"[seed] Skipped (run `alembic upgrade head` first): {e}")


@app.get("/")
async def root():
    return {"message": "Online Library API", "version": "2.0.0"}


# ─── Auth ───────────────────────────────────────────────────────

@app.post("/api/auth/register", status_code=status.HTTP_201_CREATED)
async def register(payload: UserCreate, session: Session = Depends(get_db_session)):
    if auth_svc.get_user_by_username(session, payload.username):
        raise HTTPException(status_code=400, detail="Username already exists")
    user = auth_svc.create_user(
        session, payload.username, payload.password, payload.email, payload.role or "USER"
    )
    return {"id": user.user_id, "username": user.username}


@app.post("/api/auth/login", response_model=Token)
async def login(payload: UserLogin, session: Session = Depends(get_db_session)):
    user = auth_svc.verify_password(session, payload.username, payload.password)
    if not user:
        raise HTTPException(status_code=401, detail="Invalid credentials")

    user_dict = auth_svc.user_to_dict(user)
    token = f"token_{user.user_id}_{datetime.now().timestamp()}"
    fake_users_db[token] = user_dict
    return {"access_token": token, "token_type": "bearer", "user": user_dict}


@app.post("/api/auth/logout")
async def logout(token: str):
    fake_users_db.pop(token, None)
    return {"message": "Logged out"}


# ─── Local Books (DB) ──────────────────────────────────────────

@app.get("/api/books")
async def list_books(
    limit: int = 50, offset: int = 0, session: Session = Depends(get_db_session)
):
    return books_svc.list_books(session, limit, offset)


@app.get("/api/books/{book_id}")
async def get_book(book_id: int, session: Session = Depends(get_db_session)):
    book = books_svc.get_book(session, book_id)
    if not book:
        raise HTTPException(status_code=404, detail="Book not found")
    return book


# ─── OpenLibrary Proxy API ─────────────────────────────────────

@app.get("/api/search")
async def search_books_external(
    q: str = "programming", limit: int = 20, page: int = 1,
    session: Session = Depends(get_db_session),
):
    """Search the local catalog first. Fall back to OpenLibrary only if the
    local DB has no match (so the demo still works on an empty schema)."""
    local = books_svc.search_books_local(session, q, limit, page)
    if local["books"]:
        return local

    cache_key = f"search:{q}:{limit}:{page}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    result = await openlibrary.search_books(q, limit, page)
    redis_svc.set_cache(cache_key, result, ttl=600)
    return result


@app.get("/api/works/{work_id}")
async def get_work_details(work_id: str, session: Session = Depends(get_db_session)):
    """Local catalog first; fall back to OpenLibrary for works we haven't
    imported yet."""
    local = books_svc.get_work_dict_by_openlibrary_key(session, work_id)
    if local:
        return local

    cache_key = f"work:/works/{work_id}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    details = await openlibrary.get_book_details(f"/works/{work_id}")
    if not details:
        raise HTTPException(status_code=404, detail="Work not found")
    redis_svc.set_cache(cache_key, details, ttl=3600)
    return details


@app.get("/api/trending")
async def get_trending(limit: int = 20, session: Session = Depends(get_db_session)):
    """'Trending' from the local catalog (most-recently-added). Falls back
    to the OpenLibrary trending feed if the catalog is empty."""
    local = books_svc.get_trending_local(session, limit)
    if local:
        return local

    cache_key = f"trending:{limit}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    books = await openlibrary.get_trending_books(limit)
    redis_svc.set_cache(cache_key, books, ttl=1800)
    return books


@app.get("/api/subjects/{subject}")
async def get_subject(
    subject: str, limit: int = 20, offset: int = 0,
    session: Session = Depends(get_db_session),
):
    local = books_svc.list_books_by_subject(session, subject, limit, offset)
    if local["books"]:
        return local

    cache_key = f"subject:{subject}:{limit}:{offset}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    result = await openlibrary.get_subject_books(subject, limit, offset)
    redis_svc.set_cache(cache_key, result, ttl=1800)
    return result


@app.get("/api/authors/{author_key}")
async def get_author(author_key: str):
    cache_key = f"author:{author_key}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    details = await openlibrary.get_author_details(author_key)
    if not details:
        raise HTTPException(status_code=404, detail="Author not found")
    redis_svc.set_cache(cache_key, details, ttl=3600)
    return details


@app.post("/api/books/import")
async def import_books(
    query: str = "programming", limit: int = 50,
    session: Session = Depends(get_db_session),
):
    saved = await openlibrary.fetch_and_save_books(session, query, limit)
    return {"saved": saved}


@app.post("/api/books/import_one")
async def import_one(
    payload: ImportOne, session: Session = Depends(get_db_session),
):
    book_id = await openlibrary.import_one_by_work_key(session, payload.openlibrary_key)
    if not book_id:
        raise HTTPException(status_code=404, detail="OpenLibrary work not found")
    book = books_svc.get_book(session, book_id)
    return book


# ─── Cart (Redis) ──────────────────────────────────────────────

@app.post("/api/cart/add/{book_id}")
async def add_to_cart(book_id: int, user_id: int):
    success = redis_svc.add_to_cart(user_id, str(book_id))
    return {"success": success, "book_id": book_id}


@app.delete("/api/cart/remove/{book_id}")
async def remove_from_cart(book_id: int, user_id: int):
    redis_svc.remove_from_cart(user_id, str(book_id))
    return {"success": True}


@app.get("/api/cart")
async def get_cart(user_id: int, session: Session = Depends(get_db_session)):
    cart = redis_svc.get_cart(user_id)
    books = []
    for book_id in cart:
        book = books_svc.get_book(session, int(book_id))
        if book:
            books.append(book)
    return books


@app.post("/api/cart/checkout")
async def checkout_cart(user_id: int, session: Session = Depends(get_db_session)):
    cart = redis_svc.get_cart(user_id)
    if not cart:
        raise HTTPException(status_code=400, detail="Cart is empty")

    user = auth_svc.get_user_by_id(session, user_id)
    if not user or not user.member_id:
        raise HTTPException(
            status_code=400,
            detail="User has no patron (member) record; cannot borrow books.",
        )

    loans: list[int] = []
    skipped: list[dict] = []
    for book_id in cart:
        try:
            loan_id = loans_svc.create_loan_request(session, user.member_id, int(book_id))
            loans.append(loan_id)
        except loans_svc.MaxLoansExceeded:
            skipped.append({"book_id": int(book_id), "reason": "max active loans (5)"})
            break
        except Exception as e:
            skipped.append({"book_id": int(book_id), "reason": str(e)})

    redis_svc.clear_cart(user_id)
    return {"loans": loans, "skipped": skipped, "message": "Checkout successful"}


# ─── Loans ──────────────────────────────────────────────────────

@app.get("/api/loans/user/{user_id}")
async def get_user_loans(
    user_id: int, status: Optional[str] = None,
    session: Session = Depends(get_db_session),
):
    user = auth_svc.get_user_by_id(session, user_id)
    if not user or not user.member_id:
        return []
    return loans_svc.get_user_loans(session, user.member_id, status)


@app.get("/api/loans")
async def list_loans(
    status: Optional[str] = None, session: Session = Depends(get_db_session)
):
    return loans_svc.get_all_loans(session, status)


@app.post("/api/loans/{loan_id}/approve")
async def approve_loan(
    loan_id: int, admin_id: int, session: Session = Depends(get_db_session)
):
    """Approve a loan. Returns 200 whenever the loan state changed
    (APPROVED or auto-REJECTED-no-copies) so the UI can reload either way.
    Returns 400 only when the request itself was invalid (not pending,
    not found, bad admin)."""
    result = loans_svc.approve_loan(session, loan_id, admin_id)
    if result["status"] in ("APPROVED", "REJECTED"):
        return result
    raise HTTPException(status_code=400, detail=result["message"])


@app.post("/api/loans/{loan_id}/return")
async def return_loan(loan_id: int, session: Session = Depends(get_db_session)):
    success = loans_svc.return_loan(session, loan_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot return loan")
    return {"success": True}


@app.post("/api/loans/{loan_id}/cancel")
async def cancel_loan(loan_id: int, session: Session = Depends(get_db_session)):
    success = loans_svc.cancel_loan(session, loan_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot cancel loan")
    return {"success": True}


@app.post("/api/loans/transfer")
async def transfer_loan(data: LoanAction, session: Session = Depends(get_db_session)):
    success = loans_svc.transfer_loan(session, data.loan_id, data.user_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot transfer loan")
    return {"success": True}


@app.post("/api/loans/bulk-approve")
async def bulk_approve(
    data: BulkApprove, admin_id: int,
    session: Session = Depends(get_db_session),
):
    approved = loans_svc.bulk_approve_loans(session, data.loan_ids, admin_id)
    return {"approved": approved}


# ─── Audit ──────────────────────────────────────────────────────

@app.get("/api/audit")
async def audit_log(limit: int = 50, session: Session = Depends(get_db_session)):
    return audit_svc.get_audit_log(session, limit)


# ─── Health ─────────────────────────────────────────────────────

@app.get("/api/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
