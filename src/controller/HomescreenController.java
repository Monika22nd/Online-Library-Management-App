package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.Optional;

public class HomescreenController {
    @FXML
    protected Button homeButton;

    @FXML
    protected void handleHomeButtonClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserHomeScreen.fxml"));
            Parent root = loader.load();

            Scene homeScene = new Scene(root);
            Stage currentStage = (Stage) homeButton.getScene().getWindow();
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

    @FXML
    protected void handleAddMemberClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AddMember.fxml"));
            Parent root = loader.load();

            Stage addMemberStage = new Stage();
            addMemberStage.setTitle("Add Member");
            addMemberStage.setScene(new Scene(root));
            addMemberStage.initModality(Modality.APPLICATION_MODAL);
            addMemberStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            Stage currentStage = (Stage) homeButton.getScene().getWindow();
            currentStage.setScene(loginScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Book info handler (robustly obtains title from sibling Label in the slot)
    @FXML
    protected void handleBookInfoClicked(ActionEvent event) {
        Button src = (Button) event.getSource();
        String title = extractTitleFromSlot(src);
        if (title == null || title.isEmpty()) {
            title = "Unknown Book";
        }

        displayBookInfo(title);
    }

    private void displayBookInfo(String title) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Book Info");
        info.setHeaderText(title);
        info.setContentText("Details about " + title + " (author, year, description...).");
        //info.initOwner(homeButton.getScene().getWindow());
        try {
            info.showAndWait();
        } catch (Exception e) {
            System.err.println("Error displaying book info: " + e.getMessage());
        }
    }

    // Borrow handler (uses same title extraction)
    @FXML
    protected void handleBorrowClicked(ActionEvent event) {
        Button src = (Button) event.getSource();
        String title = extractTitleFromSlot(src);
        if (title == null || title.isEmpty()) title = "Unknown Book";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Borrow Confirmation");
        confirm.setHeaderText("Borrow " + title);
        confirm.setContentText("Do you want to borrow " + title + "?");
        //confirm.initOwner(homeButton.getScene().getWindow());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Alert done = new Alert(Alert.AlertType.INFORMATION);
            done.setTitle("Borrowed");
            done.setHeaderText(null);
            done.setContentText(title + " has been borrowed successfully.");
            //done.initOwner(homeButton.getScene().getWindow());
            done.showAndWait();
        }
    }

    // Helper: find the Label text inside the same VBox slot as the clicked button
    private String extractTitleFromSlot(Button src) {
        if (src == null) return null;
        Node maybeHBox = src.getParent(); // the HBox containing the buttons
        if (maybeHBox instanceof HBox) {
            Node maybeVBox = maybeHBox.getParent(); // the VBox containing image, label, hbox
            if (maybeVBox instanceof VBox) {
                VBox vbox = (VBox) maybeVBox;
                for (Node child : vbox.getChildren()) {
                    if (child instanceof Label) {
                        return ((Label) child).getText();
                    }
                }
            }
        }
        // fallback: try button text (not ideal but better than nothing)
        return src.getText();
    }
}
