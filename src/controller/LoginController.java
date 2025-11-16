package controller;

import javafx.event.ActionEvent;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;

public class LoginController {
    @FXML
    protected TextField usernameField;

    @FXML
    protected PasswordField passwordField;

    @FXML
    protected Button loginButton;

    @FXML
    protected void handleLoginButton(ActionEvent event){
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.equals("admin") && password.equals("1")){
            System.out.println("Done");
        }
        else {
            showErrorAlert();
        }
    }

    protected void showErrorAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Failed!");
        alert.setHeaderText(null);
        alert.setContentText("Invalid username or password");
        alert.showAndWait();
    }
}
