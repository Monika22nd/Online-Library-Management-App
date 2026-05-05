import sqlite3
from contextlib import contextmanager
from datetime import date, datetime
from typing import Optional, List, Dict, Any
import json
import os

DATABASE_PATH = os.environ.get("DATABASE_PATH", "library.db")

@contextmanager
def get_db():
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
    finally:
        conn.close()

def init_db():
    """Initialize the SQLite database with required tables and triggers."""
    with get_db() as conn:
        conn.executescript('''
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
                author TEXT NOT NULL,
                isbn TEXT,
                cover_url TEXT,
                description TEXT,
                publish_year INTEGER,
                subjects TEXT,
                available_copies INTEGER NOT NULL DEFAULT 0,
                total_copies INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                book_id INTEGER NOT NULL,
                request_date DATE NOT NULL DEFAULT (date('now')),
                borrow_date DATE,
                due_date DATE,
                return_date DATE,
                status TEXT NOT NULL DEFAULT 'PENDING',
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
                changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE INDEX IF NOT EXISTS idx_loans_user_id ON loans(user_id);
            CREATE INDEX IF NOT EXISTS idx_loans_book_id ON loans(book_id);
            CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);
            CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
        ''')

        trigger_sql = '''
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

            CREATE TRIGGER IF NOT EXISTS auto_request_date
            BEFORE INSERT ON loans
            FOR EACH ROW
            WHEN NEW.request_date IS NULL
            BEGIN
                UPDATE loans SET request_date = date('now') WHERE id = NEW.id;
            END;

            CREATE TRIGGER IF NOT EXISTS loan_status_audit
            AFTER UPDATE ON loans
            FOR EACH ROW
            WHEN OLD.status != NEW.status
            BEGIN
                INSERT INTO audit_log (table_name, record_id, action, old_value, new_value, changed_by_id)
                VALUES ('loans', NEW.id, 'STATUS_CHANGE', OLD.status, NEW.status, NEW.approved_by_id);
            END;
        '''
        conn.executescript(trigger_sql)

def row_to_dict(row) -> dict:
    if row is None:
        return None
    return dict(row)

def get_user_by_username(username: str) -> Optional[Dict]:
    with get_db() as conn:
        row = conn.execute('SELECT * FROM users WHERE username = ?', (username,)).fetchone()
        return row_to_dict(row)

def get_user_by_id(user_id: int) -> Optional[Dict]:
    with get_db() as conn:
        row = conn.execute('SELECT id, username, role, email FROM users WHERE id = ?', (user_id,)).fetchone()
        return row_to_dict(row)

def create_user(username: str, password: str, email: str, role: str = 'USER') -> int:
    with get_db() as conn:
        cursor = conn.execute(
            'INSERT INTO users (username, password, role, email) VALUES (?, ?, ?, ?)',
            (username, password, role, email)
        )
        conn.commit()
        return cursor.lastrowid

def verify_password(username: str, password: str) -> Optional[Dict]:
    user = get_user_by_username(username)
    if user and user['password'] == password:
        return user
    return None

def search_books_local(query: str, limit: int = 50, offset: int = 0) -> List[Dict]:
    with get_db() as conn:
        rows = conn.execute(
            'SELECT * FROM books WHERE title LIKE ? OR author LIKE ? ORDER BY id LIMIT ? OFFSET ?',
            (f'%{query}%', f'%{query}%', limit, offset)
        ).fetchall()
        return [row_to_dict(r) for r in rows]

def get_books_count() -> int:
    with get_db() as conn:
        row = conn.execute('SELECT COUNT(*) as count FROM books').fetchone()
        return row['count'] if row else 0

def get_all_books(limit: int = 50, offset: int = 0) -> List[Dict]:
    with get_db() as conn:
        rows = conn.execute(
            'SELECT * FROM books ORDER BY id LIMIT ? OFFSET ?',
            (limit, offset)
        ).fetchall()
        return [row_to_dict(r) for r in rows]

def get_book_by_id(book_id: int) -> Optional[Dict]:
    with get_db() as conn:
        row = conn.execute('SELECT * FROM books WHERE id = ?', (book_id,)).fetchone()
        return row_to_dict(row)

def get_book_by_openlibrary_key(key: str) -> Optional[Dict]:
    with get_db() as conn:
        row = conn.execute('SELECT * FROM books WHERE openlibrary_key = ?', (key,)).fetchone()
        return row_to_dict(row)

