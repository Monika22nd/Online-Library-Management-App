package database;

import models.Book;
import models.Loan;

import javax.xml.crypto.Data;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LoanDAO {
    // Xử lý mượn hàng loạt (Transaction)
    public boolean addLoans(int userId, List<Book> books) {
        if (books == null || books.isEmpty()) return false;

        String insertSql = "INSERT INTO loans (user_id, book_id, borrow_date, due_date, status) VALUES (?, ?, ?, ?, 'BORROWED')";
        String decrementSql = "UPDATE books SET copies_available = copies_available - 1 WHERE id = ? AND copies_available > 0";

        Connection con = null;
        PreparedStatement insertPstmt = null;
        PreparedStatement decPstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Bắt đầu Transaction

            insertPstmt = con.prepareStatement(insertSql);
            long millis = System.currentTimeMillis();
            Date borrowDate = new Date(millis);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            cal.add(Calendar.DAY_OF_MONTH, 14); // Hạn 14 ngày
            Date dueDate = new Date(cal.getTimeInMillis());

            for (Book b : books) {
                insertPstmt.setInt(1, userId);
                insertPstmt.setInt(2, b.getId());
                insertPstmt.setDate(3, borrowDate);
                insertPstmt.setDate(4, dueDate);
                insertPstmt.addBatch();
            }
            int[] insertResults = insertPstmt.executeBatch();

            // Kiểm tra lỗi insert
            for (int r : insertResults) {
                if (r == Statement.EXECUTE_FAILED) {
                    con.rollback();
                    return false;
                }
            }

            // Trừ tồn kho
            decPstmt = con.prepareStatement(decrementSql);
            for (Book b : books) {
                decPstmt.setInt(1, b.getId());
                int updated = decPstmt.executeUpdate();
                if (updated == 0) {
                    con.rollback(); // Hết sách -> Hủy toàn bộ
                    return false;
                }
            }

            con.commit(); // Thành công -> Lưu
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException e) {}
        }
    }

    // Trả sách và cộng tồn kho
    public boolean returnBook(int loanId) {
        String selectSql = "SELECT book_id FROM loans WHERE id = ? AND status = 'BORROWED'";
        String updateLoanSql = "UPDATE loans SET return_date = ?, status = 'RETURNED' WHERE id = ?";
        String incrementSql = "UPDATE books SET copies_available = copies_available + 1 WHERE id = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // Lấy ID sách
            PreparedStatement selStmt = con.prepareStatement(selectSql);
            selStmt.setInt(1, loanId);
            ResultSet rs = selStmt.executeQuery();
            if (!rs.next()) { con.rollback(); return false; }
            int bookId = rs.getInt("book_id");

            // Cập nhật Loan
            PreparedStatement updStmt = con.prepareStatement(updateLoanSql);
            updStmt.setDate(1, new Date(System.currentTimeMillis()));
            updStmt.setInt(2, loanId);
            if (updStmt.executeUpdate() == 0) { con.rollback(); return false; }

            // Cộng sách
            PreparedStatement incStmt = con.prepareStatement(incrementSql);
            incStmt.setInt(1, bookId);
            if (incStmt.executeUpdate() == 0) { con.rollback(); return false; }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        }
    }

    public List<Integer> getAlreadyBorrowedBookIds(int userId, List<Book> books) {
        List<Integer> result = new ArrayList<>();
        if (books.isEmpty()) return result;
        StringBuilder params = new StringBuilder();
        for(int i=0; i<books.size(); i++) params.append(i==0 ? "?" : ",?");

        String sql = "SELECT book_id FROM loans WHERE user_id = ? AND status = 'BORROWED' AND book_id IN (" + params + ")";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int idx = 2;
            for(Book b : books) ps.setInt(idx++, b.getId());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) result.add(rs.getInt("book_id"));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public List<Loan> getBorrowedLoansByUser(int userId) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans WHERE user_id = ? AND status = 'BORROWED'";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                loans.add(new Loan(rs.getInt("id"), rs.getInt("user_id"), rs.getInt("book_id"),
                        rs.getDate("borrow_date"), rs.getDate("due_date"), rs.getDate("return_date"), rs.getString("status")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return loans;
    }

    public List<Loan> getLoanHistoryByUser( int userId){
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT l.*, b.title " +
                    "FROM loans l " +
                    "JOIN books b ON l.book_id = b.id " +
                    "WHERE l.user_id = ? " +
                    "ORDER BY l.borrow_date DESC";
        try (Connection con  = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                loans.add(new Loan(
                        rs.getInt("id"), rs.getInt("user_id"), null,
                        rs.getInt("book_id"), rs.getString("title"),
                        rs.getDate("borrow_date"), rs.getDate("due_date"),
                        rs.getDate("return_date"), rs.getString("status")
                ));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return loans;
    }

    public List<Loan> getAllLoans(){
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT l.*, b.title, u.username " +
                "FROM loans l " +
                "JOIN books b ON l.book_id = b.id " +
                "JOIN users u ON l.user_id = u.id " +
                "ORDER BY l.borrow_date DESC";
        try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()){
                loans.add(new Loan(
                        rs.getInt("id"), rs.getInt("user_id"), rs.getString("username"),
                        rs.getInt("book_id"), rs.getString("title"),
                        rs.getDate("borrow_date"), rs.getDate("due_date"),
                        rs.getDate("return_date"), rs.getString("status")
                ));
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return loans;
    }
}