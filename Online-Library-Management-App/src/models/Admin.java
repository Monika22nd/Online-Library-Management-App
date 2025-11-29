package models;

import database.BookDAO;
import database.UserDAO;
import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class Admin {
    
    private static final String username = "admin";
    private static final String password = "1";
    private static final String email = "admin@gmail.com";
    private static final String phone = "0987654321";

    private final UserDAO userDAO = new UserDAO();
    private final BookDAO bookDAO = new BookDAO();

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // Methods
    //Create user
    public void createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        boolean created = userDAO.registerUser(user);
        if (!created) {
            throw new IllegalStateException("Failed to create user (maybe already exists or DB error)");
        }
    }

    // Delete user by id
     
    public void deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                throw new IllegalStateException("No user found with id: " + userId);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error deleting user: " + e.getMessage(), e);
        }
    }

    
    //Update user
    
    public void updateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getId() == 0) {
            throw new IllegalArgumentException("User id must be set to update");
        }
        String sql = "UPDATE users SET name = ?, username = ?, password = ?, email = ?, phone = ?, role = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getRole().toString());
            pstmt.setInt(7, user.getId());

            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                throw new IllegalStateException("No user found with id: " + user.getId());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error updating user: " + e.getMessage(), e);
        }
    }

     //Reset password by id.
     
    public String resetPassword(int userId) {
    	
        String newPassword = password; 
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
        		
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            int rows = pstmt.executeUpdate();
            
            if (rows == 0) {
                throw new IllegalStateException("No user found with id: " + userId);
            }
            return newPassword;
            
        } catch (SQLException e) {
            throw new IllegalStateException("Error resetting password: " + e.getMessage(), e);
        }
    }


    // Book management


    // add book using BookDAO.addBook.

    public void addBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        boolean added = bookDAO.addBook(book);
        if (!added) {
            throw new IllegalStateException("Failed to add book (maybe duplicate or DB error)");
        }
    }

    
     //Remove book id
     
    public void removeBook(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Book id must be positive");
        }
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                throw new IllegalStateException("No book found with id: " + id);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error removing book: " + e.getMessage(), e);
        }
    }


    
     //Update book by id; if id == 0 try ISBN.
     
    public void updateBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }

        // If id is present, update by id
        if (book.getId() != 0) {
            String sql = "UPDATE books SET isbn = ?, title = ?, author_id = ?, genre = ?, price = ?, copies_available = ?, description = ? WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, book.getIsbn());
                pstmt.setString(2, book.getTitle());
                pstmt.setInt(3, book.getAuthorId());
                pstmt.setString(4, book.getGenre());
                pstmt.setDouble(5, book.getPrice());
                pstmt.setInt(6, book.getCopiesAvailable());
                pstmt.setString(7, book.getDescription());
                pstmt.setInt(8, book.getId());

                int rows = pstmt.executeUpdate();
                if (rows == 0) {
                    throw new IllegalStateException("No book found with id: " + book.getId());
                }
                return;
            } catch (SQLException e) {
                throw new IllegalStateException("Error updating book: " + e.getMessage(), e);
            }
        }

        // Otherwise try update by ISBN
        if (book.getIsbn() != null) {
            String sql = "UPDATE books SET title = ?, author_id = ?, genre = ?, price = ?, copies_available = ?, description = ? WHERE isbn = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, book.getTitle());
                pstmt.setInt(2, book.getAuthorId());
                pstmt.setString(3, book.getGenre());
                pstmt.setDouble(4, book.getPrice());
                pstmt.setInt(5, book.getCopiesAvailable());
                pstmt.setString(6, book.getDescription());
                pstmt.setString(7, book.getIsbn());

                int rows = pstmt.executeUpdate();
                if (rows == 0) {
                    throw new IllegalStateException("No book found with ISBN: " + book.getIsbn());
                }
                return;
            } catch (SQLException e) {
                throw new IllegalStateException("Error updating book: " + e.getMessage(), e);
            }
        }

        throw new IllegalArgumentException("Book must have id or ISBN to update");
    }


    // Listing all books/users

    public List<User> listUsers() {

        List<User> result = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
        		
             PreparedStatement pstmt = conn.prepareStatement(sql);
        		
             java.sql.ResultSet rs = pstmt.executeQuery()) {
        	
            while (rs.next()) {
                result.add(new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error listing users: " + e.getMessage(), e);
        }
        return result;
    }

    public List<Book> listBooks() {
    	
        return bookDAO.getAllBooks();
    }


    // Admin login check

    public boolean checkAdminLogin(String password) {
        if (password == null) {
            return false;
        }
        return Admin.password.equals(password);
    }
}
