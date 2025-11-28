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
        String name = nameRegisterField.getText();
        String username = usernameRegisterField.getText();
        String password = passwordRegisterField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailRegisterField.getText();
        String phone = phoneRegisterField.getText();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Lỗi", "Mật khẩu không trùng khớp.");
            return;
        }

        // Tạo user mới (include email and phone)
        User newUser = new User(name, username, password, email, phone, Role.CLIENT);

        if (userDAO.registerUser(newUser) && !userDAO.isUserExists(newUser.getUsername(), newUser.getEmail(), newUser.getPhone())) {
            showAlert("Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.close();
        } else {
            showAlert("Thất bại", "Tên đăng nhập, email hoặc số điện thoại đã được sử dụng.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}