package database;

import models.Book;
import models.Loan;
import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

public class LoanDAO {
    // Add multiple loans in a batch. Returns true if all inserts succeed and copies are decremented.
    public boolean addLoans(int userId, List<Book> books) {
        if (books == null || books.isEmpty()) return false;

        String insertSql = "INSERT INTO loans (user_id, book_id, borrow_date, due_date, status) VALUES (?, ?, ?, ?, 'BORROWED')";
        String decrementSql = "UPDATE books SET copies_available = copies_available - 1 WHERE id = ? AND copies_available > 0";

        Connection con = null;
        PreparedStatement insertPstmt = null;
        PreparedStatement decPstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // start transaction

            insertPstmt = con.prepareStatement(insertSql);
            long millis = System.currentTimeMillis();
            Date borrowDate = new Date(millis);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            cal.add(Calendar.DAY_OF_MONTH, 14); // due in 14 days
            Date dueDate = new Date(cal.getTimeInMillis());

            // prepare inserts
            for (Book b : books) {
                insertPstmt.setInt(1, userId);
                insertPstmt.setInt(2, b.getId());
                insertPstmt.setDate(3, borrowDate);
                insertPstmt.setDate(4, dueDate);
                insertPstmt.addBatch();
            }
            int[] insertResults = insertPstmt.executeBatch();

            // verify inserts didn't fail
            for (int r : insertResults) {
                if (r == Statement.EXECUTE_FAILED) {
                    con.rollback();
                    return false;
                }
            }

            // decrement copies for each book; if any decrement affects 0 rows -> no copies available
            decPstmt = con.prepareStatement(decrementSql);
            for (Book b : books) {
                decPstmt.setInt(1, b.getId());
                int updated = decPstmt.executeUpdate();
                if (updated == 0) {
                    // no available copies for this book -> rollback everything
                    con.rollback();
                    return false;
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            try { if (insertPstmt != null) insertPstmt.close(); } catch (SQLException ignored) {}
            try { if (decPstmt != null) decPstmt.close(); } catch (SQLException ignored) {}
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    // Update a loan as returned and increment book copies atomically
    public boolean returnBook(int loanId) {
        String selectSql = "SELECT book_id FROM loans WHERE id = ? AND status = 'BORROWED' FOR UPDATE";
        String updateLoanSql = "UPDATE loans SET return_date = ?, status = 'RETURNED' WHERE id = ?";
        String incrementSql = "UPDATE books SET copies_available = copies_available + 1 WHERE id = ?";

        Connection con = null;
        PreparedStatement selStmt = null;
        PreparedStatement updLoanStmt = null;
        PreparedStatement incStmt = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            selStmt = con.prepareStatement(selectSql);
            selStmt.setInt(1, loanId);
            rs = selStmt.executeQuery();
            if (!rs.next()) {
                // No borrowed loan found (either doesn't exist or already returned)
                con.rollback();
                return false;
            }
            int bookId = rs.getInt("book_id");

            long millis = System.currentTimeMillis();
            Date returnDate = new Date(millis);

            updLoanStmt = con.prepareStatement(updateLoanSql);
            updLoanStmt.setDate(1, returnDate);
            updLoanStmt.setInt(2, loanId);
            int loanUpdated = updLoanStmt.executeUpdate();
            if (loanUpdated == 0) {
                con.rollback();
                return false;
            }

            incStmt = con.prepareStatement(incrementSql);
            incStmt.setInt(1, bookId);
            int incUpdated = incStmt.executeUpdate();
            if (incUpdated == 0) {
                // Failed to increment book count (shouldn't normally happen) -> rollback
                con.rollback();
                return false;
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (selStmt != null) selStmt.close(); } catch (SQLException ignored) {}
            try { if (updLoanStmt != null) updLoanStmt.close(); } catch (SQLException ignored) {}
            try { if (incStmt != null) incStmt.close(); } catch (SQLException ignored) {}
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    // NEW: return list of book IDs that the user already has borrowed (status = 'BORROWED')
    public List<Integer> getAlreadyBorrowedBookIds(int userId, List<Book> books) {
        List<Integer> result = new ArrayList<>();
        if (books == null || books.isEmpty()) return result;

        // Build IN clause placeholders
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < books.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append("?");
        }

        String sql = "SELECT book_id FROM loans WHERE user_id = ? AND status = 'BORROWED' AND book_id IN (" + inClause + ")";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int idx = 2;
            for (Book b : books) {
                pstmt.setInt(idx++, b.getId());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("book_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // NEW: return list of Book objects that the user currently has borrowed (status = 'BORROWED')
    public List<Book> getBorrowedBooksByUser(int userId) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT b.*, a.name AS author_name " +
                     "FROM loans l " +
                     "JOIN books b ON l.book_id = b.id " +
                     "LEFT JOIN authors a ON b.author_id = a.id " +
                     "WHERE l.user_id = ? AND l.status = 'BORROWED'";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getInt("author_id"),
                        rs.getString("author_name"),
                        rs.getString("genre"),
                        rs.getDouble("price"),
                        rs.getInt("copies_available"),
                        rs.getString("description")
                    );
                    books.add(book);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    // NEW: return list of Loan objects for current BORROWED loans of a user
    public List<Loan> getBorrowedLoansByUser(int userId) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT id, user_id, book_id, borrow_date, due_date, return_date, status FROM loans WHERE user_id = ? AND status = 'BORROWED'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Loan ln = new Loan(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getDate("borrow_date"),
                        rs.getDate("due_date"),
                        rs.getDate("return_date"),
                        rs.getString("status")
                    );
                    loans.add(ln);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loans;
    }
}