package controller;

import database.BookDAO;
import database.LoanDAO;
import models.Book;
import models.Loan;
import models.Role;
import models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HomescreenController {

    @FXML private HBox topHBox; // Phải khớp fx:id trong FXML
    @FXML private Label welcomeLabel;
    @FXML private Button cartButton;
    @FXML private Button viewBorrowedButton;
    @FXML private TilePane bookContainer;
    @FXML private TextField searchField; // Logic tìm kiếm của bạn
    @FXML private Button historyButton;

    private User currentUser;
    private BookDAO bookDAO = new BookDAO();
    private LoanDAO loanDAO = new LoanDAO();
    private List<Book> cart = new ArrayList<>();
    private List<Book> masterBookList = new ArrayList<>(); // Danh sách gốc để tìm kiếm
    private Scene previousScene;

    public void initData(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) welcomeLabel.setText("Xin chào, " + user.getName());

        // 1. Thêm nút Admin nếu là Admin (Logic xịn)
        if (currentUser.getRole() == Role.ADMIN && topHBox != null) {
            Button adminBtn = new Button("Admin Panel");
            adminBtn.setOnAction(e -> openAdminPanel());
            topHBox.getChildren().add(topHBox.getChildren().size() - 2, adminBtn);
        }

        updateCartUI();
        // 2. Load sách và chuẩn bị tìm kiếm
        loadBooksFromDatabase();
        setupSearchListener();
    }

    private void openAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AdminPanel.fxml"));
            Parent root = loader.load();
            AdminController adminCtrl = loader.getController();
            adminCtrl.initData(currentUser); // Truyền user qua admin
            Stage stage = (Stage) cartButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Chưa có file AdminPanel.fxml!").showAndWait();
        }
    }

    // --- LOGIC TÌM KIẾM CỦA BẠN (GIỮ NGUYÊN) ---
    private void loadBooksFromDatabase(){
        masterBookList = bookDAO.getAllBooks();
        renderBooks(masterBookList);
    }

    private void setupSearchListener(){
        if(searchField == null) return;
        searchField.textProperty().addListener((obs, oldV, newV) -> filterBooks(newV));
    }

    private void filterBooks(String keyword){
        if(keyword == null || keyword.isEmpty()){
            renderBooks(masterBookList);
        } else {
            String lower = keyword.toLowerCase();
            List<Book> filtered = masterBookList.stream()
                    .filter(b -> b.getTitle().toLowerCase().contains(lower) ||
                            b.getAuthorName().toLowerCase().contains(lower) ||
                            b.getGenre().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
            renderBooks(filtered);
        }
    }

    private void renderBooks(List<Book> booksToRender) {
        if (bookContainer == null) return;
        bookContainer.getChildren().clear();
        try {
            for (Book book : booksToRender) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/BookCard.fxml"));
                VBox cardBox = fxmlLoader.load();
                BookCardController cardController = fxmlLoader.getController();
                cardController.setData(book, this);
                bookContainer.getChildren().add(cardBox);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- LOGIC GIỎ HÀNG & MƯỢN TRẢ ---

    public void addToCart(Book book) {
        if (!cart.contains(book)) { cart.add(book); updateCartUI(); }
    }

    public void removeFromCart(Book book) {
        cart.remove(book); updateCartUI();
    }

    private void updateCartUI() {
        if (cartButton != null) cartButton.setText("Giỏ mượn (" + cart.size() + ")");
    }

    @FXML
    private void handleCartClicked(ActionEvent event) {
        if (cart.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Giỏ trống!").showAndWait();
            return;
        }

        // Kiểm tra sách đã mượn chưa (Logic xịn)
        List<Integer> alreadyIds = loanDAO.getAlreadyBorrowedBookIds(currentUser.getId(), cart);
        List<Book> toBorrow = new ArrayList<>();
        for (Book b : cart) {
            if (!alreadyIds.contains(b.getId())) toBorrow.add(b);
        }

        if (toBorrow.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Tất cả sách trong giỏ bạn đang mượn rồi!").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Mượn " + toBorrow.size() + " quyển sách hợp lệ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean success = loanDAO.addLoans(currentUser.getId(), toBorrow);
            if (success) {
                new Alert(Alert.AlertType.INFORMATION, "Mượn thành công!").showAndWait();
                cart.clear();
                updateCartUI();
                loadBooksFromDatabase(); // Refresh lại để cập nhật số lượng tồn kho
            } else {
                new Alert(Alert.AlertType.ERROR, "Lỗi khi mượn (Có thể hết sách).").showAndWait();
            }
        }
    }

    @FXML
    private void handleViewBorrowedClicked(ActionEvent event) {
        // Chuyển cảnh sang màn hình trả sách (Logic xịn có nút Return)
        List<Loan> loans = loanDAO.getBorrowedLoansByUser(currentUser.getId());
        if (loans.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Bạn chưa mượn sách nào.").showAndWait();
            return;
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        Label title = new Label("Sách đang mượn (" + loans.size() + ")");
        ListView<HBox> listView = new ListView<>();
        VBox.setVgrow(listView, Priority.ALWAYS);

        for (Loan ln : loans) {
            Book b = bookDAO.getBookById(ln.getBookId());
            if (b == null) continue;
            Label lbl = new Label(b.getTitle() + " - " + b.getAuthorName() + " (Hạn: " + ln.getDueDate() + ")");
            Button retBtn = new Button("Trả sách");
            retBtn.setOnAction(e -> {
                if(loanDAO.returnBook(ln.getId())) {
                    new Alert(Alert.AlertType.INFORMATION, "Đã trả sách!").showAndWait();
                    handleViewBorrowedClicked(null); // Reload lại danh sách
                    loadBooksFromDatabase(); // Cập nhật lại kho ở màn hình chính
                }
            });
            HBox row = new HBox(10, lbl, retBtn);
            listView.getItems().add(row);
        }

        Button backBtn = new Button("Quay lại");
        previousScene = viewBorrowedButton.getScene();
        backBtn.setOnAction(e -> ((Stage) backBtn.getScene().getWindow()).setScene(previousScene));

        root.getChildren().addAll(title, listView, backBtn);
        Stage stage = (Stage) viewBorrowedButton.getScene().getWindow();
        stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
    }

    @FXML
    private void handleLogoutClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ui/LoginView.fxml"));
            Stage stage = (Stage) cartButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleHistoryClicked(ActionEvent event){
        List<Loan> history = loanDAO.getLoanHistoryByUser(currentUser.getId());

        // Tạo bảng hiển thị nhanh (Code giao diện Java thuần để đỡ phải tạo thêm file FXML)
        TableView<Loan> table = new TableView<>();
        ObservableList<Loan> data = FXCollections.observableArrayList(history);

        // Cột Tên sách
        TableColumn<Loan, String> bookCol = new TableColumn<>("Tên sách");
        bookCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("bookTitle"));

        // Cột Ngày mượn
        TableColumn<Loan, Date> borrowCol = new TableColumn<>("Ngày mượn");
        borrowCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("borrowDate"));

        // Cột Hạn trả
        TableColumn<Loan, Date> dueCol = new TableColumn<>("Hạn trả");
        dueCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dueDate"));

        // Cột Ngày trả thực tế
        TableColumn<Loan, Date> returnCol = new TableColumn<>("Ngày trả");
        returnCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("returnDate"));

        // Cột Trạng thái
        TableColumn<Loan, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));

        table.getColumns().addAll(bookCol, borrowCol, dueCol, returnCol, statusCol);
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefWidth(600);

        // Hiển thị bảng trong một hộp thoại (Dialog)
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lịch sử mượn sách");
        dialog.setHeaderText("Lịch sử của: " + currentUser.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); // Nút đóng
        dialog.getDialogPane().setContent(new VBox(table));
        dialog.setResizable(true);
        dialog.showAndWait();


    }
}