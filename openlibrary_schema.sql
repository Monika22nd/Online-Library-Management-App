CREATE DATABASE IF NOT EXISTS library_openlibrary
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE library_openlibrary;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS fines;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS readers;
DROP TABLE IF EXISTS editions;
DROP TABLE IF EXISTS authors;
DROP TABLE IF EXISTS book_authors;
DROP TABLE IF EXISTS copies;
DROP TABLE IF EXISTS members;
DROP TABLE IF EXISTS sync_logs;
DROP TABLE IF EXISTS books;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE authors (
  author_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  openlibrary_author_key VARCHAR(32) NOT NULL,
  author_name VARCHAR(255) NOT NULL,
  slug VARCHAR(255) NULL,
  birth_date VARCHAR(64) NULL,
  bio TEXT NULL,
  alternate_names JSON NULL,
  top_subjects JSON NULL,
  work_count INT UNSIGNED NULL,
  author_raw JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (author_id)
) ENGINE=InnoDB;

CREATE TABLE books (
  book_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  openlibrary_work_key VARCHAR(32) NOT NULL,
  title VARCHAR(500) NOT NULL,
  subtitle VARCHAR(500) NULL,
  description TEXT NULL,
  first_publish_year SMALLINT UNSIGNED NULL,
  first_publish_date VARCHAR(64) NULL,
  subjects JSON NULL,
  places JSON NULL,
  series JSON NULL,
  links JSON NULL,
  excerpts JSON NULL,
  cover_ids JSON NULL,
  cover_edition_key VARCHAR(32) NULL,
  work_raw JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (book_id)
) ENGINE=InnoDB;