def create_book(
    openlibrary_key: str,
    title: str,
    author: str,
    isbn: Optional[str] = None,
    cover_url: Optional[str] = None,
    description: Optional[str] = None,
    publish_year: Optional[int] = None,
    subjects: Optional[str] = None,
    total_copies: int = 1
) -> int:
    with get_db() as conn:
        cursor = conn.execute('''
            INSERT INTO books (openlibrary_key, title, author, isbn, cover_url, description, publish_year, subjects, total_copies, available_copies)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (openlibrary_key, title, author, isbn, cover_url, description, publish_year, subjects, total_copies, total_copies))
        conn.commit()
        return cursor.lastrowid

def update_book_copies(book_id: int, delta: int):
    with get_db() as conn:
        conn.execute(
            'UPDATE books SET available_copies = available_copies + ? WHERE id = ?',
            (delta, book_id)
        )
        conn.commit()

def get_user_loans(user_id: int, status: Optional[str] = None) -> List[Dict]:
    with get_db() as conn:
        if status:
            rows = conn.execute('''
                SELECT l.*, b.title, b.author, b.cover_url
                FROM loans l
                JOIN books b ON l.book_id = b.id
                WHERE l.user_id = ? AND l.status = ?
                ORDER BY l.request_date DESC
            ''', (user_id, status)).fetchall()
        else:
            rows = conn.execute('''
                SELECT l.*, b.title, b.author, b.cover_url
                FROM loans l
                JOIN books b ON l.book_id = b.id
                WHERE l.user_id = ?
                ORDER BY l.request_date DESC
            ''', (user_id,)).fetchall()
        return [row_to_dict(r) for r in rows]

def get_all_loans(status: Optional[str] = None) -> List[Dict]:
    with get_db() as conn:
        if status:
            rows = conn.execute('''
                SELECT l.*, u.username, b.title, b.author
                FROM loans l
                JOIN users u ON l.user_id = u.id
                JOIN books b ON l.book_id = b.id
                WHERE l.status = ?
                ORDER BY l.request_date DESC
            ''', (status,)).fetchall()
        else:
            rows = conn.execute('''
                SELECT l.*, u.username, b.title, b.author
                FROM loans l
                JOIN users u ON l.user_id = u.id
                JOIN books b ON l.book_id = b.id
                ORDER BY l.request_date DESC
            ''').fetchall()
        return [row_to_dict(r) for r in rows]

def create_loan(user_id: int, book_id: int) -> int:
    with get_db() as conn:
        cursor = conn.execute('''
            INSERT INTO loans (user_id, book_id, status) VALUES (?, ?, 'PENDING')
        ''', (user_id, book_id))
        conn.commit()
        return cursor.lastrowid

def approve_loan(loan_id: int, admin_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan or loan['status'] != 'PENDING':
            return False
        
        book = conn.execute('SELECT * FROM books WHERE id = ?', (loan['book_id'],)).fetchone()
        if not book or book['available_copies'] <= 0:
            conn.execute(
                'UPDATE loans SET status = ?, notes = ? WHERE id = ?',
                ('REJECTED', 'No copies available', loan_id)
            )
            conn.commit()
            return False
        
        conn.execute('''
            UPDATE loans SET status = 'APPROVED', approved_by_id = ?, borrow_date = date('now'), due_date = date('now', '+14 days')
            WHERE id = ?
        ''', (admin_id, loan_id))
        
        conn.execute(
            'UPDATE books SET available_copies = available_copies - 1 WHERE id = ?',
            (loan['book_id'],)
        )
        conn.commit()
        return True

def return_loan(loan_id: int) -> bool:
    with get_db() as conn:
        loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
        if not loan or loan['status'] != 'APPROVED':
            return False
        
        conn.execute('''
            UPDATE loans SET status = 'RETURNED', return_date = date('now') WHERE id = ?
        ''', (loan_id,))
        
        conn.execute(
            'UPDATE books SET available_copies = available_copies + 1 WHERE id = ?',
            (loan['book_id'],)
        )
        conn.commit()
        return True

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

def bulk_approve_loans(loan_ids: List[int], admin_id: int) -> int:
    approved = 0
    with get_db() as conn:
        for loan_id in loan_ids:
            loan = conn.execute('SELECT * FROM loans WHERE id = ?', (loan_id,)).fetchone()
            if not loan or loan['status'] != 'PENDING':
                continue
            
            book = conn.execute('SELECT * FROM books WHERE id = ?', (loan['book_id'],)).fetchone()
            if not book or book['available_copies'] <= 0:
                conn.execute('UPDATE loans SET status = ?, notes = ? WHERE id = ?', ('REJECTED', 'No copies available', loan_id))
                continue
            
            conn.execute('''
                UPDATE loans SET status = 'APPROVED', approved_by_id = ?, borrow_date = date('now'), due_date = date('now', '+14 days')
                WHERE id = ?
            ''', (admin_id, loan_id))
            conn.execute('UPDATE books SET available_copies = available_copies - 1 WHERE id = ?', (loan['book_id'],))
            approved += 1
        
        conn.commit()
        return approved

def get_audit_log(limit: int = 50) -> List[Dict]:
    with get_db() as conn:
        rows = conn.execute('SELECT * FROM audit_log ORDER BY changed_at DESC LIMIT ?', (limit,)).fetchall()
        return [row_to_dict(r) for r in rows]

def seed_sample_data():
    users = [
        ('admin', 'admin123', 'ADMIN', 'admin@library.com'),
        ('john', 'user123', 'USER', 'john@example.com'),
        ('jane', 'user123', 'USER', 'jane@example.com'),
    ]
    
    with get_db() as conn:
        for username, password, role, email in users:
            existing = conn.execute('SELECT id FROM users WHERE username = ?', (username,)).fetchone()
            if not existing:
                conn.execute(
                    'INSERT INTO users (username, password, role, email) VALUES (?, ?, ?, ?)',
                    (username, password, role, email)
                )
        conn.commit()