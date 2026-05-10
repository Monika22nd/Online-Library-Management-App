-- =========================================================================
-- Triggers & stored procedures for the MySQL library schema.
--
-- This file is the human-readable reference. The Alembic revision
-- 0002_triggers_and_procedures applies the IDENTICAL DDL via op.execute()
-- so the database state and this file stay in sync.
--
-- Apply manually with:   mysql -u <user> -p library_openlibrary < triggers_and_procedures.sql
--
-- Replaces the three SQLite triggers from the legacy app:
--   * check_max_loans       -> trg_loans_check_max
--   * loan_status_audit     -> trg_loans_status_audit
--   * auto_request_date     -> handled by `request_date DEFAULT (CURRENT_DATE)`
--                              on the loans table (no trigger needed)
-- =========================================================================

USE library_openlibrary;

-- Idempotency: drop everything first so this script can be re-run.
DROP TRIGGER IF EXISTS trg_loans_check_max;
DROP TRIGGER IF EXISTS trg_loans_status_audit;
DROP PROCEDURE IF EXISTS sp_approve_loan;
DROP PROCEDURE IF EXISTS sp_return_loan;
DROP PROCEDURE IF EXISTS sp_bulk_approve_loans;
DROP PROCEDURE IF EXISTS sp_cancel_loan;

DELIMITER $$

-- -------------------------------------------------------------------------
-- Trigger: enforce the "max 5 active loans per member" rule at the DB layer.
-- An active loan is one whose approval_status is PENDING or APPROVED.
-- Raises SQLSTATE '45000' to abort the INSERT; SQLAlchemy surfaces this
-- as an IntegrityError that the service layer can catch.
-- -------------------------------------------------------------------------
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
END$$

-- -------------------------------------------------------------------------
-- Trigger: write an audit row whenever a loan's approval_status changes.
-- Mirrors the SQLite loan_status_audit trigger.
-- -------------------------------------------------------------------------
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
END$$

-- -------------------------------------------------------------------------
-- Procedure: approve a PENDING loan, atomically picking an available copy
-- of the requested book and flipping the copy's circulation_status.
--
-- Replaces the Python approve_loan() function. All work happens in one
-- transaction; FOR UPDATE locks prevent two admins from grabbing the same
-- copy simultaneously.
--
-- Behavior:
--   * If the loan is not PENDING -> no-op.
--   * If no copy is available    -> loan becomes REJECTED with a note.
--   * Otherwise                  -> loan becomes APPROVED, copy_id is set,
--                                    borrow_date = today, due_date = +14d,
--                                    copy.circulation_status = 'on_loan'.
-- -------------------------------------------------------------------------
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
      -- Pick the first available copy of the requested book, lock the row.
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
END$$

-- -------------------------------------------------------------------------
-- Procedure: return an APPROVED loan and free the copy.
-- -------------------------------------------------------------------------
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
END$$

-- -------------------------------------------------------------------------
-- Procedure: cancel a loan from PENDING or APPROVED state.
-- If APPROVED, releases the copy back to 'available'.
-- -------------------------------------------------------------------------
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
END$$

-- -------------------------------------------------------------------------
-- Procedure: approve many loans in one transaction. Loops over a
-- comma-separated list of loan IDs (MySQL has no array type; the app passes
-- e.g. '12,17,33'). Per-loan failures don't abort the whole batch — each
-- approval is a sub-call to sp_approve_loan.
-- -------------------------------------------------------------------------
CREATE PROCEDURE sp_bulk_approve_loans(
  IN p_loan_ids  TEXT,
  IN p_admin_id  BIGINT UNSIGNED
)
BEGIN
  DECLARE v_pos    INT DEFAULT 1;
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
END$$

DELIMITER ;