CREATE TABLE book_authors (
  book_id BIGINT UNSIGNED NOT NULL,
  author_id BIGINT UNSIGNED NOT NULL,
  author_order SMALLINT UNSIGNED NULL,
  role_name VARCHAR(128) NULL,
  PRIMARY KEY (book_id, author_id),
  CONSTRAINT fk_book_authors_book
    FOREIGN KEY (book_id) REFERENCES books (book_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_book_authors_author
    FOREIGN KEY (author_id) REFERENCES authors (author_id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE editions (
  edition_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  openlibrary_edition_key VARCHAR(32) NOT NULL,
  book_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(500) NOT NULL,
  subtitle VARCHAR(500) NULL,
  by_statement VARCHAR(500) NULL,
  publish_date VARCHAR(64) NULL,
  number_of_pages INT UNSIGNED NULL,
  languages JSON NULL,
  publishers JSON NULL,
  isbn_10 VARCHAR(20) NULL,
  isbn_13 VARCHAR(20) NULL,
  lccn VARCHAR(64) NULL,
  oclc VARCHAR(64) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (edition_id),
  CONSTRAINT fk_editions_book
    FOREIGN KEY (book_id) REFERENCES books (book_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE members (
  member_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  member_code VARCHAR(32) NOT NULL,
  member_name VARCHAR(255) NOT NULL,
  gender ENUM('male', 'female', 'other', 'unknown') NOT NULL DEFAULT 'unknown',
  dob DATE NULL,
  nationality VARCHAR(128) NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(255) NULL,
  address VARCHAR(500) NULL,
  registration_date DATE NOT NULL,
  status ENUM('active', 'inactive', 'blocked') NOT NULL DEFAULT 'active',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id)
) ENGINE=InnoDB;

CREATE TABLE copies (
  copy_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  edition_id BIGINT UNSIGNED NOT NULL,
  barcode VARCHAR(64) NOT NULL,
  call_number VARCHAR(128) NULL,
  acquisition_date DATE NULL,
  condition_status ENUM('new', 'good', 'fair', 'damaged', 'lost') NOT NULL DEFAULT 'good',
  circulation_status ENUM('available', 'on_loan', 'reserved', 'lost', 'repair') NOT NULL DEFAULT 'available',
  shelf_location VARCHAR(128) NULL,
  notes VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (copy_id),
  CONSTRAINT fk_copies_edition
    FOREIGN KEY (edition_id) REFERENCES editions (edition_id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE users (
  user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
  email VARCHAR(255) NULL,
  member_id BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_users_username (username),
  CONSTRAINT fk_users_member
    FOREIGN KEY (member_id) REFERENCES members (member_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE loans (
  loan_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  member_id BIGINT UNSIGNED NOT NULL,
  copy_id BIGINT UNSIGNED NULL,
  requested_book_id BIGINT UNSIGNED NULL,
  request_date DATE NOT NULL DEFAULT (CURRENT_DATE),
  borrow_date DATE NULL,
  due_date DATE NULL,
  return_date DATE NULL,
  renew_count TINYINT UNSIGNED NOT NULL DEFAULT 0,
  approval_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'RETURNED', 'CANCELLED')
    NOT NULL DEFAULT 'PENDING',
  loan_status ENUM('open', 'returned', 'overdue', 'lost') NOT NULL DEFAULT 'open',
  fine_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  approved_by_id BIGINT UNSIGNED NULL,
  notes VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (loan_id),
  KEY idx_loans_member (member_id),
  KEY idx_loans_copy (copy_id),
  KEY idx_loans_requested_book (requested_book_id),
  KEY idx_loans_approval_status (approval_status),
  CONSTRAINT fk_loans_member
    FOREIGN KEY (member_id) REFERENCES members (member_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_loans_copy
    FOREIGN KEY (copy_id) REFERENCES copies (copy_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_loans_requested_book
    FOREIGN KEY (requested_book_id) REFERENCES books (book_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_loans_approver
    FOREIGN KEY (approved_by_id) REFERENCES users (user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE audit_log (
  audit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  table_name VARCHAR(64) NOT NULL,
  record_id BIGINT UNSIGNED NULL,
  action VARCHAR(32) NOT NULL,
  old_value TEXT NULL,
  new_value TEXT NULL,
  changed_by_id BIGINT UNSIGNED NULL,
  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (audit_id),
  KEY idx_audit_table_record (table_name, record_id),
  KEY idx_audit_changed_at (changed_at),
  CONSTRAINT fk_audit_user
    FOREIGN KEY (changed_by_id) REFERENCES users (user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE reservations (
  reservation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  member_id BIGINT UNSIGNED NOT NULL,
  book_id BIGINT UNSIGNED NOT NULL,
  request_date DATE NOT NULL,
  expiry_date DATE NULL,
  reservation_status ENUM('pending', 'ready', 'fulfilled', 'cancelled', 'expired') NOT NULL DEFAULT 'pending',
  notes VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  CONSTRAINT fk_reservations_member
    FOREIGN KEY (member_id) REFERENCES members (member_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_reservations_book
    FOREIGN KEY (book_id) REFERENCES books (book_id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE fines (
  fine_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  loan_id BIGINT UNSIGNED NOT NULL,
  member_id BIGINT UNSIGNED NOT NULL,
  fine_type ENUM('late_return', 'lost_copy', 'damage', 'other') NOT NULL,
  fine_amount DECIMAL(12,2) NOT NULL,
  assessed_date DATE NOT NULL,
  paid_status ENUM('unpaid', 'partial', 'paid', 'waived') NOT NULL DEFAULT 'unpaid',
  note VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (fine_id),
  CONSTRAINT fk_fines_loan
    FOREIGN KEY (loan_id) REFERENCES loans (loan_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_fines_member
    FOREIGN KEY (member_id) REFERENCES members (member_id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE payments (
  payment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  fine_id BIGINT UNSIGNED NOT NULL,
  payment_date DATE NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  payment_method ENUM('cash', 'card', 'bank_transfer', 'e_wallet', 'other') NOT NULL DEFAULT 'cash',
  transaction_ref VARCHAR(128) NULL,
  note VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (payment_id),
  CONSTRAINT fk_payments_fine
    FOREIGN KEY (fine_id) REFERENCES fines (fine_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE sync_logs (
  sync_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_name VARCHAR(64) NOT NULL DEFAULT 'openlibrary',
  endpoint_name VARCHAR(128) NOT NULL,
  source_key VARCHAR(128) NULL,
  request_url VARCHAR(1000) NOT NULL,
  response_status SMALLINT UNSIGNED NULL,
  rows_upserted INT UNSIGNED NOT NULL DEFAULT 0,
  sync_status ENUM('running', 'success', 'failed', 'partial') NOT NULL DEFAULT 'running',
  error_message TEXT NULL,
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  PRIMARY KEY (sync_log_id)
) ENGINE=InnoDB;
