package Java.database;

import Java.models.Author;
import Java.models.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    // 1. Ánh xạ dữ liệu từ DB sang Object Book
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
                rs.getString("description"),
                rs.getString("image_path")   // Đọc cột ảnh
        );
    }

    // 2. Lấy danh sách tất cả sách
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT b.*, a.name AS author_name FROM books b LEFT JOIN authors a ON b.author_id = a.id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    // 3. Lấy sách theo ID
    public Book getBookById(int id) {
        String sql = "SELECT b.*, a.name AS author_name FROM books b LEFT JOIN authors a ON b.author_id = a.id WHERE b.id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToBook(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 4. Lấy tác giả theo ID
    public Author getAuthorById(int id) {
        String sql = "SELECT * FROM authors WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Author(rs.getInt("id"), rs.getString("name"), rs.getString("biography"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- LOGIC QUAN TRỌNG: Lấy ID tác giả từ Tên (Nếu chưa có thì tạo mới) ---
    private Integer getOrCreateAuthorId(Connection con, String authorName) throws SQLException {
        if (authorName == null || authorName.trim().isEmpty()) return null;

        String name = authorName.trim();
        // 1. Kiểm tra xem tác giả đã tồn tại chưa
        String checkSql = "SELECT id FROM authors WHERE name = ?";
        try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
            checkStmt.setString(1, name);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id"); // Trả về ID cũ
                }
            }
        }

        // 2. Nếu chưa có, tạo tác giả mới
        String insertSql = "INSERT INTO authors (name) VALUES (?)";
        try (PreparedStatement insertStmt = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, name);
            int rows = insertStmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet genKeys = insertStmt.getGeneratedKeys()) {
                    if (genKeys.next()) {
                        return genKeys.getInt(1); // Trả về ID mới tạo
                    }
                }
            }
        }
        return null;
    }

    // 5. Thêm sách (Xử lý author_id và image_path)
    public boolean addBook(Book book) {
        String sql = "INSERT INTO books (isbn, title, author_id, genre, copies_available, price, description, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            // Lấy ID tác giả (tự động tạo nếu chưa có)
            Integer authorId = getOrCreateAuthorId(con, book.getAuthorName());

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, book.getIsbn());
                pstmt.setString(2, book.getTitle());

                // Xử lý author_id (có thể null)
                if (authorId != null) pstmt.setInt(3, authorId);
                else pstmt.setNull(3, Types.INTEGER);

                pstmt.setString(4, book.getGenre());
                pstmt.setInt(5, book.getCopiesAvailable());
                pstmt.setDouble(6, book.getPrice());
                pstmt.setString(7, book.getDescription());
                pstmt.setString(8, book.getImagePath()); // Lưu đường dẫn ảnh

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 6. Cập nhật sách (Xử lý author_id và image_path)
    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET isbn=?, title=?, author_id=?, genre=?, price=?, copies_available=?, description=?, image_path=? WHERE id=?";

        try (Connection con = DatabaseConnection.getConnection()) {
            // Lấy ID tác giả từ tên mới (nếu người dùng sửa tên tác giả)
            Integer authorId = getOrCreateAuthorId(con, book.getAuthorName());

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, book.getIsbn());
                ps.setString(2, book.getTitle());

                if (authorId != null) ps.setInt(3, authorId);
                else ps.setNull(3, Types.INTEGER);

                ps.setString(4, book.getGenre());
                ps.setDouble(5, book.getPrice());
                ps.setInt(6, book.getCopiesAvailable());
                ps.setString(7, book.getDescription());
                ps.setString(8, book.getImagePath()); // Cập nhật ảnh
                ps.setInt(9, book.getId());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 7. Xóa sách
    public boolean deleteBook(int id) {
        String sql = "DELETE FROM books WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 8. Lấy tác giả kèm tiểu sử
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

    //9. Lấy danh sách tất cả thể loại để đổ vào ComboBox
    public List<String> getAllGenres() {
        List<String> genres = new ArrayList<>();
        String sql = "SELECT DISTINCT genre FROM books WHERE genre IS NOT NULL AND TRIM(genre) <> '' ORDER BY genre";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String g = rs.getString("genre");
                if (g != null && !g.trim().isEmpty()) genres.add(g);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return genres;
    }
}