package database;

import models.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt("id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getInt("author_id"),
                rs.getString("author_name"), // Cột này có được nhờ JOIN
                rs.getString("genre"),
                rs.getDouble("price"),
                rs.getInt("copies_available"),
                rs.getString("description")
        );
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT b.*, a.name AS author_name " +
                "FROM books b LEFT JOIN authors a ON b.author_id = a.id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public List<Book> searchBooks(String keyword){
        List<Book> books = new ArrayList<>();
        String sql = "SELECT b.*, a.name AS author_name " +
                "FROM books b LEFT JOIN authors a ON b.author_id = a.id " +
                "WHERE b.title LIKE ? OR a.name LIKE ?"; // Tìm theo tên sách hoặc tên tác giả

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()){
                books.add(mapResultSetToBook(rs));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return books;
    }

    public boolean addBook (Book book){
        String sql = "INSERT INTO books (isbn, title, author, genre, copies_available, price) VALUES(?, ?, ?, ?, ?, ?)";

        try(Connection con = DatabaseConnection.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthorName());
            pstmt.setString(4, book.getGenre());
            pstmt.setInt(5, book.getCopiesAvailable());
            pstmt.setDouble(6, book.getPrice());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }
}
