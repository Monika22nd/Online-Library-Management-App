package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/quanlythuvien2";
    private static final String USER = "Espi";
    private static final String PASSWORD = "1012005dung";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi: Không tìm thấy Driver MySQL!", e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi: Không thể kết nối đến Database!", e);
        }
        return conn;
    }
}