package Java.models;

public class User {
    private int id;
    private String name;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Role role;

    // Constructor đầy đủ (Dùng khi lấy từ Database ra)
    public User(int id, String name, String username, String password, String email, String phone, Role role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    // Constructor không có ID (Dùng khi Đăng ký mới - ID tự tăng trong DB)
    public User(String name, String username, String password, String email, String phone, Role role) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    // Getter methods
    public int getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }

    // Setter (nếu cần thiết cho việc cập nhật)
    public void setRole(Role role) { this.role = role; }

    @Override
    public String toString() {
        return name; // Để hiển thị đẹp trong ComboBox nếu cần
    }
}