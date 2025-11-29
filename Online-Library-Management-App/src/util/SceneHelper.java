package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Small helper utilities for loading FXML and switching scenes.
 * Callers handle IOExceptions to keep control over error reporting.
 */
public final class SceneHelper {

    // Prevent instantiation
    private SceneHelper() {}

    /**
     * Load FXML and return the FXMLLoader instance (already loaded).
     * Caller can access loader.getController() and loader.getRoot().
     * @param fxml resource path (e.g. "/ui/Homescreen.fxml")
     */
    public static FXMLLoader getLoader(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneHelper.class.getResource(fxml));
        loader.load();
        return loader;
    }

    /**
     * Switch the given stage to the root loaded by the provided FXMLLoader.
     * Keeps the current stage size and centers on screen.
     */
    public static void switchScene(Stage stage, FXMLLoader loader, String title) {
        Parent root = loader.getRoot();
        stage.setTitle(title);
        // Use existing stage dimensions for a smoother transition
        stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        stage.centerOnScreen();
    }

    /**
     * Convenience: load FXML, optionally initialize controller, then switch scene.
     * Propagates IOException for caller to report/fallback.
     */
    public static void switchScene(Stage stage, String fxml, String title, Consumer<Object> initController) throws IOException {
        FXMLLoader loader = getLoader(fxml);
        if (initController != null) {
            initController.accept(loader.getController());
        }
        switchScene(stage, loader, title);
    }

    /**
     * Helper to get Stage from any Node on the scene graph.
     */
    public static Stage getStage(Node node) {
        return (Stage) node.getScene().getWindow();
    }
}
