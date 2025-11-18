package controller;

import database.TestDB;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    protected TextField usernameField;

    @FXML
    protected PasswordField passwordField;

    @FXML
    protected Button loginButton;

    @FXML
    protected Hyperlink registerLink;

    @FXML
    protected void handleLoginButton(ActionEvent event){
        TestDB db = TestDB.getInstance();

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.equals("admin") && password.equals("1")){
            System.out.println("Done");
        }
        else if ((username.equals("testuser") && password.equals("10")) || db.checkLogin(username, password)){
            try{
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserHomeScreen.fxml"));
                Parent root = loader.load();

                Stage stage = new Stage();
                stage.setTitle("Home");
                stage.setScene(new Scene(root));
                stage.show();

                Stage loginStage = (Stage) loginButton.getScene().getWindow();
                loginStage.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        else {
            showErrorAlert();
        }
    }

    @FXML
    protected void handleLinkClicked(ActionEvent event){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/RegisterView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Register");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e){
            e.printStackTrace();
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
