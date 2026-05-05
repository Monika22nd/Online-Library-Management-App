# Database Scripts

Scripts for SQLite (main database) and Redis (side database).

---

## SQLite Scripts

SQLite has limited stored procedure support. Use triggers and transactions instead.

### Triggers

```sql
-- Prevent user from having more than 5 active loans
CREATE TRIGGER IF NOT EXISTS check_max_loans
BEFORE INSERT ON loans
FOR EACH ROW
WHEN NEW.status IN ('PENDING', 'APPROVED')
BEGIN
    SELECT CASE
        WHEN (SELECT COUNT(*) FROM loans WHERE user_id = NEW.user_id AND status IN ('PENDING', 'APPROVED')) >= 5
        THEN RAISE(ABORT, 'User has reached maximum active loans (5)')
    END;
END;

-- Auto-set request date
CREATE TRIGGER IF NOT EXISTS auto_request_date
BEFORE INSERT ON loans
FOR EACH ROW
WHEN NEW.request_date IS NULL
BEGIN
    UPDATE loans SET request_date = date('now') WHERE id = NEW.id;
END;

-- Audit loan status changes
CREATE TRIGGER IF NOT EXISTS loan_status_audit
AFTER UPDATE ON loans
FOR EACH ROW
WHEN OLD.status != NEW.status
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, old_value, new_value, changed_by_id)
    VALUES ('loans', NEW.id, 'STATUS_CHANGE', OLD.status, NEW.status, NEW.approved_by_id);
END;
```

### Transactions (Manual)

```python
# Approve loan (transaction in Python)
def approve_loan(loan_id: int, admin_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan or loan['status'] != 'PENDING':
            return False
        
        book = conn.execute('SELECT * FROM books WHERE id = ?', (loan['book_id'],)).fetchone()
        if not book or book['available_copies'] <= 0:
            conn.execute('UPDATE loans SET status = ?, notes = ? WHERE id = ?', 
                      ('REJECTED', 'No copies available', loan_id))
            conn.commit()
            return False
        
        # Use SAVEPOINT for atomicity
        conn.execute('''
            UPDATE loans SET status = 'APPROVED', approved_by_id = ?, borrow_date = date('now'), due_date = date('now', '+14 days')
            WHERE id = ?
        ''', (admin_id, loan_id))
        
        conn.execute('UPDATE books SET available_copies = available_copies - 1 WHERE id = ?', (loan['book_id'],))
        conn.commit()
        return True
```

```python
# Cancel loan with inventory restore
def cancel_loan(loan_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan:
            return False
        
        if loan['status'] == 'PENDING':
            conn.execute('UPDATE loans SET status = ? WHERE id = ?', ('CANCELLED', loan_id))
            conn.commit()
            return True
        elif loan['status'] == 'APPROVED':
            conn.execute('UPDATE loans SET status = ? WHERE id = ?', ('CANCELLED', loan_id))
            conn.execute('UPDATE books SET available_copies = available_copies + 1 WHERE id = ?', (loan['book_id'],))
            conn.commit()
            return True
        return False
```

```python
# Return book
def return_loan(loan_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan or loan['status'] != 'APPROVED':
            return False
        
        conn.execute('UPDATE loans SET status = ?, return_date = date("now") WHERE id = ?', ('RETURNED', loan_id))
        conn.execute('UPDATE books SET available_copies = available_copies + 1 WHERE id = ?', (loan['book_id'],))
        conn.commit()
        return True
```

```python
# Transfer loan ownership
def transfer_loan(loan_id: int, new_user_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan or loan['status'] != 'PENDING':
            return False
        
        new_user = conn.execute('SELECT id FROM users WHERE id = ?', (new_user_id,)).fetchone()
        if not new_user:
            return False
        
        conn.execute('UPDATE loans SET user_id = ? WHERE id = ?', (new_user_id, loan_id))
        conn.commit()
        return True
```

```python
# Bulk approve
def bulk_approve_loans(loan_ids: List[int], admin_id: int) -> int:
    approved = 0
    with get_db() as conn:
        for loan_id in loan_ids:
            loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
            if not loan or loan['status'] != 'PENDING':
                continue
            
            book = conn.execute('SELECT * FROM books WHERE id = ?', (loan['book_id'],)).fetchone()
            if not book or book['available_copies'] <= 0:
                conn.execute('UPDATE loans SET status = ?, notes = ? WHERE id = ?', 
                          ('REJECTED', 'No copies available', loan_id))
                continue
            
            conn.execute('''
                UPDATE loans SET status = 'APPROVED', approved_by_id = ?, borrow_date = date('now'), due_date = date('now', '+14 days')
                WHERE id = ?
            ''', (admin_id, loan_id))
            conn.execute('UPDATE books SET available_copies = available_copies - 1 WHERE id = ?', (loan['book_id'],))
            approved += 1
        
        conn.commit()
        return approved
```

---

## Redis Lua Scripts

Redis uses Lua for atomic operations. Loaded via `redis_client.eval()`.

### Cart Operations

```lua
-- Atomic add to cart (check duplicates)
local items = redis.call('LRANGE', KEYS[1], 0, -1)
for i, v in ipairs(items) do
    if v == ARGV[1] then return 0 end
end
redis.call('RPUSH', KEYS[1], ARGV[1])
return 1
```

```lua
-- Atomic checkout (clear cart, return count)
local count = redis.call('LLEN', KEYS[1])
redis.call('DEL', KEYS[1])
return count
```

### Rate Limiting

```lua
-- Check rate limit
local current = redis.call('GET', KEYS[1])
if current and tonumber(current) >= tonumber(ARGV[1]) then
    return 0
end
if current then
    redis.call('INCR', KEYS[1])
else
    redis.call('SET', KEYS[1], 1)
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return 1
```

### Distributed Lock

```lua
-- Acquire lock
local result = redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')
if result then return 1 else return 0 end
```

```lua
-- Release lock
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
return 0
```

---

## Python Usage

```python
import app.services.redis_service as redis

# Cart
redis.add_to_cart(user_id, "5")
cart = redis.get_cart(user_id)
redis.clear_cart(user_id)

# Atomic
redis.add_to_cart_atomic(user_id, "5")
redis.checkout_cart_atomic(user_id)

# Lock
if redis.acquire_lock("resource", "owner123", 10):
    try:
        # do work
    finally:
        redis.release_lock("resource", "owner123")

# Rate limit
if redis.check_rate_limit("/api/books", user_id):
    raise HTTPException(429, "Too many requests")
```