package Java.database;

import Java.models.Book;
import Java.models.Loan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    // 1. GỬI YÊU CẦU MƯỢN (User dùng) - Status = PENDING, KHÔNG trừ kho
    public boolean requestLoans(int userId, List<Book> books) {
        if (books == null || books.isEmpty()) return false;
        String sql = "INSERT INTO loans (user_id, book_id, borrow_date, due_date, status) VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            Date now = new Date(System.currentTimeMillis());
            // Giả sử hạn trả là 14 ngày sau khi duyệt (tạm thời lưu ngày hiện tại + 14)
            Date dueDate = new Date(System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000));

            for (Book b : books) {
                ps.setInt(1, userId);
                ps.setInt(2, b.getId());
                ps.setDate(3, now);
                ps.setDate(4, dueDate);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            return results.length > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. ADMIN DUYỆT MƯỢN (Approve) - Trừ kho, Status = BORROWED
    public String approveLoan(int loanId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // a. Lấy book_id và kiểm tra tồn kho
            String checkSql = "SELECT l.book_id, b.copies_available FROM loans l JOIN books b ON l.book_id = b.id WHERE l.id = ?";
            int bookId = -1;
            int currentCopies = 0;
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, loanId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    bookId = rs.getInt("book_id");
                    currentCopies = rs.getInt("copies_available");
                } else {
                    con.rollback(); return "Không tìm thấy phiếu mượn.";
                }
            }

            if (currentCopies <= 0) {
                con.rollback(); return "Sách này đã hết hàng, không thể duyệt.";
            }

            // b. Trừ tồn kho
            String decSql = "UPDATE books SET copies_available = copies_available - 1 WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(decSql)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            // c. Cập nhật status
            String upSql = "UPDATE loans SET status = 'BORROWED' WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(upSql)) {
                ps.setInt(1, loanId);
                ps.executeUpdate();
            }

            con.commit();
            return "SUCCESS";
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return "Lỗi Database: " + e.getMessage();
        } finally {
            try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException e) {}
        }
    }

    // 3. ADMIN TỪ CHỐI (Reject) - Status = REJECTED
    public boolean rejectLoan(int loanId) {
        String sql = "UPDATE loans SET status = 'REJECTED' WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // 4. ADMIN XÁC NHẬN TRẢ (Return) - Cộng kho, Status = RETURNED
    public boolean adminConfirmReturn(int loanId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // a. Lấy book_id
            String getBookSql = "SELECT book_id FROM loans WHERE id = ?";
            int bookId = -1;
            try (PreparedStatement ps = con.prepareStatement(getBookSql)) {
                ps.setInt(1, loanId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) bookId = rs.getInt("book_id");
                else { con.rollback(); return false; }
            }

            // b. Update Loan -> RETURNED, set return_date
            String upLoan = "UPDATE loans SET status = 'RETURNED', return_date = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(upLoan)) {
                ps.setDate(1, new Date(System.currentTimeMillis()));
                ps.setInt(2, loanId);
                ps.executeUpdate();
            }

            // c. Cộng tồn kho
            String upBook = "UPDATE books SET copies_available = copies_available + 1 WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(upBook)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException e) {}
        }
    }

    // 5. User xem sách đang mượn (Gồm cả PENDING và BORROWED)
    public List<Loan> getActiveLoansByUser(int userId) {
        List<Loan> loans = new ArrayList<>();
        // Lấy cả PENDING và BORROWED
        String sql = "SELECT l.*, b.title FROM loans l JOIN books b ON l.book_id = b.id WHERE l.user_id = ? AND l.status IN ('PENDING', 'BORROWED')";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                loans.add(new Loan(
                        rs.getInt("id"), rs.getInt("user_id"), null,
                        rs.getInt("book_id"), rs.getString("title"),
                        rs.getDate("borrow_date"), rs.getDate("due_date"),
                        rs.getDate("return_date"), rs.getString("status")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return loans;
    }

    // 6. Lịch sử (cho User xem)
    public List<Loan> getLoanHistoryByUser(int userId) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT l.*, b.title FROM loans l JOIN books b ON l.book_id = b.id WHERE l.user_id = ? ORDER BY l.borrow_date DESC";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                loans.add(new Loan(
                        rs.getInt("id"), rs.getInt("user_id"), null,
                        rs.getInt("book_id"), rs.getString("title"),
                        rs.getDate("borrow_date"), rs.getDate("due_date"),
                        rs.getDate("return_date"), rs.getString("status")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return loans;
    }

    // 7. Admin xem tất cả (để quản lý)
    public List<Loan> getAllLoans() {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT l.*, b.title, u.username FROM loans l JOIN books b ON l.book_id = b.id JOIN users u ON l.user_id = u.id ORDER BY l.id DESC";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                loans.add(new Loan(
                        rs.getInt("id"), rs.getInt("user_id"), rs.getString("username"),
                        rs.getInt("book_id"), rs.getString("title"),
                        rs.getDate("borrow_date"), rs.getDate("due_date"),
                        rs.getDate("return_date"), rs.getString("status")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return loans;
    }

    // Check trùng (chỉ check PENDING hoặc BORROWED)
    public List<Integer> getAlreadyBorrowedBookIds(int userId, List<Book> books) {
        List<Integer> result = new ArrayList<>();
        if (books.isEmpty()) return result;
        StringBuilder params = new StringBuilder();
        for(int i=0; i<books.size(); i++) params.append(i==0 ? "?" : ",?");

        String sql = "SELECT book_id FROM loans WHERE user_id = ? AND status IN ('PENDING', 'BORROWED') AND book_id IN (" + params + ")";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int idx = 2;
            for(Book b : books) ps.setInt(idx++, b.getId());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) result.add(rs.getInt("book_id"));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
}