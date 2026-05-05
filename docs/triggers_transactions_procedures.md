# Triggers, Transactions & Procedures — Cheatsheet

Quick-reference summary of all database logic in this project across **SQLite**, **Redis (Lua)**, and **Python**.

---

## SQLite Triggers

| Trigger | Event | Purpose |
|---------|-------|---------|
| `check_max_loans` | `BEFORE INSERT ON loans` | Blocks insert if user already has ≥5 active loans (PENDING/APPROVED). Uses `RAISE(ABORT)`. |
| `auto_request_date` | `BEFORE INSERT ON loans` | Auto-fills `request_date = date('now')` when it's NULL. |
| `loan_status_audit` | `AFTER UPDATE ON loans` | Logs every status change to `audit_log` table (old → new status, who changed it). |

### Code

```sql
-- 1. Max Loans Enforcement
CREATE TRIGGER IF NOT EXISTS check_max_loans
BEFORE INSERT ON loans
FOR EACH ROW
WHEN NEW.status IN ('PENDING', 'APPROVED')
BEGIN
    SELECT CASE
        WHEN (SELECT COUNT(*) FROM loans
              WHERE user_id = NEW.user_id
              AND status IN ('PENDING', 'APPROVED')) >= 5
        THEN RAISE(ABORT, 'User has reached maximum active loans (5)')
    END;
END;

-- 2. Auto-fill Request Date
CREATE TRIGGER IF NOT EXISTS auto_request_date
BEFORE INSERT ON loans
FOR EACH ROW
WHEN NEW.request_date IS NULL
BEGIN
    UPDATE loans SET request_date = date('now') WHERE id = NEW.id;
END;

-- 3. Audit Trail for Status Changes
CREATE TRIGGER IF NOT EXISTS loan_status_audit
AFTER UPDATE ON loans
FOR EACH ROW
WHEN OLD.status != NEW.status
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, old_value, new_value, changed_by_id)
    VALUES ('loans', NEW.id, 'STATUS_CHANGE', OLD.status, NEW.status, NEW.approved_by_id);
END;
```

---

## Redis Lua Scripts (Atomic Procedures)

| Script | Purpose | Atomicity |
|--------|---------|-----------|
| `LUA_ADD_TO_CART` | Add item to cart list if not already present | Single EVAL = atomic |
| `LUA_CHECKOUT` | Get cart count and delete in one operation | Single EVAL = atomic |
| `LUA_ACQUIRE_LOCK` | Distributed lock with `SET NX EX` | Single EVAL = atomic |
| `LUA_RELEASE_LOCK` | Release lock only if caller is the owner | Single EVAL = atomic |

### Code

```lua
-- Add to cart (duplicate check)
local items = redis.call('LRANGE', KEYS[1], 0, -1)
for i, v in ipairs(items) do
    if v == ARGV[1] then return 0 end
end
redis.call('RPUSH', KEYS[1], ARGV[1])
return 1

-- Checkout (count + delete)
local count = redis.call('LLEN', KEYS[1])
redis.call('DEL', KEYS[1])
return count

-- Acquire lock
local result = redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')
if result then return 1 else return 0 end

-- Release lock (owner check)
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
return 0
```

---

## Python Transactions (SQLite)

| Function | What It Does | Atomic? |
|----------|-------------|---------|
| `approve_loan()` | Updates loan status + decrements book copies | Yes — single `conn.commit()` |
| `return_loan()` | Sets RETURNED + increments book copies | Yes |
| `cancel_loan()` | Sets CANCELLED + restores copies if was APPROVED | Yes |
| `transfer_loan()` | Reassigns loan to a different user | Yes |
| `bulk_approve_loans()` | Approves multiple loans in a single transaction | Yes — one `commit()` for all |

All functions use the pattern:
```python
with get_db() as conn:
    # ... multiple SQL statements ...
    conn.commit()  # atomic commit
```

---

## In-Memory Fallback

When Redis is unavailable, all cart/cache/lock operations fall back to Python dictionaries. This keeps the app functional without Redis but loses persistence across restarts.
