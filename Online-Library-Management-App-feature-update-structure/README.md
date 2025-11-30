# Online Library Management App

Một ứng dụng quản lý thư viện đơn giản cho phép quản lý tài liệu, người đọc, giao dịch mượn/trả, phân quyền nhân viên và báo cáo thống kê.

## Mục lục
- Công nghệ sử dụng
- Tổng quan
- Cài đặt và chạy
- Cấu hình và biến môi trường

## Công nghệ sử dụng
- Ngôn ngữ: Java
- IDE: VS Code và JetBrains IntelliJ
- Giao diện: Sử dụng SceneBuilder với các thư viện JavaFX để tạo giao diện kéo thả
- Cơ sở dữ liệu: MySQL cùng với thư viện MySQL Connector để kết nối ứng dụng với cơ sở dữ liệu

## Tổng quan
Các chức năng chính:
- Đăng ký tài khoản
- Mượn sách, trả sách, xem thông tin sách.
- Xem lịch sử mượn.
- Xem dach sách những cuốn sách đã mượn.
- Đặt mượn trước để có thể đến thư viện và xác nhận.
- Tìm kiếm sách theo tên.
- Hiển thị sách theo thể loại.

Chức năng của người kiểm duyệt (admin):
- Gồm các chức năng gốc của người dùng.
- Quản lý danh sách người dùng: Thêm người dùng thủ công, sửa thông tin của người dùng và xóa người dùng.
- Quản lý sách trong thư viện: thêm sách, chỉnh sửa thông tin của sách (Tiêu đề, tác giả, số lượng, giá mượn) và xóa sách.
- Quản lý vé mượn: Hiển thị các thông tin của người dùng và sách mà người dùng đó mượn.

## Cài đặt và chạy ứng dụng
Trước khi tải ứng dụng, đảm bảo đã cài đặt [thư viện JavaFX của Gluon](https://download2.gluonhq.com/openjfx/17.0.17/openjfx-17.0.17_windows-x64_bin-sdk.zip) và thư viện [MySQL Connector](https://dev.mysql.com/downloads/file/?id=546178)

Cài đặt cơ sở dữ liệu bằng cách mở file schema.sql trong MySQL Workbench và chạy file.

### VS Code
Trong terminal của VS Code, tải ứng dụng bằng lệnh
```bash
git clone https://github.com/Monika22nd/Online-Library-Management-App.git
```
Sau đó, vào tab Explorer (Ctrl + Shift + E), tìm phần JAVA PROJECTS  
Trong mục Referenced Libraries, thêm các file .jar trong thư viện javafx và mysql-connector

Trong tab Run của thanh công cụ trên cùng, chọn Add Configuration và dán
```JSON
{"version":"0.2.0","configurations":[
    
    {"type":"java","name":"Launch Main","request":"launch","mainClass":"Main","vmArgs":["--module-path","C:\path\to\javafx-sdk-21\lib","--add-modules","javafx.controls,javafx.fxml"]}]}
```
Để chạy ứng dụng, vào file Main.java và chọn Run Java, hoặc vào tab Run and Debug (Ctrl + Shift + D) và nhấn Launch (F5)

---

### IntelliJ
Trong tab Project Structure (Ctrl + Alt + Shift + S), chọn mục Libraries trong Project Settings  
Thêm thư viện JavaFX bằng cách bấm dấu "+" hoặc nhấn Alt + Insert, chọn Java  
Chọn thư mục lib của thư viện JavaFX  
Trong tab Run, chọn Edit Configurations
- Chọn Main class của ứng dụng
- Chọn Modify options (Alt + M) -> Add VM options (Alt + V)
- Trong trường VM options, dán
```bash
--module-path "C:\path\to\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml
```
Vào file Main.java và chạy ứng dụng

## Cấu hình và biến môi trường
Trong folder Java.database, vào file DatabaseConnection.java  
Chỉnh các biến bên dưới
```Java
private static final String URL = "jdbc:mysql://127.0.0.1:3306/[database-name]";
private static final String USER = "[MySQL username]";
private static final String PASSWORD = "[MySQL password]";
```