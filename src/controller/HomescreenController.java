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
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomescreenController {

    @FXML private Button homeButton;    // Dùng để lấy Stage
    @FXML private Label welcomeLabel;   // Hiển thị tên User
    @FXML private Button cartButton;    // Nút giỏ hàng

    // Container chứa danh sách sách (Quan trọng: Cần fx:id trong UserHomeScreen.fxml)
    @FXML private TilePane bookContainer;
    @FXML private TextField searchField;

    private User currentUser;
    private BookDAO bookDAO = new BookDAO();
    private List<Book> cart = new ArrayList<>();//

    // --- 1. DANH SÁCH GỐC (Chứa toàn bộ sách lấy từ DB) ---// Giỏ hàng
    private List<Book> masterBookList = new ArrayList<>();

    public void initData(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Xin chào, " + user.getName());
        }
        updateCartUI(); // Reset số hiển thị về 0
        //loadBooks();    // Bắt đầu tải sách
        loadBooksFromDatabase();// Tải dữ liệu từ DB
        setupSearchListener();// Kích hoạt tính năng tìm kiếm
    }

    private void loadBooksFromDatabase(){
        masterBookList = bookDAO.getAllBooks(); // Đưa DB của sách vào danh sách gốc

        renderBooks(masterBookList); //Hiện thị ra màn hình
    }

    // --- 2. LẮNG NGHE SỰ KIỆN GÕ PHÍM ---
    private void setupSearchListener(){
        if(searchField == null) {
            return ;
        }
        // Mỗi khi nội dung trong ô search thay đổi, hàm này sẽ chạy
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterBooks(newValue);
        });
    }

    private void filterBooks(String keyword){
        if(keyword == null || keyword.isEmpty()){
            // Nếu ô tìm kiếm rỗng -> Hiện lại tất cả
            renderBooks(masterBookList);
        }else{
            // Chuyển từ khóa về chữ thường để tìm không phân biệt hoa thường
            String lowerCaseKeyword = keyword.toLowerCase();

            List<Book> filteredList = masterBookList.stream()
                    .filter(b ->
                            b.getTitle().toLowerCase().contains(lowerCaseKeyword)||
                            b.getAuthorName().toLowerCase().contains(lowerCaseKeyword)||
                            b.getGenre().toLowerCase().contains(lowerCaseKeyword)
                    )
                    .collect(Collectors.toList());

            renderBooks(filteredList);
        }
    }

    // --- 3. HIỂN THỊ SÁCH --- //

    private void renderBooks(List<Book> booksToRender) {
        if (bookContainer == null) return;

        bookContainer.getChildren().clear(); // Xóa danh sách cũ đang hiện

        try {
            for (Book book : booksToRender) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/BookCard.fxml"));
                VBox cardBox = fxmlLoader.load();

                BookCardController cardController = fxmlLoader.getController();
                cardController.setData(book, this); // Truyền dữ liệu vào card

                bookContainer.getChildren().add(cardBox);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không thể load BookCard.fxml");
        }
    }

    // ==========================================
    // 2. LOAD SÁCH (Sử dụng BookCard.fxml)
    // ==========================================
    /*private void loadBooks() {
        System.out.println("--> Bắt đầu tải sách..."); // Debug 1

        if (bookContainer == null) {
            System.err.println("LỖI: bookContainer bị NULL! Kiểm tra lại fx:id trong FXML.");
            return;
        }

        bookContainer.getChildren().clear();
        List<Book> books = bookDAO.getAllBooks();

        System.out.println("--> Tìm thấy " + books.size() + " quyển sách trong Database."); // Debug 2

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
    }*/

    // ==========================================
    // 4. LOGIC GIỎ HÀNG (Được BookCard gọi về)
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
    // 5. CÁC SỰ KIỆN KHÁC
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

        // Hiện tại chỉ hiện thông báo, sau này sẽ mở màn hình ConfirmBorrow
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Xác nhận mượn");
        alert.setHeaderText("Sách đã chọn (" + cart.size() + "):");
        StringBuilder content = new StringBuilder();
        for (Book b : cart) {
            content.append("- ").append(b.getTitle()).append("\n");
        }
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleLogoutClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/LoginView.fxml"));
            Parent root = loader.load();

            // Lấy Stage hiện tại để chuyển cảnh
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Login");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}