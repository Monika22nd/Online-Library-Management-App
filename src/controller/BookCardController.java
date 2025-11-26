package controller;

import models.Book;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class BookCardController {
    @FXML private ImageView bookImage;
    @FXML private Label bookTitle;
    @FXML private Hyperlink bookAuthor;
    @FXML private CheckBox selectCheckbox;

    private Book book;
    private HomescreenController mainController; // Để gọi ngược lại khi cần (ví dụ update giỏ hàng)

    // Hàm này được gọi từ HomescreenController để điền dữ liệu
    public void setData(Book book, HomescreenController mainController) {
        this.book = book;
        this.mainController = mainController;

        bookTitle.setText(book.getTitle());
        bookAuthor.setText(book.getAuthorName());

        // Load ảnh (nếu chưa có ảnh thật thì dùng ảnh mặc định)
        // bookImage.setImage(new Image(getClass().getResourceAsStream("/img/book_placeholder.png")));

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
        // Gọi sang HomescreenController để thêm/xóa khỏi giỏ
        if (selectCheckbox.isSelected()) {
            mainController.addToCart(book);
        } else {
            mainController.removeFromCart(book);
        }
    }
}