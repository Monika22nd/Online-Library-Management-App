# Online Library Management App (Python/React)

A modern library management system built with FastAPI, React, Redis, and Docker.

## Features
- **OpenLibrary Integration**: Search and import books directly from OpenLibrary.
- **Redis Caching**: Faster API responses and real-time cart management.
- **User Management**: Authentication and role-based access (USER/ADMIN).
- **Loan System**: Borrow books for 14 days with approval workflow.
- **Docker Ready**: Easy deployment with `docker-compose`.

## Tech Stack
- **Backend**: FastAPI (Python 3.11)
- **Frontend**: React (Vite)
- **Database**: SQLite (Core) + Redis (Cache/Cart)
- **Containerization**: Docker & Docker Compose

## Quick Start
```bash
docker-compose up --build
```
The app will be available at `http://localhost`.
