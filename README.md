# Online Library Management App

A library management system built with **FastAPI**, **React**, **MySQL**, and **Redis**.

## Features

- **OpenLibrary Integration** — Search and browse millions of books via the OpenLibrary API
- **Redis Caching** — API response caching and atomic cart operations (in-memory fallback when Redis is unavailable)
- **User Management** — Authentication (bcrypt) and role-based access (USER / ADMIN)
- **Loan System** — Borrow books for 14 days with admin approval workflow
- **Physical Inventory** — Books → editions → individual copies with circulation status
- **Admin Panel** — Approve/reject/bulk-approve loan requests and view audit logs
- **MySQL Triggers + Stored Procedures** — Enforce max loans, audit status changes, and run loan approvals atomically at the database level
- **Alembic Migrations** — Versioned schema with two revisions (initial tables + triggers/procedures)

## Tech Stack

| Layer      | Technology                              |
|------------|-----------------------------------------|
| Backend    | FastAPI (Python 3.11) + SQLAlchemy 2.0  |
| Database   | MySQL 8 + Redis                         |
| Migrations | Alembic                                 |
| Frontend   | React 19 + Vite                         |

## Quick Start

### 1. MySQL

You need a running MySQL 8 instance. The app does **not** ship a MySQL container.

```sql
CREATE DATABASE library_openlibrary CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Set the connection URL:

```bash
export MYSQL_URL="mysql+pymysql://root:@localhost:3306/library_openlibrary?charset=utf8mb4"
```

### 2. Backend

```bash
pip install -r requirements.txt
alembic upgrade head            # creates tables + triggers + stored procedures
python -m app.scripts.seed      # creates demo users (admin/john/jane)
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
# Visit http://localhost:3000
```

### Redis (optional)

Redis is the only containerized dependency. The app falls back to in-memory
cart/cache if it can't connect, so this step is optional for local dev.

```bash
docker-compose up -d redis
```

That's it — the API and frontend run directly on the host (see steps 2 and 3
above). MySQL also runs on the host.

## Demo Accounts

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| john     | user123   | USER  |
| jane     | user123   | USER  |

## Project Structure

```
├── app/
│   ├── main.py                  # FastAPI entry point & routes
│   ├── models/                  # SQLAlchemy ORM models (one file per entity)
│   ├── services/                # auth_svc, books_svc, loans_svc, audit_svc, openlibrary, redis_service
│   └── scripts/seed.py          # Demo user seeding
├── alembic/                     # Migrations (0001_initial, 0002_triggers_and_procedures)
├── alembic.ini
├── db/sql/                      # Reference copy of triggers & stored procedures
├── frontend/                    # React SPA (Vite)
├── openlibrary_schema.sql       # Canonical SQL schema (single source of truth)
└── docker-compose.yml           # Redis only — API/frontend/MySQL run on the host
```

## Database

- **Schema source of truth**: `openlibrary_schema.sql`
- **Triggers + procedures (readable)**: `db/sql/triggers_and_procedures.sql`
- **Applied via Alembic**: `alembic/versions/0001_initial.py`, `alembic/versions/0002_triggers_and_procedures.py`

The MySQL layer enforces:

- `trg_loans_check_max` — aborts INSERTs if a member already has 5 active loans
- `trg_loans_status_audit` — writes an `audit_log` row whenever `loans.approval_status` changes
- `sp_approve_loan(loan_id, admin_id)` — atomically picks an available copy, marks it `on_loan`, and flips the loan to APPROVED
- `sp_return_loan(loan_id)` — frees the copy and marks the loan RETURNED
- `sp_cancel_loan(loan_id)` — cancels a PENDING/APPROVED loan and frees the copy if needed
- `sp_bulk_approve_loans(loan_ids, admin_id)` — calls `sp_approve_loan` for each ID in a comma-separated list

## Environment variables

| Var                  | Default                                                          | Notes                                |
|----------------------|------------------------------------------------------------------|--------------------------------------|
| `MYSQL_URL`          | `mysql+pymysql://root:@localhost:3306/library_openlibrary?charset=utf8mb4` | Required (no real default in code) |
| `REDIS_HOST`         | `localhost`                                                      | In-memory fallback if unavailable    |
| `REDIS_PORT`         | `6379`                                                           |                                      |
| `CORS_ALLOW_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173` | Comma-separated allowlist        |
