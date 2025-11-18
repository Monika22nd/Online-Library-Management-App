package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;

public class HomescreenController {
    @FXML
    protected Hyperlink homescreenHyperLink;

    @FXML
    protected Image logoImage;

    @FXML
    protected ImageView logoPic;

    @FXML
    protected void handleImageClicked(MouseEvent event){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserHomeScreen.fxml"));
            Parent root = loader.load();

            Scene homeScene = new Scene(root);
            Stage currentStage = (Stage) logoPic.getScene().getWindow();
            currentStage.setScene(homeScene);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
