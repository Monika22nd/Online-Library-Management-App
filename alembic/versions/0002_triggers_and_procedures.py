"""triggers and procedures

Applies the trigger + stored-procedure DDL. The same SQL lives in
db/sql/triggers_and_procedures.sql for reading; THIS file is what actually
runs against the database. Keep them in sync.

We can't use a single op.execute() for the whole script because PyMySQL only
sends one statement per call and the procs use BEGIN/END blocks that span
multiple semicolons. Each CREATE TRIGGER / CREATE PROCEDURE is therefore
issued separately.

Revision ID: 0002_triggers_and_procedures
Revises: 0001_initial
Create Date: 2026-05-09
"""
from typing import Sequence, Union

from alembic import op


revision: str = "0002_triggers_and_procedures"
down_revision: Union[str, None] = "0001_initial"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


TRIGGER_CHECK_MAX = """
CREATE TRIGGER trg_loans_check_max
BEFORE INSERT ON loans
FOR EACH ROW
BEGIN
  IF NEW.approval_status IN ('PENDING', 'APPROVED') THEN
    IF (SELECT COUNT(*) FROM loans
         WHERE member_id = NEW.member_id
           AND approval_status IN ('PENDING', 'APPROVED')) >= 5 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'User has reached maximum active loans (5)';
    END IF;
  END IF;
END
"""

TRIGGER_STATUS_AUDIT = """
CREATE TRIGGER trg_loans_status_audit
AFTER UPDATE ON loans
FOR EACH ROW
BEGIN
  IF OLD.approval_status <> NEW.approval_status THEN
    INSERT INTO audit_log
      (table_name, record_id, action, old_value, new_value, changed_by_id)
    VALUES
      ('loans', NEW.loan_id, 'STATUS_CHANGE',
       OLD.approval_status, NEW.approval_status, NEW.approved_by_id);
  END IF;
END
"""

PROC_APPROVE_LOAN = """
CREATE PROCEDURE sp_approve_loan(
  IN p_loan_id  BIGINT UNSIGNED,
  IN p_admin_id BIGINT UNSIGNED
)
BEGIN
  DECLARE v_book_id BIGINT UNSIGNED;
  DECLARE v_copy_id BIGINT UNSIGNED;
  DECLARE v_status  VARCHAR(16);

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

    SELECT approval_status, requested_book_id
      INTO v_status, v_book_id
      FROM loans
     WHERE loan_id = p_loan_id
     FOR UPDATE;

    IF v_status = 'PENDING' AND v_book_id IS NOT NULL THEN
      SELECT c.copy_id
        INTO v_copy_id
        FROM copies c
        JOIN editions e ON e.edition_id = c.edition_id
       WHERE e.book_id = v_book_id
         AND c.circulation_status = 'available'
       LIMIT 1
       FOR UPDATE;

      IF v_copy_id IS NULL THEN
        UPDATE loans
           SET approval_status = 'REJECTED',
               approved_by_id  = p_admin_id,
               notes           = 'No copies available'
         WHERE loan_id = p_loan_id;
      ELSE
        UPDATE loans
           SET approval_status = 'APPROVED',
               loan_status     = 'open',
               copy_id         = v_copy_id,
               approved_by_id  = p_admin_id,
               borrow_date     = CURRENT_DATE,
               due_date        = DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY)
         WHERE loan_id = p_loan_id;

        UPDATE copies
           SET circulation_status = 'on_loan'
         WHERE copy_id = v_copy_id;
      END IF;
    END IF;

  COMMIT;
END
"""

