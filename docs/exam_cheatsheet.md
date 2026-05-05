# Database Concepts — Exam Cheatsheet

Academic reference for **Triggers**, **Transactions**, and **Stored Procedures** as implemented in a real-world Library Management System using SQLite, Redis, and Python.

---

## Part 1: Triggers

### What is a Trigger?

A trigger is a **database object** that automatically executes a specified set of SQL statements in response to certain events (INSERT, UPDATE, DELETE) on a particular table. Triggers enforce business rules at the database level, ensuring data integrity regardless of which application accesses the data.

### Trigger Syntax (SQLite)

```sql
CREATE TRIGGER trigger_name
{BEFORE | AFTER} {INSERT | UPDATE | DELETE} ON table_name
FOR EACH ROW
[WHEN condition]
BEGIN
    -- SQL statements
END;
```

### Example 1: Validation Trigger (BEFORE INSERT)

**Requirement:** A user cannot have more than 5 active loans at any time.

```sql
CREATE TRIGGER check_max_loans
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
```

**Key Concepts:**
- `BEFORE INSERT` — Fires before the row is inserted, allowing us to abort
- `NEW.user_id` — References the value being inserted
- `RAISE(ABORT, ...)` — Rolls back the entire statement and returns an error
- The subquery counts existing active loans for the same user

### Example 2: Default Value Trigger (BEFORE INSERT)

**Requirement:** If no request date is provided, auto-fill with today's date.

```sql
CREATE TRIGGER auto_request_date
BEFORE INSERT ON loans
FOR EACH ROW
WHEN NEW.request_date IS NULL
BEGIN
    UPDATE loans SET request_date = date('now') WHERE id = NEW.id;
END;
```

**Key Concepts:**
- `WHEN` clause makes the trigger conditional
- `date('now')` is SQLite's built-in function for the current date

### Example 3: Audit Trail Trigger (AFTER UPDATE)

**Requirement:** Every time a loan's status changes, log the change.

```sql
CREATE TRIGGER loan_status_audit
AFTER UPDATE ON loans
FOR EACH ROW
WHEN OLD.status != NEW.status
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, old_value, new_value, changed_by_id)
    VALUES ('loans', NEW.id, 'STATUS_CHANGE', OLD.status, NEW.status, NEW.approved_by_id);
END;
```

**Key Concepts:**
- `AFTER UPDATE` — Fires after the row has been modified
- `OLD.status` — The value before the update
- `NEW.status` — The value after the update
- This creates a full audit trail for compliance and debugging

---

## Part 2: Transactions

### What is a Transaction?

A transaction is a **sequence of database operations** that are treated as a single atomic unit. Either all operations succeed (COMMIT) or all fail (ROLLBACK). Transactions guarantee the **ACID** properties:

| Property | Meaning |
|----------|---------|
| **Atomicity** | All-or-nothing execution |
| **Consistency** | Database moves from one valid state to another |
| **Isolation** | Concurrent transactions don't interfere |
| **Durability** | Committed data survives crashes |

### Example 1: Multi-Table Transaction (Approve Loan)

**Requirement:** When approving a loan, update the loan record AND decrement the book's available copies — atomically.

```python
def approve_loan(loan_id: int, admin_id: int) -> bool:
    with get_db() as conn:
        # Step 1: Validate the loan exists and is PENDING
        loan = conn.execute(
            'SELECT * FROM loans WHERE id = ?', (loan_id,)
        ).fetchone()
        if not loan or loan['status'] != 'PENDING':
            return False

        # Step 2: Validate book has available copies
        book = conn.execute(
            'SELECT * FROM books WHERE id = ?', (loan['book_id'],)
        ).fetchone()
        if not book or book['available_copies'] <= 0:
            conn.execute(
                'UPDATE loans SET status = ?, notes = ? WHERE id = ?',
                ('REJECTED', 'No copies available', loan_id)
            )
            conn.commit()
            return False

        # Step 3: Update loan status + set dates
        conn.execute('''
            UPDATE loans
            SET status = 'APPROVED',
                approved_by_id = ?,
                borrow_date = date('now'),
                due_date = date('now', '+14 days')
            WHERE id = ?
        ''', (admin_id, loan_id))

        # Step 4: Decrement available copies
        conn.execute(
            'UPDATE books SET available_copies = available_copies - 1 WHERE id = ?',
            (loan['book_id'],)
        )

        # Step 5: Commit both changes atomically
        conn.commit()
        return True
```

**Why this must be a transaction:** If step 3 succeeds but step 4 fails, the loan would be approved but the book's inventory wouldn't decrease — creating an inconsistency.

### Example 2: Batch Transaction (Bulk Approve)

**Requirement:** Approve multiple loans in a single atomic operation.

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
            conn.execute(
                'UPDATE books SET available_copies = available_copies - 1 WHERE id = ?',
                (loan['book_id'],)
            )
            approved += 1

        conn.commit()  # ONE commit for ALL approvals
        return approved
