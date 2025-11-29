package controller;

import models.Book;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.InputStream;

public class BookCardController {
    @FXML private ImageView bookImage;
    @FXML private Label bookTitle;
    @FXML private Hyperlink bookAuthor;
    @FXML private CheckBox selectCheckbox;

    private Book book;
    private HomescreenController mainController;

    public void setData(Book book, HomescreenController mainController) {
        this.book = book;
        this.mainController = mainController;

        bookTitle.setText(book.getTitle());
        bookAuthor.setText(book.getAuthorName());

        // --- XỬ LÝ ẢNH CHUẨN ---
        loadImageSafe(book.getImagePath());

        // Xử lý nút tác giả
        bookAuthor.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Tác giả: " + book.getAuthorName());
            alert.showAndWait();
        });

        // Xử lý nếu hết sách
        if (!book.isAvailable()) {
            selectCheckbox.setDisable(true);
            selectCheckbox.setText("Hết hàng");
        }
    }

    private void loadImageSafe(String path) {
        Image image = null;
        try {
            // 1. Nếu path rỗng hoặc null -> Dùng ảnh placeholder
            if (path == null || path.trim().isEmpty()) {
                loadPlaceholder();
                return;
            }

            // 2. Nếu là đường dẫn file tuyệt đối (ổ đĩa) hoặc URL (http)
            if (path.startsWith("file:") || path.startsWith("http")) {
                image = new Image(path, true); // true = load background (không treo UI)
            }
            // 3. Nếu là đường dẫn trong Resources (bắt đầu bằng /)
            else {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    image = new Image(is);
                } else {
                    // Không tìm thấy file trong resource
                    System.err.println("Không tìm thấy ảnh resource: " + path);
                    loadPlaceholder();
                    return;
                }
            }

            // Set ảnh vào ImageView
            if (image != null) {
                // Xử lý khi ảnh load lỗi (ví dụ link chết)
                image.errorProperty().addListener((obs, oldV, newV) -> {
                    if (newV) loadPlaceholder();
                });
                bookImage.setImage(image);
            } else {
                loadPlaceholder();
            }

        } catch (Exception e) {
            e.printStackTrace();
            loadPlaceholder();
        }
    }

    private void loadPlaceholder() {
        try {
            // Đảm bảo bạn đã có file book_placeholder.png trong /src/main/resources/img/
            bookImage.setImage(new Image(getClass().getResourceAsStream("/img/placeholder.png")));
        } catch (Exception e) {
            // Trường hợp xấu nhất: không có cả ảnh placeholder
            System.err.println("Thiếu ảnh placeholder!");
        }
    }

    // ... Giữ nguyên các hàm handleDetails, handleSelect
    @FXML
    private void handleDetails(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Chi tiết sách");
        info.setHeaderText(book.getTitle());
        info.setContentText(
                "Tác giả: " + book.getAuthorName() + "\n" +
                        "Thể loại: " + book.getGenre() + "\n" +
                        "Giá: " + book.getPrice() + "\n" +
                        "Mô tả: " + book.getDescription()
        );
        info.showAndWait();
    }

    @FXML
    private void handleSelect(ActionEvent event) {
        if (selectCheckbox.isSelected()) {
            mainController.addToCart(book);
        } else {
            mainController.removeFromCart(book);
        }
    }
}