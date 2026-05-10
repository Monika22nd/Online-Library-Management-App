"""Alembic environment.

Reads the MySQL connection string from $MYSQL_URL so the same alembic.ini
works for dev / docker / prod without edits. Imports `app.models` so every
model is registered against `Base.metadata` for autogenerate.
"""
import os
import sys
from logging.config import fileConfig

from sqlalchemy import engine_from_config, pool
from alembic import context

# Make `app` importable when alembic runs from the project root.
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__) + "/.."))

from app.models import Base  # noqa: E402  (registers all models)

config = context.config
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

mysql_url = os.environ.get("MYSQL_URL")
if not mysql_url:
    raise RuntimeError(
        "MYSQL_URL is not set. Example: "
        "mysql+pymysql://root:@localhost:3306/library_openlibrary?charset=utf8mb4"
    )
config.set_main_option("sqlalchemy.url", mysql_url)

target_metadata = Base.metadata


def run_migrations_offline() -> None:
    context.configure(
        url=mysql_url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
        compare_type=True,
    )
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    connectable = engine_from_config(
        config.get_section(config.config_ini_section, {}),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )
    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
            compare_type=True,
        )
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
