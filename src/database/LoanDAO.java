package database;
import java.sql.*;

public class LoanDAO {
    public boolean addLoan(int userId, int bookId, Date dueDate) {
        String sql = "INSERT INTO loans (user_id, book_id, borrow_data, due_data, status) VALUES (?, ?, ?, ?, 'Borrowed')";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            Long millis = System.currentTimeMillis();
            Date currentDate = new Date(millis);

            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.setDate(3, currentDate); // Ngày mượn là hôm nay
            pstmt.setDate(4, dueDate);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
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
}