package controller;

import database.LoanDAO;
import database.BookDAO;
import database.UserDAO; // NEW
import models.Book;
import models.Loan;
import models.User;
import models.Role;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import models.Author; // NEW
import util.SceneHelper; // NEW

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.sql.Date;
import java.util.stream.Collectors; // NEW

public class HomescreenController {

    @FXML private HBox topHBox; // injected from FXML (new)
    @FXML private Button homeButton;    // Dùng để lấy Stage
    @FXML private Label welcomeLabel;   // Hiển thị tên User
    @FXML private Button cartButton;    // Nút giỏ hàng
    @FXML private Button viewBorrowedButton; // Nút xem sách đã mượn

    // Container chứa danh sách sách (Quan trọng: Cần fx:id trong UserHomeScreen.fxml)
    @FXML private TilePane bookContainer;

    // --- Added search field ---
    @FXML private TextField searchField; // NEW
    @FXML private ComboBox<String> genreCombo; // NEW

    private User currentUser;
    private BookDAO bookDAO = new BookDAO();
    private LoanDAO loanDAO = new LoanDAO(); // persist loans and query existing
    private List<Book> cart = new ArrayList<>(); // Giỏ hàng

    // NEW: store confirmed borrowed books
    private List<Book> borrowedBooks = new ArrayList<>();

    // Store previous scene so we can go back
    private Scene previousScene;

    // --- master list for searching ---
    private List<Book> masterBookList = new ArrayList<>(); // NEW

    // NEW: user data access object
    private final UserDAO userDAO = new UserDAO();

    // ==========================================
    // 1. KHỞI TẠO (Được gọi từ LoginController)
    // ==========================================
    public void initData(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Xin chào, " + user.getName());
        }

        // Add admin button dynamically if user is ADMIN
        if (currentUser != null && currentUser.getRole() == Role.ADMIN && topHBox != null) {
            Button adminBtn = new Button("Admin Panel");
            adminBtn.setOnAction(e -> openAdminPanel());
            // add before viewBorrowedButton (keep layout)
            topHBox.getChildren().add(topHBox.getChildren().size() - 2, adminBtn);
        }

        updateCartUI(); // Reset số hiển thị về 0

