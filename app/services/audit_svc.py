from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import AuditLog


def get_audit_log(session: Session, limit: int = 50) -> list[dict]:
    rows = session.scalars(
        select(AuditLog).order_by(AuditLog.changed_at.desc()).limit(limit)
    ).all()
    return [
        {
            "id": r.audit_id,
            "table_name": r.table_name,
            "record_id": r.record_id,
            "action": r.action,
            "old_value": r.old_value,
            "new_value": r.new_value,
            "changed_by_id": r.changed_by_id,
            "changed_at": r.changed_at.isoformat() if r.changed_at else None,
        }
        for r in rows
    ]