```

**Key Concept:** A single `conn.commit()` at the end ensures either all approvals succeed or none do.

### Example 3: Compensating Transaction (Return Book)

**Requirement:** Returning a book must reverse the inventory change made during approval.

```python
def return_loan(loan_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan or loan['status'] != 'APPROVED':
            return False

        # Update loan status AND restore inventory atomically
        conn.execute(
            'UPDATE loans SET status = ?, return_date = date("now") WHERE id = ?',
            ('RETURNED', loan_id)
        )
        conn.execute(
            'UPDATE books SET available_copies = available_copies + 1 WHERE id = ?',
            (loan['book_id'],)
        )
        conn.commit()
        return True
```

---

## Part 3: Stored Procedures (via Redis Lua Scripts)

### What is a Stored Procedure?

A stored procedure is a **precompiled set of statements** stored in the database and executed as a single unit. SQLite doesn't support traditional stored procedures, but **Redis** achieves the same concept using **Lua scripts** executed atomically on the server.

### Why Use Lua Scripts in Redis?

Redis is single-threaded, and Lua scripts execute atomically — **no other command can run** while a Lua script is executing. This makes them equivalent to stored procedures for:
- Eliminating race conditions
- Combining multiple operations into one round-trip
- Enforcing business logic server-side

### Example 1: Atomic Add to Cart (Check-Then-Insert)

**Problem:** Two concurrent requests could both add the same book to a cart.
**Solution:** A Lua script that checks and inserts in one atomic step.

```lua
-- KEYS[1] = "cart:{user_id}", ARGV[1] = book_id
local items = redis.call('LRANGE', KEYS[1], 0, -1)
for i, v in ipairs(items) do
    if v == ARGV[1] then return 0 end  -- Already in cart
end
redis.call('RPUSH', KEYS[1], ARGV[1])
return 1  -- Successfully added
```

**Called from Python:**
```python
result = redis_client.eval(LUA_ADD_TO_CART, 1, f"cart:{user_id}", book_id)
```

### Example 2: Atomic Checkout (Read-Then-Delete)

**Problem:** Between reading the cart count and deleting it, another process could modify the cart.
**Solution:** Count and delete in one atomic operation.

```lua
-- KEYS[1] = "cart:{user_id}"
local count = redis.call('LLEN', KEYS[1])
redis.call('DEL', KEYS[1])
return count
```

### Example 3: Distributed Lock (Compare-And-Swap)

**Problem:** Multiple servers processing the same request simultaneously.
**Solution:** A lock using `SET NX EX` with owner verification on release.

```lua
-- Acquire: only set if key doesn't exist, with expiration
local result = redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')
if result then return 1 else return 0 end
```

```lua
-- Release: only delete if we are the owner
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
return 0
```

**Key Concept:** The release script prevents a process from accidentally deleting another process's lock. This is a classic **Compare-And-Swap (CAS)** pattern.

---

## Part 4: Comparison Table

| Feature | SQLite Triggers | Python Transactions | Redis Lua Scripts |
|---------|----------------|--------------------|--------------------|
| **Execution** | Automatic (event-driven) | Manual (application code) | Manual (via `EVAL`) |
| **Atomicity** | Per-statement | Per `commit()` | Per script execution |
| **Where it runs** | Database engine | Application server | Redis server |
| **Use case** | Data validation, audit trails | Multi-table updates | Race condition prevention |
| **Rollback** | `RAISE(ABORT)` | Don't call `commit()` | Script returns error |
| **Equivalent to** | Database triggers | Transactions / SAVEPOINTs | Stored procedures |

---

## Part 5: Common Exam Questions

### Q1: "Write a trigger that prevents inserting a loan if the book has no available copies."

```sql
CREATE TRIGGER check_book_availability
BEFORE INSERT ON loans
FOR EACH ROW
WHEN NEW.status = 'APPROVED'
BEGIN
    SELECT CASE
        WHEN (SELECT available_copies FROM books WHERE id = NEW.book_id) <= 0
        THEN RAISE(ABORT, 'No copies available for this book')
    END;
END;
```

### Q2: "Write a transaction that transfers a loan from one user to another, ensuring both users exist."

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

### Q3: "Explain why the cart checkout needs to be atomic."

**Answer:** Without atomicity, two problems can occur:
1. **Lost update:** Between `LLEN` and `DEL`, another process could add an item. The count returned wouldn't match what was deleted.
2. **Double checkout:** Two concurrent checkout requests could both read the cart before either deletes it, causing duplicate loan requests.

The Lua script solves this by executing `LLEN` + `DEL` as a single indivisible operation on the Redis server.

### Q4: "What is the difference between BEFORE and AFTER triggers?"

| | BEFORE Trigger | AFTER Trigger |
|-|----------------|---------------|
| **When** | Before the operation executes | After the operation completes |
| **Can abort?** | Yes, using `RAISE(ABORT)` | No (change already applied) |
| **Use case** | Validation, default values | Audit logging, cascading updates |
| **Access** | `NEW.*` values (proposed) | `OLD.*` and `NEW.*` (actual) |

### Q5: "Write a trigger that automatically sets due_date to 14 days after borrow_date when a loan is approved."

```sql
CREATE TRIGGER auto_due_date
AFTER UPDATE ON loans
FOR EACH ROW
WHEN NEW.status = 'APPROVED' AND OLD.status != 'APPROVED'
BEGIN
    UPDATE loans SET due_date = date(NEW.borrow_date, '+14 days') WHERE id = NEW.id;
END;
```
