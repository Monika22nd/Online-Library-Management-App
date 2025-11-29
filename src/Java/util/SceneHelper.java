package Java.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.function.Consumer;

public class SceneHelper {

    // Lấy Stage từ một control bất kỳ (Nút, Table, v.v.)
    public static Stage getStage(Control control) {
        if (control == null || control.getScene() == null) return null;
        return (Stage) control.getScene().getWindow();
    }

    // Hàm chuyển cảnh đa năng
    public static void switchScene(Stage stage, String fxmlPath, String title, Consumer<Object> controllerSetup) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneHelper.class.getResource(fxmlPath));
        Parent root = loader.load();

        // Nếu cần truyền dữ liệu vào controller mới (ví dụ: truyền user hiện tại)
        if (controllerSetup != null) {
            Object controller = loader.getController();
            controllerSetup.accept(controller);
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        if (title != null) stage.setTitle(title);
        stage.centerOnScreen();
        stage.show();
    }
}