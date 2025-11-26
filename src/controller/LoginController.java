package controller;

import database.UserDAO;
import models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;

    private UserDAO userDAO = new UserDAO();

    @FXML
    protected void handleLoginButton(ActionEvent event){
        String username = usernameField.getText();
        String password = passwordField.getText();

        User user = userDAO.login(username, password);

        if (user != null) {
            try {
                openHomeScreen(user);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showErrorAlert();
        }
    }

    private void openHomeScreen(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserHomeScreen.fxml"));
        Parent root = loader.load();

        HomescreenController homeController = loader.getController();
        homeController.initData(user);

        Stage stage = new Stage();
        stage.setTitle("Library Management System");
        stage.setScene(new Scene(root));
        stage.show();

        Stage loginStage = (Stage) loginButton.getScene().getWindow();
        loginStage.close();
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

    private void showErrorAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Failed");
        alert.setContentText("Sai tên đăng nhập hoặc mật khẩu.");
        alert.showAndWait();
    }
}