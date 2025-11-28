package controller;

import database.BookDAO;
import models.Book;
import models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.ListView;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HomescreenController {

    @FXML private Button homeButton;    // Dùng để lấy Stage
    @FXML private Label welcomeLabel;   // Hiển thị tên User
    @FXML private Button cartButton;    // Nút giỏ hàng
    @FXML private Button viewBorrowedButton; // Nút xem sách đã mượn

    // Container chứa danh sách sách (Quan trọng: Cần fx:id trong UserHomeScreen.fxml)
    @FXML private TilePane bookContainer;

    private User currentUser;
    private BookDAO bookDAO = new BookDAO();
    private List<Book> cart = new ArrayList<>(); // Giỏ hàng

    // NEW: store confirmed borrowed books
    private List<Book> borrowedBooks = new ArrayList<>();

    // Store previous scene so we can go back
    private Scene previousScene;

    // ==========================================
    // 1. KHỞI TẠO (Được gọi từ LoginController)
    // ==========================================
    public void initData(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Xin chào, " + user.getName());
        }
        updateCartUI(); // Reset số hiển thị về 0
        loadBooks();    // Bắt đầu tải sách
    }

    // ==========================================
    // 2. LOAD SÁCH (Sử dụng BookCard.fxml)
    // ==========================================
    private void loadBooks() {
        System.out.println("--> Bắt đầu tải sách..."); // Debug 1

        if (bookContainer == null) {
            System.err.println("LỖI: bookContainer bị NULL! Kiểm tra lại fx:id trong FXML.");
            return;
        }

        bookContainer.getChildren().clear();
        List<Book> books = bookDAO.getAllBooks();

        System.out.println("--> Found " + books.size() + " books in Database."); // Debug 2

        try {
            for (Book book : books) {
                System.out.println("--> Đang tạo thẻ cho sách: " + book.getTitle()); // Debug 3

                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/BookCard.fxml"));
                VBox cardBox = fxmlLoader.load();

                BookCardController cardController = fxmlLoader.getController();
                cardController.setData(book, this);

                bookContainer.getChildren().add(cardBox);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("LỖI: Không tìm thấy file BookCard.fxml hoặc lỗi load file.");
        }
    }

    // ==========================================
    // 3. LOGIC GIỎ HÀNG (Được BookCard gọi về)
    // ==========================================

    public void addToCart(Book book) {
        if (!cart.contains(book)) {
            cart.add(book);
            updateCartUI();
        }
    }

    public void removeFromCart(Book book) {
        cart.remove(book);
        updateCartUI();
    }

    private void updateCartUI() {
        if (cartButton != null) {
            cartButton.setText("Giỏ mượn (" + cart.size() + ")");
        }
    }

    // ==========================================
    // 4. CÁC SỰ KIỆN KHÁC
    // ==========================================

    @FXML
    private void handleCartClicked(ActionEvent event) {
        if (cart.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Giỏ mượn đang trống!");
            alert.showAndWait();
            return;
        }

        // Build detailed list of books for confirmation content
        StringBuilder content = new StringBuilder();
        content.append("Bạn có chắc muốn mượn ").append(cart.size()).append(" sách đã chọn?\n\n");
        content.append("Danh sách sách:\n");
        for (Book b : cart) {
            String author = "";
            try { author = b.getAuthorName(); } catch (Exception ex) { /* ignore if not available */ }
            content.append("- ").append(b.getTitle());
            if (author != null && !author.isEmpty()) content.append(" (").append(author).append(")");
            content.append("\n");
        }

        // Show confirmation dialog to finalize borrow with detailed content
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận mượn");
        confirm.setHeaderText(null);
        confirm.setContentText(content.toString());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Move items from cart into borrowedBooks and clear cart
            for (Book b : cart) {
                if (!borrowedBooks.contains(b)) {
                    borrowedBooks.add(b);
                }
            }
            cart.clear();
            updateCartUI();
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Thành công");
            success.setHeaderText(null);
            success.setContentText("Đã mượn sách thành công.");
            success.showAndWait();
        } else {
            // User cancelled
        }
    }

    @FXML
    private void handleViewBorrowedClicked(ActionEvent event) {
        Stage stage = (Stage) viewBorrowedButton.getScene().getWindow();
        // Save current scene to return later
        previousScene = viewBorrowedButton.getScene();

        // If no borrowed books, show a warning and return to current scene
        if (borrowedBooks.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Bạn chưa mượn cuốn sách nào.");
            alert.showAndWait();
            return;
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label title = new Label("Sách đã mượn (" + borrowedBooks.size() + ")");
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Book b : borrowedBooks) {
            // use available getters for display
            String author = "";
            try { author = b.getAuthorName(); } catch (Exception ex) { /* fallback if not available */ }
            items.add(b.getTitle() + (author.isEmpty() ? "" : " - " + author));
        }
        listView.setItems(items);
        VBox.setVgrow(listView, Priority.ALWAYS);

        Button backButton = new Button("Quay lại");
        backButton.setOnAction(e -> {
            if (previousScene != null) {
                stage.setScene(previousScene);
            }
        });

        root.getChildren().addAll(title, listView, backButton);

        Scene borrowedScene = new Scene(root, stage.getWidth(), stage.getHeight());
        stage.setScene(borrowedScene);
    }

    @FXML
    private void handleLogoutClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/LoginView.fxml"));
            Parent root = loader.load();

            // Lấy Stage hiện tại để chuyển cảnh
            Stage stage = (Stage) cartButton.getScene().getWindow();
            stage.setTitle("Login");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}