package database;

import models.Book;
import models.Author;
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

    // Replace previous addBook implementation so it resolves author by name (create if missing)
    public boolean addBook (Book book){
        String findAuthorSql = "SELECT id FROM authors WHERE name = ? LIMIT 1";
        String insertAuthorSql = "INSERT INTO authors (name) VALUES (?)";
        String insertBookSql = "INSERT INTO books (isbn, title, author_id, genre, copies_available, price, description) VALUES(?, ?, ?, ?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement findAuthorStmt = null;
        PreparedStatement insertAuthorStmt = null;
        PreparedStatement insertBookStmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();

            Integer authorId = null;
            String authorName = book.getAuthorName();
            if (authorName != null && !authorName.trim().isEmpty()) {
                findAuthorStmt = con.prepareStatement(findAuthorSql);
                findAuthorStmt.setString(1, authorName.trim());
                rs = findAuthorStmt.executeQuery();
                if (rs.next()) {
                    authorId = rs.getInt("id");
                } else {
                    // create new author
                    insertAuthorStmt = con.prepareStatement(insertAuthorSql, Statement.RETURN_GENERATED_KEYS);
                    insertAuthorStmt.setString(1, authorName.trim());
                    int aRows = insertAuthorStmt.executeUpdate();
                    if (aRows > 0) {
                        try (ResultSet gen = insertAuthorStmt.getGeneratedKeys()) {
                            if (gen.next()) {
                                authorId = gen.getInt(1);
                            }
                        }
                    }
                }
                try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
                try { if (findAuthorStmt != null) findAuthorStmt.close(); } catch (SQLException ignored) {}
                try { if (insertAuthorStmt != null) insertAuthorStmt.close(); } catch (SQLException ignored) {}
            } else {
                // no author provided; leave authorId null
                authorId = null;
            }

            insertBookStmt = con.prepareStatement(insertBookSql);
            insertBookStmt.setString(1, book.getIsbn());
            insertBookStmt.setString(2, book.getTitle());
            if (authorId != null) insertBookStmt.setInt(3, authorId);
            else insertBookStmt.setNull(3, java.sql.Types.INTEGER);
            insertBookStmt.setString(4, book.getGenre());
            insertBookStmt.setInt(5, book.getCopiesAvailable());
            insertBookStmt.setDouble(6, book.getPrice());
            insertBookStmt.setString(7, book.getDescription());

            int rowsAffected = insertBookStmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (findAuthorStmt != null) findAuthorStmt.close(); } catch (SQLException ignored) {}
            try { if (insertAuthorStmt != null) insertAuthorStmt.close(); } catch (SQLException ignored) {}
            try { if (insertBookStmt != null) insertBookStmt.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
        return false;
    }

    // NEW: update an existing book
    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET isbn = ?, title = ?, author_id = ?, genre = ?, price = ?, copies_available = ?, description = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setObject(3, book.getAuthorId(), java.sql.Types.INTEGER); // allow null
            pstmt.setString(4, book.getGenre());
            pstmt.setDouble(5, book.getPrice());
            pstmt.setInt(6, book.getCopiesAvailable());
            pstmt.setString(7, book.getDescription());
            pstmt.setInt(8, book.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // NEW: delete a book by id
    public boolean deleteBook(int bookId) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // NEW: get single book by id
    public Book getBookById(int id) {
        String sql = "SELECT b.*, a.name AS author_name FROM books b LEFT JOIN authors a ON b.author_id = a.id WHERE b.id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBook(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // NEW: return Author object by id (biography included)
	public Author getAuthorById(Integer authorId) {
	    if (authorId == null) return null;
	    String sql = "SELECT id, name, biography FROM authors WHERE id = ?";
	    try (Connection con = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = con.prepareStatement(sql)) {
	        pstmt.setInt(1, authorId);
	        try (ResultSet rs = pstmt.executeQuery()) {
	            if (rs.next()) {
	                return new Author(
	                    rs.getInt("id"),
	                    rs.getString("name"),
	                    rs.getString("biography")
	                );
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	// Optional thin wrapper kept for compatibility
	public String getAuthorBiographyById(Integer authorId) {
	    Author a = getAuthorById(authorId);
	    return a != null ? a.getBiography() : null;
	}
}