        // load data and enable search + genre dropdown
        loadBooksFromDatabase();
        setupSearchListener();
        setupGenreCombo();
    }

    private void openAdminPanel() {
        try {
            Stage stage = SceneHelper.getStage(cartButton);
            SceneHelper.switchScene(stage, "/ui/AdminPanel.fxml", "Admin Panel", controller -> {
                if (controller instanceof AdminController) {
                    ((AdminController) controller).initData(currentUser);
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR, "Không thể mở Admin Panel: " + ex.getMessage(), ButtonType.OK);
            err.showAndWait();
        }
    }

    // ==========================================
    // 2. LOAD SÁCH (Sử dụng BookCard.fxml)
    // ==========================================
    // Replaced original direct-loading behavior with master list + render function
    private void loadBooksFromDatabase() {
        masterBookList = bookDAO.getAllBooks();
        renderBooks(masterBookList);
    }

    // Render a list of books into the TilePane (re-uses BookCard.fxml)
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
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("LỖI: Không thể load BookCard.fxml");
        }
    }

    // Setup listener to filter books on each keystroke
    private void setupSearchListener() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, oldText, newText) -> filterBooks(newText));
    }

    // populate genre dropdown and add listener
    private void setupGenreCombo() {
        if (genreCombo == null) return;
        List<String> genres = bookDAO.getAllGenres();
        if (genres == null) genres = new ArrayList<>();
        List<String> items = new ArrayList<>();
        items.add("Tất cả");
        items.addAll(genres);
        genreCombo.setItems(FXCollections.observableArrayList(items));
        genreCombo.getSelectionModel().selectFirst();
        genreCombo.valueProperty().addListener((obs, oldV, newV) -> {
            filterBooks(searchField != null ? searchField.getText() : "");
        });
    }

    private void filterBooks(String keyword) {
        String kw = (keyword == null) ? "" : keyword.trim().toLowerCase();
        String selGenre = "";
        if (genreCombo != null && genreCombo.getValue() != null && !"Tất cả".equals(genreCombo.getValue())) {
            selGenre = genreCombo.getValue().toLowerCase();
        }

        final String lower = kw;
        final String genreFilter = selGenre;
        List<Book> filtered = masterBookList.stream()
                .filter(b -> {
                    String title = b.getTitle() == null ? "" : b.getTitle().toLowerCase();
                    String author = b.getAuthorName() == null ? "" : b.getAuthorName().toLowerCase();
                    String genre = b.getGenre() == null ? "" : b.getGenre().toLowerCase();
                    boolean txtMatch = lower.isEmpty() || title.contains(lower) || author.contains(lower) || genre.contains(lower);
                    boolean genreMatch = genreFilter.isEmpty() || genre.equals(genreFilter);
                    return txtMatch && genreMatch;
                })
                .collect(Collectors.toList());
        renderBooks(filtered);
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

    // --- New: update user profile from Menu item ---
    @FXML
    private void handleUpdateProfileClicked(javafx.event.ActionEvent event) {
        if (currentUser == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Không có người dùng hợp lệ.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Cập nhật hồ sơ");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField nameF = new TextField(currentUser.getName());
        TextField userF = new TextField(currentUser.getUsername()); userF.setDisable(true);
        PasswordField passF = new PasswordField(); passF.setText(currentUser.getPassword());
        TextField emailF = new TextField(currentUser.getEmail());
        TextField phoneF = new TextField(currentUser.getPhone());
        // optional: let user choose preferred genre (not persisted unless User/DB supports it)
        ComboBox<String> prefGenre = new ComboBox<>();
        if (genreCombo != null && genreCombo.getItems() != null) prefGenre.setItems(genreCombo.getItems());
        prefGenre.getSelectionModel().selectFirst();

        grid.addRow(0, new Label("Name:"), nameF);
        grid.addRow(1, new Label("User:"), userF);
        grid.addRow(2, new Label("Pass:"), passF);
        grid.addRow(3, new Label("Email:"), emailF);
        grid.addRow(4, new Label("Phone:"), phoneF);
        grid.addRow(5, new Label("Favorite genre:"), prefGenre);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? new User(currentUser.getId(), nameF.getText(), userF.getText(), passF.getText(), emailF.getText(), phoneF.getText(), currentUser.getRole()) : null);

        dialog.showAndWait().ifPresent(updated -> {
            if (userDAO.updateUser(updated)) {
                currentUser = updated;
                if (welcomeLabel != null) welcomeLabel.setText("Xin chào, " + currentUser.getName());
                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cập nhật thành công.", ButtonType.OK);
                ok.showAndWait();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "Cập nhật thất bại.", ButtonType.OK);
                err.showAndWait();
            }
        });
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

        // Check which selected books are already borrowed by this user
        List<Integer> alreadyIds = loanDAO.getAlreadyBorrowedBookIds(currentUser != null ? currentUser.getId() : -1, cart);
        Set<Integer> alreadySet = new HashSet<>(alreadyIds);

        // --- Replaced manual loop with Stream API ---
        List<Book> duplicateBooks = cart.stream()
                .filter(b -> alreadySet.contains(b.getId()))
                .collect(Collectors.toList());

        List<Book> toSave = cart.stream()
                .filter(b -> !alreadySet.contains(b.getId()))
                .collect(Collectors.toList());

        String alreadyMsg = duplicateBooks.stream()
                .map(b -> "- " + b.getTitle() + (b.getAuthorName() != null && !b.getAuthorName().isEmpty() ? " (" + b.getAuthorName() + ")" : ""))
                .collect(Collectors.joining("\n"));

        if (!alreadyMsg.isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Một số sách đã mượn");
            info.setHeaderText("Bạn đã mượn những cuốn sau trước đó. Chúng sẽ được bỏ qua:");
            info.setContentText(alreadyMsg);
            info.showAndWait();
        }

        if (toSave.isEmpty()) {
            Alert none = new Alert(Alert.AlertType.INFORMATION);
            none.setTitle("Không có sách để mượn");
            none.setHeaderText(null);
            none.setContentText("Không còn sách nào để mượn (tất cả đều đã được mượn trước đó).");
            none.showAndWait();
            return;
        }

        // Build confirmation content using Stream join
        String listContent = toSave.stream()
                .map(b -> "- " + b.getTitle() + (b.getAuthorName() != null && !b.getAuthorName().isEmpty() ? " (" + b.getAuthorName() + ")" : ""))
                .collect(Collectors.joining("\n"));

        StringBuilder content = new StringBuilder();
        content.append("Bạn có chắc muốn mượn ").append(toSave.size()).append(" sách đã chọn?\n\n");
        content.append("Danh sách sách:\n");
        content.append(listContent);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận mượn");
        confirm.setHeaderText(null);
        confirm.setContentText(content.toString());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (currentUser == null) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Lỗi");
                err.setHeaderText(null);
                err.setContentText("Không có người dùng hợp lệ để thực hiện mượn.");
                err.showAndWait();
                return;
            }

            boolean saved = loanDAO.addLoans(currentUser.getId(), toSave);
            if (saved) {
                // bulk update borrowedBooks and remove from cart using streams
                List<Book> newBorrowed = toSave.stream()
                        .filter(b -> !borrowedBooks.contains(b))
                        .collect(Collectors.toList());
                borrowedBooks.addAll(newBorrowed);
                cart.removeAll(toSave);

                updateCartUI();
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Thành công");
                success.setHeaderText(null);
                success.setContentText("Đã mượn sách thành công và lưu vào cơ sở dữ liệu.");
                success.showAndWait();
            } else {
                Alert fail = new Alert(Alert.AlertType.ERROR);
                fail.setTitle("Lỗi");
                fail.setHeaderText(null);
                fail.setContentText("Lưu thông tin mượn thất bại. Vui lòng thử lại sau.");
                fail.showAndWait();
            }
        } else {
            // User cancelled
        }
    }

    @FXML
    private void handleViewBorrowedClicked(ActionEvent event) {
        if (viewBorrowedButton == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Không thể mở màn hình sách đã mượn.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        try {
            previousScene = viewBorrowedButton.getScene(); // Store scene before switching
            Stage stage = SceneHelper.getStage(viewBorrowedButton);
            SceneHelper.switchScene(stage, "/ui/BorrowedBooks.fxml", "Sách đã mượn", controller -> {
                if (controller instanceof BorrowedBooksController) {
                    ((BorrowedBooksController) controller).initData(currentUser, previousScene);
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR, "Không thể mở màn hình sách đã mượn: " + ex.getMessage(), ButtonType.OK);
            err.showAndWait();
        }
    }

    @FXML
    private void handleLogoutClicked(ActionEvent event) {
        try {
            Stage stage = SceneHelper.getStage(cartButton);
            SceneHelper.switchScene(stage, "/ui/LoginView.fxml", "Login", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}