# Alembic migrations

Set `MYSQL_URL` first:

```bash
export MYSQL_URL="mysql+pymysql://root:@localhost:3306/library_openlibrary?charset=utf8mb4"
```

Common commands:

```bash
alembic upgrade head                   # apply all pending migrations
alembic downgrade -1                   # roll back one revision
alembic revision --autogenerate -m "msg"   # generate a new revision from model changes
alembic current                        # show current DB revision
alembic history                        # full revision history
```

Revisions live in `alembic/versions/`. The DDL for triggers and stored
procedures is also kept as a human-readable copy at
`db/sql/triggers_and_procedures.sql`.
