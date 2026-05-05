# Cấu trúc và Chức năng Cơ sở dữ liệu: Triggers, Transactions, và Procedures

Tài liệu này mô tả chi tiết cách hệ thống quản lý thư viện trực tuyến xử lý toàn vẹn dữ liệu, đồng bộ hóa và logic nghiệp vụ thông qua Triggers (SQLite), Transactions (SQLite & Python), và Procedures (Python & Redis).

## 1. Triggers (SQLite)
Hệ thống sử dụng các SQLite Triggers tự động thực thi các hành động khi có sự kiện `INSERT` hoặc `UPDATE` trên bảng `loans` (mượn sách). Các triggers này giúp đảm bảo tính toàn vẹn dữ liệu ở cấp độ cơ sở dữ liệu mà không cần phụ thuộc hoàn toàn vào code ứng dụng.

*   **`check_max_loans` (BEFORE INSERT trên `loans`)**:
    *   **Chức năng**: Kiểm tra xem người dùng đã đạt đến giới hạn số lượng sách được phép mượn tối đa hay chưa (giới hạn là 5 cuốn).
    *   **Logic**: Nếu số lượng sách có trạng thái `PENDING` hoặc `APPROVED` của user_id hiện tại >= 5, trigger sẽ hủy bỏ thao tác `INSERT` với thông báo lỗi `'User has reached maximum active loans (5)'`.
*   **`auto_request_date` (BEFORE INSERT trên `loans`)**:
    *   **Chức năng**: Tự động điền ngày yêu cầu mượn sách.
    *   **Logic**: Khi một bản ghi `loans` mới được chèn nhưng cột `request_date` là `NULL`, trigger tự động cập nhật `request_date` thành ngày hiện tại (`date('now')`).
*   **`loan_status_audit` (AFTER UPDATE trên `loans`)**:
    *   **Chức năng**: Tự động ghi log mỗi khi trạng thái của một yêu cầu mượn sách thay đổi.
    *   **Logic**: Nếu `OLD.status` khác với `NEW.status` (ví dụ: từ `PENDING` sang `APPROVED`), trigger sẽ tự động chèn một bản ghi vào bảng `audit_log` ghi lại hành động (`STATUS_CHANGE`), giá trị cũ, giá trị mới và ID của người thực hiện thay đổi.

## 2. Transactions (SQLite & Python)
Vì SQLite không hỗ trợ cú pháp `START TRANSACTION` / `COMMIT` theo kiểu script độc lập trong Python driver một cách tự động cho nhiều bảng, ứng dụng quản lý Transactions ở tầng Python thông qua `conn.commit()` của thư viện `sqlite3`. 

Các giao dịch này đảm bảo tính chất **ACID** (Atomicity - tính nguyên tử), đặc biệt khi một thao tác nghiệp vụ yêu cầu cập nhật nhiều bảng cùng lúc.

*   **Chức năng Mượn/Phê duyệt sách (`approve_loan`, `bulk_approve_loans`)**:
    *   **Logic**: Cập nhật trạng thái của bản ghi trong bảng `loans` thành `APPROVED` **VÀ** giảm số lượng sách có sẵn (`available_copies`) trong bảng `books` đi 1. Cả hai thao tác này được thực thi, sau đó `conn.commit()` được gọi. Nếu một trong hai thất bại, không có thay đổi nào được lưu lại.
*   **Chức năng Trả sách (`return_loan`) / Hủy yêu cầu (`cancel_loan`)**:
    *   **Logic**: Cập nhật trạng thái trong bảng `loans` thành `RETURNED` hoặc `CANCELLED` **VÀ** tăng số lượng sách có sẵn trong bảng `books` lên 1. Giao dịch đảm bảo số lượng sách trong kho luôn đồng bộ với trạng thái mượn/trả.
*   **Quản lý người dùng (`create_user`) và sách (`create_book`)**:
    *   Sử dụng `conn.commit()` để xác nhận chèn dữ liệu mới vào cơ sở dữ liệu.

## 3. Procedures (Stored Procedures tương đương)
### A. Tầng Python (thay thế cho Stored Procedures của DB)
SQLite không có khái niệm Stored Procedures. Thay vào đó, dự án đóng gói logic nghiệp vụ phức tạp vào các hàm Python trong file `app/models/database.py`. Các hàm này đóng vai trò như các Procedures:

*   **`approve_loan(loan_id, admin_id)`**: Procedure thực hiện kiểm tra sách còn không, nếu còn thì phê duyệt, cập nhật người phê duyệt, gán ngày mượn, ngày trả dự kiến, và trừ số lượng sách.
*   **`bulk_approve_loans(loan_ids, admin_id)`**: Procedure duyệt qua một danh sách các `loan_ids`. Với mỗi ID hợp lệ, nó áp dụng logic phê duyệt tương tự `approve_loan`. Tất cả được gom vào một lần `conn.commit()` cuối cùng để tối ưu hiệu suất.
*   **`transfer_loan(loan_id, new_user_id)`**: Procedure kiểm tra yêu cầu có đang `PENDING` hay không, sau đó chuyển quyền mượn sách sang cho user khác.

### B. Tầng Redis (Sử dụng Lua Scripts)
Để đảm bảo tính nguyên tử (Atomicity) giống như Stored Procedures khi làm việc với Redis, hệ thống sử dụng **Lua Scripts**. Redis cam kết rằng khi một Lua script đang chạy, không có script hay command nào khác có thể chen ngang.

*   **`LUA_ACQUIRE_LOCK` / `LUA_RELEASE_LOCK`**:
    *   Được sử dụng như các procedures để triển khai **Distributed Locks** (Khóa phân tán). 
    *   `LUA_ACQUIRE_LOCK`: Kiểm tra xem một khóa (`key`) đã tồn tại chưa. Nếu chưa, nó set khóa đó với một `owner` (người giữ khóa) và thời gian sống (`EX`), đảm bảo tính nguyên tử của thao tác "Kiểm tra và Thiết lập" (Check-and-Set).
    *   `LUA_RELEASE_LOCK`: Kiểm tra xem người yêu cầu nhả khóa có đúng là `owner` đang giữ khóa hay không. Nếu đúng, nó mới thực thi lệnh xóa khóa (`DEL`), tránh việc một user xóa nhầm khóa của user khác.
*   **`LUA_ADD_TO_CART` / `LUA_CHECKOUT`**:
    *   **`LUA_ADD_TO_CART`**: Đọc danh sách giỏ hàng hiện tại, kiểm tra xem sách đã có trong giỏ chưa. Nếu chưa, thêm vào cuối danh sách (`RPUSH`). Toàn bộ quá trình kiểm tra và thêm diễn ra trong một script nguyên tử.
    *   **`LUA_CHECKOUT`**: Trả về độ dài danh sách giỏ hàng, sau đó xóa giỏ hàng (`DEL`) trong cùng một giao dịch.

---
**Tổng kết:** Hệ thống kết hợp sự chặt chẽ của SQL Triggers cho các ràng buộc mức dữ liệu (Data-level constraints), sự linh hoạt của Python Transactions cho logic nghiệp vụ nhiều bước (Multi-step business logic), và tốc độ/tính nguyên tử của Redis Lua Scripts cho xử lý đồng thời (Concurrency handling).
