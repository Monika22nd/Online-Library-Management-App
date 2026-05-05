# Database Scripts — Full Code Reference

Complete source code for all triggers, transactions, and procedures in the project.

---

## 1. SQLite Schema

```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER',
    email TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS books (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    openlibrary_key TEXT UNIQUE,
    title TEXT NOT NULL,
    author TEXT,
    isbn TEXT,
    cover_url TEXT,
    description TEXT,
    publish_year INTEGER,
    subjects TEXT,
    total_copies INTEGER DEFAULT 1,
    available_copies INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    status TEXT DEFAULT 'PENDING',
    request_date TEXT,
    borrow_date TEXT,
    due_date TEXT,
    return_date TEXT,
    approved_by_id INTEGER,
    notes TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (approved_by_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_name TEXT,
    record_id INTEGER,
    action TEXT,
    old_value TEXT,
    new_value TEXT,
    changed_by_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Indexes

```sql
CREATE INDEX IF NOT EXISTS idx_loans_user_id ON loans(user_id);
CREATE INDEX IF NOT EXISTS idx_loans_book_id ON loans(book_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
```

---

## 2. SQLite Triggers

```sql
-- TRIGGER 1: Prevent user from having more than 5 active loans
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

-- TRIGGER 2: Auto-set request_date to today if NULL
CREATE TRIGGER IF NOT EXISTS auto_request_date
BEFORE INSERT ON loans
FOR EACH ROW
WHEN NEW.request_date IS NULL
BEGIN
    UPDATE loans SET request_date = date('now') WHERE id = NEW.id;
END;

-- TRIGGER 3: Log every loan status change to audit_log
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

## 3. Python Transactions

### Approve Loan

```python
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

        conn.execute('''
            UPDATE loans SET status = 'APPROVED', approved_by_id = ?,
            borrow_date = date('now'), due_date = date('now', '+14 days')
            WHERE id = ?
        ''', (admin_id, loan_id))

        conn.execute('UPDATE books SET available_copies = available_copies - 1 WHERE id = ?',
                    (loan['book_id'],))
        conn.commit()
        return True
```

### Return Loan

```python
def return_loan(loan_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan or loan['status'] != 'APPROVED':
            return False

        conn.execute('UPDATE loans SET status = ?, return_date = date("now") WHERE id = ?',
                    ('RETURNED', loan_id))
        conn.execute('UPDATE books SET available_copies = available_copies + 1 WHERE id = ?',
                    (loan['book_id'],))
        conn.commit()
        return True
```

### Cancel Loan

```python
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
            conn.execute('UPDATE books SET available_copies = available_copies + 1 WHERE id = ?',
                        (loan['book_id'],))
            conn.commit()
            return True
        return False
```

### Transfer Loan

```python
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

### Bulk Approve

```python
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
                UPDATE loans SET status = 'APPROVED', approved_by_id = ?,
                borrow_date = date('now'), due_date = date('now', '+14 days')
                WHERE id = ?
            ''', (admin_id, loan_id))
            conn.execute('UPDATE books SET available_copies = available_copies - 1 WHERE id = ?',
                        (loan['book_id'],))
            approved += 1

        conn.commit()  # Single commit for all approvals = atomic batch
        return approved
```

---

## 4. Redis Lua Scripts (Atomic Procedures)

### Add to Cart (Duplicate Prevention)

```lua
-- KEYS[1] = cart key, ARGV[1] = book_id
local items = redis.call('LRANGE', KEYS[1], 0, -1)
for i, v in ipairs(items) do
    if v == ARGV[1] then return 0 end
end
redis.call('RPUSH', KEYS[1], ARGV[1])
return 1
```

### Checkout (Count + Delete)

```lua
-- KEYS[1] = cart key
local count = redis.call('LLEN', KEYS[1])
redis.call('DEL', KEYS[1])
return count
```

### Acquire Distributed Lock

```lua
-- KEYS[1] = lock key, ARGV[1] = owner, ARGV[2] = timeout
local result = redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')
if result then return 1 else return 0 end
```

### Release Distributed Lock (Owner Check)

```lua
-- KEYS[1] = lock key, ARGV[1] = owner
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
return 0
```

---

## 5. Python Usage Examples

```python
import app.services.redis_service as redis_svc

# Cart operations
redis_svc.add_to_cart(user_id, "5")
cart = redis_svc.get_cart(user_id)
redis_svc.clear_cart(user_id)

# Atomic cart operations (Lua scripts)
redis_svc.add_to_cart_atomic(user_id, "5")
count = redis_svc.checkout_cart_atomic(user_id)

# Distributed locking
if redis_svc.acquire_lock_atomic("resource", "owner123", 10):
    try:
        # do exclusive work
        pass
    finally:
        redis_svc.release_lock_atomic("resource", "owner123")

# Caching
redis_svc.set_cache("trending:20", data, ttl=1800)
cached = redis_svc.get_cache("trending:20")

# Rate limiting
if redis_svc.is_rate_limited(f"rate:{user_id}", max_requests=10, window=60):
    raise HTTPException(429, "Too many requests")
```