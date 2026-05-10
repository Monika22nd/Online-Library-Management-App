from datetime import date
from passlib.hash import bcrypt
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import User, Member


def get_user_by_username(session: Session, username: str) -> User | None:
    return session.scalar(select(User).where(User.username == username))


def get_user_by_id(session: Session, user_id: int) -> User | None:
    return session.get(User, user_id)


def verify_password(session: Session, username: str, password: str) -> User | None:
    user = get_user_by_username(session, username)
    if not user:
        return None
    if not bcrypt.verify(password, user.password_hash):
        return None
    return user


def create_user(
    session: Session,
    username: str,
    password: str,
    email: str | None,
    role: str = "USER",
    create_member: bool = True,
) -> User:
    """Register a user. By default also creates a paired `members` row so the
    user can immediately borrow books — admins typically pass create_member=False."""
    member_id = None
    if create_member:
        member = Member(
            member_code=f"M-{username}",
            member_name=username,
            email=email,
            registration_date=date.today(),
        )
        session.add(member)
        session.flush()
        member_id = member.member_id

    user = User(
        username=username,
        password_hash=bcrypt.hash(password),
        email=email,
        role=role,
        member_id=member_id,
    )
    session.add(user)
    session.commit()
    session.refresh(user)
    return user


def user_to_dict(user: User) -> dict:
    return {
        "id": user.user_id,
        "username": user.username,
        "email": user.email,
        "role": user.role,
        "member_id": user.member_id,
    }
