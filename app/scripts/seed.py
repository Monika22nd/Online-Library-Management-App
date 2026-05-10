"""Seed the MySQL database with demo users.

Run after `alembic upgrade head`:

    python -m app.scripts.seed

Idempotent: skips users that already exist. Admin user is created without
a member record (admins don't borrow); regular users get a paired member.
"""
from app.models import SessionLocal
from app.services import auth_svc


DEMO_USERS = [
    # (username, password, email, role, create_member)
    ("admin", "admin123", "admin@library.com", "ADMIN", False),
    ("john",  "user123",  "john@example.com",  "USER",  True),
    ("jane",  "user123",  "jane@example.com",  "USER",  True),
]


def seed_users() -> int:
    created = 0
    with SessionLocal() as session:
        for username, password, email, role, create_member in DEMO_USERS:
            if auth_svc.get_user_by_username(session, username):
                continue
            auth_svc.create_user(
                session, username, password, email, role,
                create_member=create_member,
            )
            created += 1
    return created


if __name__ == "__main__":
    n = seed_users()
    print(f"Seeded {n} new user(s).")
