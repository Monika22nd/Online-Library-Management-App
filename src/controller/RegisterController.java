package controller;

import java.util.*;

import database.TestDB;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {
    @FXML
    protected TextField nameRegisterField;

    @FXML
    protected TextField usernameRegisterField;

    @FXML
    protected PasswordField passwordRegisterField;

    @FXML
    protected Button registerButton;

    @FXML
    protected PasswordField confirmPasswordField;

    @FXML
    protected void handleRegisterButton(ActionEvent event){
        TestDB db = TestDB.getInstance();
        String name = nameRegisterField.getText();
        String username = usernameRegisterField.getText();
        String password = passwordRegisterField.getText();
        String confirmPassword = confirmPasswordField.getText();
        if (username != null && password.equals(confirmPassword)){
            db.registerUser(username, password);
            showCompleteAlert();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
        }
        else {
            showErrorAlert();
        }
    }

    protected void showErrorAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Failed");
        alert.setHeaderText(null);
        alert.setContentText("Something went wrong");
    }

    protected void showCompleteAlert(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng ký thành công!");
        alert.setHeaderText(null);
        alert.setContentText("Đăng ký tài khoản thành công, vui lòng đăng nhập.");
    }
}
