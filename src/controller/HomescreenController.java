package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public class HomescreenController {
    @FXML
    protected Hyperlink homescreenHyperLink;

    @FXML
    protected Image logoImage;

    @FXML
    protected ImageView logoPic;

    @FXML
    protected void handleImageClicked(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserHomeScreen.fxml"));
            Parent root = loader.load();

            Scene homeScene = new Scene(root);
            Stage currentStage = (Stage) logoPic.getScene().getWindow();
            currentStage.setScene(homeScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleAddBookClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AddBook.fxml"));
            Parent root = loader.load();

            Stage addBookStage = new Stage();
            addBookStage.setTitle("Add Book");
            addBookStage.setScene(new Scene(root));
            addBookStage.initModality(Modality.APPLICATION_MODAL);
            addBookStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleViewBooksClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/ViewBooks.fxml"));
            Parent root = loader.load();

            Stage viewBooksStage = new Stage();
            viewBooksStage.setTitle("View Books");
            viewBooksStage.setScene(new Scene(root));
            viewBooksStage.initModality(Modality.APPLICATION_MODAL);
            viewBooksStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // @FXML
    // protected void handleAddMemberClicked(ActionEvent event) {
    //     try {
    //         FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AddMember.fxml"));
    //         Parent root = loader.load();

    //         Stage addMemberStage = new Stage();
    //         addMemberStage.setTitle("Add Member");
    //         addMemberStage.setScene(new Scene(root));
    //         addMemberStage.initModality(Modality.APPLICATION_MODAL);
    //         addMemberStage.showAndWait();
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    @FXML
    protected void handleViewMembersClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/ViewMembers.fxml"));
            Parent root = loader.load();

            Stage viewMembersStage = new Stage();
            viewMembersStage.setTitle("View Members");
            viewMembersStage.setScene(new Scene(root));
            viewMembersStage.initModality(Modality.APPLICATION_MODAL);
            viewMembersStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleChangePasswordClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/ChangePassword.fxml"));
            Parent root = loader.load();

            Stage changePasswordStage = new Stage();
            changePasswordStage.setTitle("Change Password");
            changePasswordStage.setScene(new Scene(root));
            changePasswordStage.initModality(Modality.APPLICATION_MODAL);
            changePasswordStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleLogoutClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/LoginView.fxml"));
            Parent root = loader.load();

            Scene loginScene = new Scene(root);
            Stage currentStage = (Stage) logoPic.getScene().getWindow();
            currentStage.setScene(loginScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
