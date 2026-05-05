from fastapi import FastAPI, HTTPException, Depends, status, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime, date
import os

app = FastAPI(title="Online Library API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

import app.models.database as db
import app.services.openlibrary as openlibrary
import app.services.redis_service as redis_svc

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

class BookCreate(BaseModel):
    openlibrary_key: str
    title: str
    author: str
    isbn: Optional[str] = None
    cover_url: Optional[str] = None
    description: Optional[str] = None
    publish_year: Optional[int] = None
    subjects: Optional[str] = None
    total_copies: int = 1

class LoanCreate(BaseModel):
    book_id: int

class LoanAction(BaseModel):
    loan_id: int
    user_id: Optional[int] = None

class BulkApprove(BaseModel):
    loan_ids: List[int]

fake_users_db = {}

def get_current_user(token: str = None):
    if not token:
        return None
    if token in fake_users_db:
        return fake_users_db[token]
    return None

@app.on_event("startup")
async def startup():
    db.init_db()
    db.seed_sample_data()

@app.get("/")
async def root():
    return {"message": "Online Library API", "version": "1.0.0"}

# ─── Auth ───────────────────────────────────────────────────────

@app.post("/api/auth/register", status_code=status.HTTP_201_CREATED)
async def register(user: UserCreate):
    existing = db.get_user_by_username(user.username)
    if existing:
        raise HTTPException(status_code=400, detail="Username already exists")
    
    user_id = db.create_user(user.username, user.password, user.email, user.role)
    return {"id": user_id, "username": user.username}

@app.post("/api/auth/login", response_model=Token)
async def login(user: UserLogin):
    db_user = db.verify_password(user.username, user.password)
    if not db_user:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    token = f"token_{db_user['id']}_{datetime.now().timestamp()}"
    fake_users_db[token] = db_user
    
    return {"access_token": token, "token_type": "bearer", "user": db_user}

@app.post("/api/auth/logout")
async def logout(token: str):
    if token in fake_users_db:
        del fake_users_db[token]
    return {"message": "Logged out"}

# ─── Local Books (DB) ──────────────────────────────────────────

@app.get("/api/books")
async def list_books(limit: int = 50, offset: int = 0):
    books = db.get_all_books(limit, offset)
    total = db.get_books_count()
    return {"books": books, "total": total}

@app.get("/api/books/{book_id}")
async def get_book(book_id: int):
    book = db.get_book_by_id(book_id)
    if not book:
        raise HTTPException(status_code=404, detail="Book not found")
    return book

# ─── OpenLibrary Proxy API ─────────────────────────────────────

@app.get("/api/search")
async def search_books_external(
    q: str = "programming",
    limit: int = 20,
    page: int = 1
):
    """Search books via OpenLibrary API with caching."""
    cache_key = f"search:{q}:{limit}:{page}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    
    result = await openlibrary.search_books(q, limit, page)
    redis_svc.set_cache(cache_key, result, ttl=600)  # 10 min cache
    return result

@app.get("/api/works/{work_id}")
async def get_work_details(work_id: str):
    """Get detailed info about a specific work from OpenLibrary."""
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
async def get_trending(limit: int = 20):
    """Get trending books from OpenLibrary."""
    cache_key = f"trending:{limit}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    
    books = await openlibrary.get_trending_books(limit)
    redis_svc.set_cache(cache_key, books, ttl=1800)  # 30 min cache
    return books

@app.get("/api/subjects/{subject}")
async def get_subject(subject: str, limit: int = 20, offset: int = 0):
    """Get books by subject from OpenLibrary."""
    cache_key = f"subject:{subject}:{limit}:{offset}"
    cached = redis_svc.get_cache(cache_key)
    if cached:
        return cached
    
    result = await openlibrary.get_subject_books(subject, limit, offset)
    redis_svc.set_cache(cache_key, result, ttl=1800)
    return result

@app.get("/api/authors/{author_key}")
async def get_author(author_key: str):
    """Get author details from OpenLibrary."""
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
async def import_books(query: str = "programming", limit: int = 50):
    saved = await openlibrary.fetch_and_save_books(query, limit)
    return {"saved": saved}

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
async def get_cart(user_id: int):
    cart = redis_svc.get_cart(user_id)
    books = []
    for book_id in cart:
        book = db.get_book_by_id(int(book_id))
        if book:
            books.append(book)
    return books

@app.post("/api/cart/checkout")
async def checkout_cart(user_id: int):
    cart = redis_svc.get_cart(user_id)
    if not cart:
        raise HTTPException(status_code=400, detail="Cart is empty")
    
    loans = []
    for book_id in cart:
        try:
            loan_id = db.create_loan(user_id, int(book_id))
            loans.append(loan_id)
        except Exception as e:
            print(f"Error creating loan for book {book_id}: {e}")
    
    redis_svc.clear_cart(user_id)
    return {"loans": loans, "message": "Checkout successful"}

# ─── Loans ──────────────────────────────────────────────────────

@app.get("/api/loans/user/{user_id}")
async def get_user_loans(user_id: int, status: Optional[str] = None):
    return db.get_user_loans(user_id, status)

@app.get("/api/loans")
async def list_loans(status: Optional[str] = None):
    return db.get_all_loans(status)

@app.post("/api/loans/{loan_id}/approve")
async def approve_loan(loan_id: int, admin_id: int):
    success = db.approve_loan(loan_id, admin_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot approve loan")
    return {"success": True}

@app.post("/api/loans/{loan_id}/return")
async def return_loan(loan_id: int):
    success = db.return_loan(loan_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot return loan")
    return {"success": True}

@app.post("/api/loans/{loan_id}/cancel")
async def cancel_loan(loan_id: int):
    success = db.cancel_loan(loan_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot cancel loan")
    return {"success": True}

@app.post("/api/loans/transfer")
async def transfer_loan(data: LoanAction):
    success = db.transfer_loan(data.loan_id, data.user_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot transfer loan")
    return {"success": True}

@app.post("/api/loans/bulk-approve")
async def bulk_approve(data: BulkApprove, admin_id: int):
    approved = db.bulk_approve_loans(data.loan_ids, admin_id)
    return {"approved": approved}

# ─── Audit ──────────────────────────────────────────────────────

@app.get("/api/audit")
async def audit_log(limit: int = 50):
    return db.get_audit_log(limit)

# ─── Health ─────────────────────────────────────────────────────

@app.get("/api/health")
async def health():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)