package Java.controller;

import Java.database.BookDAO;
import Java.database.LoanDAO;
import Java.database.UserDAO;
import Java.models.Book;
import Java.models.Loan;
import Java.models.Role;
import Java.models.User;
import Java.util.SceneHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HomescreenController {

    @FXML private HBox topHBox;
    @FXML private Label welcomeLabel;

    // Các nút này sẽ được FXML mapping dù nó nằm ở dưới đáy (bottom)
    @FXML private Button cartButton;
    @FXML private Button viewBorrowedButton;

    @FXML private TilePane bookContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> genreCombo;

    private User currentUser;
    private BookDAO bookDAO = new BookDAO();
    private LoanDAO loanDAO = new LoanDAO();
    private UserDAO userDAO = new UserDAO();

    private List<Book> cart = new ArrayList<>();
    private List<Book> masterBookList = new ArrayList<>();

    public void initData(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) welcomeLabel.setText("Xin chào, " + user.getName());

        if (currentUser != null && currentUser.getRole() == Role.ADMIN && topHBox != null) {
            Button adminBtn = new Button("Admin Panel");
            adminBtn.setOnAction(e -> openAdminPanel());
            topHBox.getChildren().add(topHBox.getChildren().size() - 2, adminBtn);
        }

        updateCartUI();
        loadBooksFromDatabase();
        setupSearchListener();
        setupGenreCombo();
    }

    private void openAdminPanel() {
        try {
            Stage stage = SceneHelper.getStage(cartButton); // Lấy stage từ nút bất kỳ
            SceneHelper.switchScene(stage, "/ui/AdminPanel.fxml", "Admin Panel", controller -> {
                if (controller instanceof AdminController) {
                    ((AdminController) controller).initData(currentUser);
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadBooksFromDatabase() {
        masterBookList = bookDAO.getAllBooks();
        renderBooks(masterBookList);
    }

    private void renderBooks(List<Book> books) {
        if (bookContainer == null) return;
        bookContainer.getChildren().clear();
        try {
            for (Book book : books) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/BookCard.fxml"));
                VBox cardBox = fxmlLoader.load();
                BookCardController cardController = fxmlLoader.getController();
                cardController.setData(book, this);
                bookContainer.getChildren().add(cardBox);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- TÌM KIẾM & LỌC ---
    private void setupSearchListener() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, old, newVal) -> filterBooks());
    }

    private void setupGenreCombo() {
        if (genreCombo == null) return;
        List<String> genres = bookDAO.getAllGenres();
        List<String> items = new ArrayList<>();
        items.add("Tất cả");
        items.addAll(genres);
        genreCombo.setItems(FXCollections.observableArrayList(items));
        genreCombo.getSelectionModel().selectFirst();
        genreCombo.valueProperty().addListener((obs, old, newV) -> filterBooks());
    }

    private void filterBooks() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String genre = genreCombo.getValue();
        boolean allGenres = genre == null || genre.equals("Tất cả");

        List<Book> filtered = masterBookList.stream()
                .filter(b -> {
                    boolean matchText = b.getTitle().toLowerCase().contains(keyword) ||
                            b.getAuthorName().toLowerCase().contains(keyword);
                    boolean matchGenre = allGenres || (b.getGenre() != null && b.getGenre().equalsIgnoreCase(genre));
                    return matchText && matchGenre;
                })
                .collect(Collectors.toList());
        renderBooks(filtered);
    }

    // --- CẬP NHẬT HỒ SƠ ---
    @FXML
    private void handleUpdateProfileClicked(ActionEvent event) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Cập nhật hồ sơ");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField nameF = new TextField(currentUser.getName());
        TextField emailF = new TextField(currentUser.getEmail());
        TextField phoneF = new TextField(currentUser.getPhone());
        PasswordField passF = new PasswordField();
        passF.setText(currentUser.getPassword());

        grid.addRow(0, new Label("Tên:"), nameF);
        grid.addRow(1, new Label("Email:"), emailF);
        grid.addRow(2, new Label("SĐT:"), phoneF);
        grid.addRow(3, new Label("Mật khẩu:"), passF);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ?
                new User(currentUser.getId(), nameF.getText(), currentUser.getUsername(), passF.getText(), emailF.getText(), phoneF.getText(), currentUser.getRole()) : null);

        dialog.showAndWait().ifPresent(updated -> {
            if (userDAO.updateUser(updated)) {
                currentUser = updated;
                welcomeLabel.setText("Xin chào, " + currentUser.getName());
                new Alert(Alert.AlertType.INFORMATION, "Cập nhật thành công!").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Lỗi cập nhật.").showAndWait();
            }
        });
    }

    // --- XUẤT PHIẾU MƯỢN (MỚI) ---
    private void exportLoanReceipt(User user, List<Book> books) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "PhieuMuon_" + user.getUsername() + "_" + timestamp + ".txt";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("========== PHIẾU MƯỢN SÁCH ==========\n");
            writer.write("Thư viện: Java Library System\n");
            writer.write("Khách hàng: " + user.getName() + " (SĐT: " + user.getPhone() + ")\n");
            writer.write("Ngày lập: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n");
            writer.write("-------------------------------------\n");
            writer.write("DANH SÁCH SÁCH MƯỢN:\n");

            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                writer.write(String.format("%d. %s - %s\n", (i + 1), b.getTitle(), b.getAuthorName()));
            }

            writer.write("-------------------------------------\n");
            writer.write("Hạn trả: 14 ngày kể từ ngày mượn.\n");
            writer.write("Vui lòng trả sách đúng hạn để tránh phạt.\n");
            writer.write("=====================================\n");

            System.out.println("Đã xuất file: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi xuất file phiếu mượn.").showAndWait();
        }
    }

    // --- GIỎ HÀNG & MƯỢN SÁCH ---
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

        List<Integer> alreadyIds = loanDAO.getAlreadyBorrowedBookIds(currentUser.getId(), cart);
        Set<Integer> alreadySet = new HashSet<>(alreadyIds);

        List<Book> validBooks = cart.stream()
                .filter(b -> !alreadySet.contains(b.getId()))
                .collect(Collectors.toList());

        if (validBooks.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Tất cả sách trong giỏ bạn đang mượn rồi!").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Mượn " + validBooks.size() + " cuốn sách?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean success = loanDAO.addLoans(currentUser.getId(), validBooks);
            if (success) {
                // GỌI HÀM XUẤT PHIẾU
                exportLoanReceipt(currentUser, validBooks);

                new Alert(Alert.AlertType.INFORMATION, "Mượn thành công! Đã xuất phiếu mượn ra file.").showAndWait();
                cart.clear();
                updateCartUI();
                loadBooksFromDatabase();
            } else {
                new Alert(Alert.AlertType.ERROR, "Lỗi khi mượn.").showAndWait();
            }
        }
    }

    // --- LỊCH SỬ MƯỢN (MỚI) ---
    @FXML
    private void handleHistoryClicked(ActionEvent event) {
        List<Loan> history = loanDAO.getLoanHistoryByUser(currentUser.getId());

        TableView<Loan> table = new TableView<>();
        ObservableList<Loan> data = FXCollections.observableArrayList(history);

        TableColumn<Loan, String> bookCol = new TableColumn<>("Tên sách");
        bookCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("bookTitle"));

        TableColumn<Loan, Date> borrowCol = new TableColumn<>("Ngày mượn");
        borrowCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("borrowDate"));

        TableColumn<Loan, Date> dueCol = new TableColumn<>("Hạn trả");
        dueCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dueDate"));

        TableColumn<Loan, Date> returnCol = new TableColumn<>("Ngày trả");
        returnCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("returnDate"));

        TableColumn<Loan, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));

        table.getColumns().addAll(bookCol, borrowCol, dueCol, returnCol, statusCol);
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefWidth(700);
        table.setPrefHeight(400);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lịch sử mượn sách");
        dialog.setHeaderText("Lịch sử mượn trả của: " + currentUser.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new VBox(table));
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    @FXML
    private void handleViewBorrowedClicked(ActionEvent event) {
        try {
            Stage stage = SceneHelper.getStage(viewBorrowedButton); // Lấy stage từ nút này
            Scene previousScene = viewBorrowedButton.getScene();
            SceneHelper.switchScene(stage, "/ui/BorrowedBooks.fxml", "Sách đã mượn", controller -> {
                if (controller instanceof BorrowedBooksController) {
                    ((BorrowedBooksController) controller).initData(currentUser, previousScene);
                }
            });
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    @FXML
    private void handleLogoutClicked(ActionEvent event) {
        try {
            // Lấy stage từ nút bất kỳ, ví dụ cartButton (vì nút logout nằm trong menu)
            // Hoặc an toàn hơn dùng welcomeLabel
            Stage stage = SceneHelper.getStage(welcomeLabel);
            SceneHelper.switchScene(stage, "/ui/LoginView.fxml", "Login", null);
        } catch (IOException e) { e.printStackTrace(); }
    }
}