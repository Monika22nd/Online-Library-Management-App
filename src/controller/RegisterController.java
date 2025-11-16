package controller;

import java.awt.event.ActionEvent;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {
    @FXML
    protected TextField nameRegisterField;

    @FXML
    protected TextField usernameRegisterField;

    @FXML
    protected PasswordField passwordRegisterField;

    @FXML
    protected TextField showPasswordField;

    @FXML
    protected Button registerButton;

    protected void handleRegisterButton(ActionEvent event){
        String name = nameRegisterField.getText();
        String username = usernameRegisterField.getText();
        String password = passwordRegisterField.getText();

        showCompleteAlert();
    }

    protected void showCompleteAlert(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng ký thành công!");
        alert.setHeaderText(null);
        alert.setContentText("Đăng ký tài khoản thành công, vui lòng đăng nhập.");
    }
}
