package database;

import models.Book;
import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

public class LoanDAO {
    // Add multiple loans in a batch. Returns true if all inserts succeed.
    public boolean addLoans(int userId, List<Book> books) {
        if (books == null || books.isEmpty()) return false;

        String sql = "INSERT INTO loans (user_id, book_id, borrow_date, due_date, status) VALUES (?, ?, ?, ?, 'BORROWED')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            long millis = System.currentTimeMillis();
            Date borrowDate = new Date(millis);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            cal.add(Calendar.DAY_OF_MONTH, 14); // due in 14 days
            Date dueDate = new Date(cal.getTimeInMillis());

            for (Book b : books) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, b.getId());
                pstmt.setDate(3, borrowDate);
                pstmt.setDate(4, dueDate);
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            for (int r : results) {
                if (r == Statement.EXECUTE_FAILED) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Update a loan as returned (keeps method but fixes column names)
    public boolean returnBook(int loanId) {
        String sql = "UPDATE loans SET return_date = ?, status = 'RETURNED' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            long millis = System.currentTimeMillis();
            Date returnDate = new Date(millis);

            pstmt.setDate(1, returnDate);
            pstmt.setInt(2, loanId);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
}