package controller;

import database.UserDAO;
import models.Role;
import models.User;
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
        String name = nameRegisterField.getText() == null ? "" : nameRegisterField.getText().trim();
        String username = usernameRegisterField.getText() == null ? "" : usernameRegisterField.getText().trim();
        String password = passwordRegisterField.getText() == null ? "" : passwordRegisterField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String email = emailRegisterField.getText() == null ? "" : emailRegisterField.getText().trim();
        String phone = phoneRegisterField.getText() == null ? "" : phoneRegisterField.getText().trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ thông tin (name, username, password).");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu không trùng khớp.");
            return;
        }

        // Check if user (username/email/phone) already exists BEFORE attempting to register
        if (userDAO.isUserExists(username, email, phone)) {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Tên đăng nhập, email hoặc số điện thoại đã được sử dụng.");
            return;
        }

        // Tạo user mới (include email and phone)
        User newUser = new User(name, username, password, email, phone, Role.CLIENT);

        // Attempt to persist the new user
        boolean created = userDAO.registerUser(newUser);
        if (created) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.close();
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Đăng ký thất bại. Vui lòng thử lại.");
        }
    }

    // Replace with helper that accepts AlertType so callers can show error/info appropriately
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}