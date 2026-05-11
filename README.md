# Online Library Management App

A library management system built with **FastAPI**, **React**, **MySQL**, and **Redis**.

## Features

- **OpenLibrary Integration** — Seed the local catalog with 300+ books from OpenLibrary; search/browse/trending all serve from MySQL first, with OpenLibrary used only as a fallback when the catalog has no match
- **Redis Caching** — API response caching for OpenLibrary fallback paths and atomic cart operations (in-memory fallback when Redis is unavailable)
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

### Prerequisites

| Tool | Version | Where it runs |
|------|---------|---------------|
| Python | 3.11 | Host |
| Node.js | 18+ (LTS recommended) | Host |
| MySQL Server | 8.0+ (tested on 8.0.44) | Host |
| Docker | any recent | Optional — only for Redis |

You do **not** need Docker for the API, frontend, or database — only for Redis,
and even Redis is optional (the app falls back to an in-memory cart/cache).

### 1. Create the MySQL database

Connect with any MySQL client (`mysql`, MySQL Workbench, DBeaver…) and run:

```sql
CREATE DATABASE IF NOT EXISTS library_openlibrary
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

You don't need to load `openlibrary_schema.sql` manually — Alembic creates the
tables in step 3.

### 2. Set `MYSQL_URL`

The connection string is read from the `MYSQL_URL` environment variable by
both the FastAPI runtime (`app/models/base.py`) and the Alembic migrations
(`alembic/env.py`). **Format:**

```
mysql+pymysql://USER:PASSWORD@HOST:PORT/DBNAME?charset=utf8mb4
```

URL-encode special characters in the password (`@` → `%40`, `#` → `%23`, etc.).

<details>
<summary><b>PowerShell (Windows)</b></summary>

```powershell
# Current session only
$env:MYSQL_URL = "mysql+pymysql://root:YOURPASS@localhost:3306/library_openlibrary?charset=utf8mb4"

# Persist for new terminals (then close + reopen this one)
setx MYSQL_URL "mysql+pymysql://root:YOURPASS@localhost:3306/library_openlibrary?charset=utf8mb4"
```
</details>

<details>
<summary><b>bash / zsh (macOS, Linux)</b></summary>

```bash
export MYSQL_URL="mysql+pymysql://root:YOURPASS@localhost:3306/library_openlibrary?charset=utf8mb4"
```
</details>

### 3. Backend — install, migrate, seed, run

Run these in the **same shell** where `MYSQL_URL` is set:

```bash
pip install -r requirements.txt

alembic upgrade head             # 0001 = tables; 0002 = triggers + stored procedures
python -m app.scripts.seed       # demo users: admin/john/jane (idempotent)
python -m app.scripts.seed_books # ~300 books from OpenLibrary (idempotent)

python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Important:

- **Don't skip `alembic upgrade head`.** The loan workflow (approve/return/cancel)
  is implemented as MySQL stored procedures created by migration `0002`. If you
  only run `0001` (or load `openlibrary_schema.sql` by hand), Approve will fail
  with `ERROR 1305 PROCEDURE sp_approve_loan does not exist`.
- `seed` and `seed_books` are both idempotent — re-run any time to fill gaps.
- `seed_books` defaults to 30 books × 10 subjects (matching the Home-page chips).
  Override with `--per N` for a different size, or `--no-enrich` to skip the
  per-work detail fetch (faster, but no descriptions/excerpts).
- The API listens on `:8000`. Health-check: `curl http://localhost:8000/api/health`.

### 4. Frontend — install and run

In a **second terminal**:

```bash
cd frontend
npm install
npm run dev      # Vite dev server on :3000, proxies /api/* → http://localhost:8000
```

Then open <http://localhost:3000> and sign in with one of the demo accounts.

### 5. Redis (optional)

Redis is the only containerized dependency. The app falls back to in-memory
cart/cache if it can't connect, so this step is optional for local dev.

```bash
docker-compose up -d redis
```

### Verifying the install

After running steps 1–4, you should be able to:

| Check | Expected |
|-------|----------|
| `curl http://localhost:8000/api/health` | `{"status":"ok"}` |
| `curl http://localhost:8000/api/books?limit=1` | One book in the `books` array (after `seed_books`) |
| Sign in as `admin` / `admin123` at `/login` | Lands on home, "Admin Panel" appears in user menu |
| Approve a pending loan in `/admin` | Loan flips to APPROVED, audit log row appears |

### Troubleshooting

<details>
<summary><b><code>(1305) PROCEDURE … does not exist</code> when approving a loan</b></summary>

Migration `0002_triggers_and_procedures` never ran. Fix:
```
alembic current        # should show 0002_triggers_and_procedures (head)
alembic upgrade head   # idempotent — drops + recreates triggers/procs
```
If `alembic current` is empty but the tables exist (e.g. you loaded
`openlibrary_schema.sql` by hand), stamp 0001 first:
```
alembic stamp 0001_initial
alembic upgrade head
```
</details>

<details>
<summary><b><code>(1045) Access denied for user 'root'@'localhost'</code></b></summary>

`MYSQL_URL` is missing, has the wrong password, or is set in a different
terminal from the one running alembic/uvicorn. Re-check with
`echo $env:MYSQL_URL` (PowerShell) or `echo $MYSQL_URL` (bash) **in the same
shell** you're about to run the command in.
</details>

<details>
<summary><b>Frontend shows no books on the home page</b></summary>

You skipped `python -m app.scripts.seed_books`. The home page now serves from
the local catalog first; without seeding, it falls back to OpenLibrary's
trending endpoint, which can be slow or rate-limited.
</details>

<details>
<summary><b>Port 3000 or 8000 already in use</b></summary>

Backend: `python -m uvicorn app.main:app --port 8001`<br>
Frontend: edit `frontend/vite.config.js` `server.port` and the `/api` proxy
target if you also moved the backend.
</details>

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
│   └── scripts/                 # seed.py (demo users), seed_books.py (300 books from OpenLibrary)
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