PROC_RETURN_LOAN = """
CREATE PROCEDURE sp_return_loan(
  IN p_loan_id BIGINT UNSIGNED
)
BEGIN
  DECLARE v_copy_id BIGINT UNSIGNED;
  DECLARE v_status  VARCHAR(16);

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

    SELECT approval_status, copy_id
      INTO v_status, v_copy_id
      FROM loans
     WHERE loan_id = p_loan_id
     FOR UPDATE;

    IF v_status = 'APPROVED' AND v_copy_id IS NOT NULL THEN
      UPDATE loans
         SET approval_status = 'RETURNED',
             loan_status     = 'returned',
             return_date     = CURRENT_DATE
       WHERE loan_id = p_loan_id;

      UPDATE copies
         SET circulation_status = 'available'
       WHERE copy_id = v_copy_id;
    END IF;

  COMMIT;
END
"""

PROC_CANCEL_LOAN = """
CREATE PROCEDURE sp_cancel_loan(
  IN p_loan_id BIGINT UNSIGNED
)
BEGIN
  DECLARE v_copy_id BIGINT UNSIGNED;
  DECLARE v_status  VARCHAR(16);

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

    SELECT approval_status, copy_id
      INTO v_status, v_copy_id
      FROM loans
     WHERE loan_id = p_loan_id
     FOR UPDATE;

    IF v_status IN ('PENDING', 'APPROVED') THEN
      UPDATE loans
         SET approval_status = 'CANCELLED'
       WHERE loan_id = p_loan_id;

      IF v_status = 'APPROVED' AND v_copy_id IS NOT NULL THEN
        UPDATE copies
           SET circulation_status = 'available'
         WHERE copy_id = v_copy_id;
      END IF;
    END IF;

  COMMIT;
END
"""

PROC_BULK_APPROVE = """
CREATE PROCEDURE sp_bulk_approve_loans(
  IN p_loan_ids  TEXT,
  IN p_admin_id  BIGINT UNSIGNED
)
BEGIN
  DECLARE v_next   INT;
  DECLARE v_token  VARCHAR(32);
  DECLARE v_loan   BIGINT UNSIGNED;
  DECLARE v_remain TEXT;

  SET v_remain = p_loan_ids;

  WHILE CHAR_LENGTH(v_remain) > 0 DO
    SET v_next = LOCATE(',', v_remain);
    IF v_next = 0 THEN
      SET v_token  = TRIM(v_remain);
      SET v_remain = '';
    ELSE
      SET v_token  = TRIM(SUBSTRING(v_remain, 1, v_next - 1));
      SET v_remain = SUBSTRING(v_remain, v_next + 1);
    END IF;

    IF v_token <> '' THEN
      SET v_loan = CAST(v_token AS UNSIGNED);
      CALL sp_approve_loan(v_loan, p_admin_id);
    END IF;
  END WHILE;
END
"""


def upgrade() -> None:
    # Drop first to make this revision idempotent on partial failures.
    op.execute("DROP TRIGGER IF EXISTS trg_loans_check_max")
    op.execute("DROP TRIGGER IF EXISTS trg_loans_status_audit")
    op.execute("DROP PROCEDURE IF EXISTS sp_approve_loan")
    op.execute("DROP PROCEDURE IF EXISTS sp_return_loan")
    op.execute("DROP PROCEDURE IF EXISTS sp_cancel_loan")
    op.execute("DROP PROCEDURE IF EXISTS sp_bulk_approve_loans")

    op.execute(TRIGGER_CHECK_MAX)
    op.execute(TRIGGER_STATUS_AUDIT)
    op.execute(PROC_APPROVE_LOAN)
    op.execute(PROC_RETURN_LOAN)
    op.execute(PROC_CANCEL_LOAN)
    op.execute(PROC_BULK_APPROVE)


def downgrade() -> None:
    op.execute("DROP PROCEDURE IF EXISTS sp_bulk_approve_loans")
    op.execute("DROP PROCEDURE IF EXISTS sp_cancel_loan")
    op.execute("DROP PROCEDURE IF EXISTS sp_return_loan")
    op.execute("DROP PROCEDURE IF EXISTS sp_approve_loan")
    op.execute("DROP TRIGGER IF EXISTS trg_loans_status_audit")
    op.execute("DROP TRIGGER IF EXISTS trg_loans_check_max")
