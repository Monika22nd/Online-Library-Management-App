package Java.controller;

import Java.database.UserDAO;
import Java.models.Role;
import Java.models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {
    @FXML private TextField nameRegisterField;
    @FXML private TextField usernameRegisterField;
    @FXML private PasswordField passwordRegisterField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private TextField emailRegisterField;
    @FXML private TextField phoneRegisterField;

    private UserDAO userDAO = new UserDAO();

    @FXML
    protected void handleRegisterButton(ActionEvent event){
        String name = nameRegisterField.getText().trim();
        String username = usernameRegisterField.getText().trim();
        String password = passwordRegisterField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailRegisterField.getText().trim();
        String phone = phoneRegisterField.getText().trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không trùng khớp.");
            return;
        }

        // KIỂM TRA TRÙNG (TỪ BRANCH MỚI)
        if (userDAO.isUserExists(username, email, phone)) {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Username, Email hoặc SĐT đã tồn tại.");
            return;
        }

        User newUser = new User(name, username, password, email, phone, Role.CLIENT);

        if (userDAO.registerUser(newUser)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.close();
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Đăng ký thất bại.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}