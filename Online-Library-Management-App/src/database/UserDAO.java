package database;

import models.Role;
import models.User;
import java.sql.*;

public class UserDAO {
    public User login(String username, String password){
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ){
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()){
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        Role.valueOf(rs.getString("role"))
                );
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
    public boolean registerUser(User user) {
        String sql = "INSERT INTO users (name, username, password, email, phone, role) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getRole().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();

        }
        return false;
    }
}
