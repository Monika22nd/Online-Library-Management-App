# Online Library Management App

A modern library management system built with **FastAPI**, **React**, **Redis**, and **Docker**.

## Features

- **OpenLibrary Integration** — Search and browse millions of books via the OpenLibrary API
- **Redis Caching** — API response caching and atomic cart operations (falls back to in-memory when Redis is unavailable)
- **User Management** — Authentication and role-based access (USER / ADMIN)
- **Loan System** — Borrow books for 14 days with admin approval workflow
- **Admin Panel** — Dashboard to approve/reject/bulk-approve loan requests and view audit logs
- **SQLite Triggers** — Enforce max loans, auto-fill dates, and audit status changes at the database level
- **Docker Ready** — One-command deployment with `docker-compose`

## Tech Stack

| Layer      | Technology            |
|------------|----------------------|
| Backend    | FastAPI (Python 3.11) |
| Frontend   | React 19 + Vite       |
| Database   | SQLite + Redis        |
| Deployment | Docker Compose        |

## Quick Start

### Local Development (no Docker)

```bash
# Backend
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000

# Frontend (in a separate terminal)
cd frontend
npm install
npm run dev
# Visit http://localhost:3000
```

> Redis is **optional** locally. Without it, the app uses an in-memory fallback for cart and caching.

### Docker

```bash
docker-compose up --build
# Visit http://localhost (port 80)
```

## Demo Accounts

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| john     | user123   | USER  |

## Project Structure

```
├── app/
│   ├── main.py                  # FastAPI entry point & routes
│   ├── models/database.py       # SQLite schema, triggers, CRUD
│   └── services/
│       ├── openlibrary.py       # OpenLibrary API integration
│       └── redis_service.py     # Redis + in-memory fallback
├── frontend/                    # React SPA (Vite)
├── docs/                        # Documentation & cheatsheets
├── docker-compose.yml           # Container orchestration
├── Dockerfile.api               # Backend container
├── Dockerfile.frontend          # Frontend build + Nginx
└── nginx.conf                   # Production reverse proxy
```

## Documentation

- [`docs/DATABASE-SCRIPTS.md`](docs/DATABASE-SCRIPTS.md) — Full code reference for all triggers, transactions, and procedures
- [`docs/triggers_transactions_procedures.md`](docs/triggers_transactions_procedures.md) — Summary cheatsheet of database logic
- [`docs/exam_cheatsheet.md`](docs/exam_cheatsheet.md) — Academic-style reference with explanations for exam preparation
