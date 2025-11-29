package controller;

import database.LoanDAO;
import database.BookDAO;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.sql.Date;

public class HomescreenController {

    @FXML private HBox topHBox; // injected from FXML (new)
    @FXML private Button homeButton;    // Dùng để lấy Stage
    @FXML private Label welcomeLabel;   // Hiển thị tên User
    @FXML private Button cartButton;    // Nút giỏ hàng
    @FXML private Button viewBorrowedButton; // Nút xem sách đã mượn

    // Container chứa danh sách sách (Quan trọng: Cần fx:id trong UserHomeScreen.fxml)
    @FXML private TilePane bookContainer;

    private User currentUser;
    private BookDAO bookDAO = new BookDAO();
    private LoanDAO loanDAO = new LoanDAO(); // persist loans and query existing
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

        // Add admin button dynamically if user is ADMIN
        if (currentUser != null && currentUser.getRole() == Role.ADMIN && topHBox != null) {
            Button adminBtn = new Button("Admin Panel");
            adminBtn.setOnAction(e -> openAdminPanel());
            // add before viewBorrowedButton (keep layout)
            topHBox.getChildren().add(topHBox.getChildren().size() - 2, adminBtn);
        }

        updateCartUI(); // Reset số hiển thị về 0
        loadBooks();    // Bắt đầu tải sách
    }

    private void openAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AdminPanel.fxml"));
            Parent root = loader.load();

            // Pass current user to admin controller (optional)
            controller.AdminController adminCtrl = loader.getController();
            adminCtrl.initData(currentUser);

            Stage stage = (Stage) cartButton.getScene().getWindow();
            stage.setTitle("Admin Panel");
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.centerOnScreen();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR, "Không thể mở Admin Panel: " + ex.getMessage(), ButtonType.OK);
            err.showAndWait();
        }
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

        // Check which selected books are already borrowed by this user
        List<Integer> alreadyIds = loanDAO.getAlreadyBorrowedBookIds(currentUser != null ? currentUser.getId() : -1, cart);
        Set<Integer> alreadySet = new HashSet<>(alreadyIds);

        // If some books are already borrowed, inform user and remove them from the to-save list
        List<Book> toSave = new ArrayList<>();
        StringBuilder alreadyMsg = new StringBuilder();
        for (Book b : cart) {
            if (alreadySet.contains(b.getId())) {
                try {
                    alreadyMsg.append("- ").append(b.getTitle()).append(" (").append(b.getAuthorName()).append(")\n");
                } catch (Exception ex) {
                    alreadyMsg.append("- ").append(b.getTitle()).append("\n");
                }
            } else {
                toSave.add(b);
            }
        }

        if (!alreadyMsg.toString().isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Một số sách đã mượn");
            info.setHeaderText("Bạn đã mượn những cuốn sau trước đó. Chúng sẽ được bỏ qua:");
            info.setContentText(alreadyMsg.toString());
            info.showAndWait();
        }

        if (toSave.isEmpty()) {
            // Nothing left to borrow after removing duplicates
            Alert none = new Alert(Alert.AlertType.INFORMATION);
            none.setTitle("Không có sách để mượn");
            none.setHeaderText(null);
            none.setContentText("Không còn sách nào để mượn (tất cả đều đã được mượn trước đó).");
            none.showAndWait();
            return;
        }

        // Build detailed list of books for confirmation content using only toSave
        StringBuilder content = new StringBuilder();
        content.append("Bạn có chắc muốn mượn ").append(toSave.size()).append(" sách đã chọn?\n\n");
        content.append("Danh sách sách:\n");
        for (Book b : toSave) {
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
            // Ensure we have a logged-in user
            if (currentUser == null) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Lỗi");
                err.setHeaderText(null);
                err.setContentText("Không có người dùng hợp lệ để thực hiện mượn.");
                err.showAndWait();
                return;
            }

            // Persist to database via LoanDAO for only toSave list
            boolean saved = loanDAO.addLoans(currentUser.getId(), toSave);
            if (saved) {
                // Move saved items from cart into borrowedBooks and remove them from cart
                for (Book b : toSave) {
                    if (!borrowedBooks.contains(b)) {
                        borrowedBooks.add(b);
                    }
                    cart.remove(b);
                }
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
        if (currentUser == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setContentText("Không có người dùng hợp lệ.");
            alert.showAndWait();
            return;
        }

        // 1. Fetch loans
        List<Loan> loanList = loanDAO.getBorrowedLoansByUser(currentUser.getId());

        if (loanList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setContentText("Bạn chưa mượn cuốn sách nào.");
            alert.showAndWait();
            return;
        }

        // 2. Build UI
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        Label titleLbl = new Label("Sách đã mượn (" + loanList.size() + ")");

        ListView<HBox> listView = new ListView<>();
        VBox.setVgrow(listView, Priority.ALWAYS);

        // populate rows: title, clickable author, due date
        ObservableList<HBox> rows = FXCollections.observableArrayList();
        for (Loan ln : loanList) {
            Book b = bookDAO.getBookById(ln.getBookId());
            String title = b != null ? b.getTitle() : ("Book ID " + ln.getBookId());
            String authorName = (b != null && b.getAuthorName() != null) ? b.getAuthorName() : "";

            Label titleLabel = new Label(title);
            Hyperlink authorLink = new Hyperlink(authorName);
            Label dueLabel = new Label(" Due: " + (ln.getDueDate() != null ? ln.getDueDate().toString() : "N/A"));

            // author click -> show biography (blank if null)
            final Book bookRef = b;
            authorLink.setOnAction(ev -> {
                String bio = "";
                if (bookRef != null) {
                    Author author = bookDAO.getAuthorById(bookRef.getAuthorId());
                    if (author != null && author.getBiography() != null) bio = author.getBiography();
                    // update header to author's name when available
                    String hdr = (author != null && author.getName() != null && !author.getName().isEmpty()) ? author.getName() : (authorName == null || authorName.isEmpty() ? "Author" : authorName);
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Author Biography");
                    a.setHeaderText(hdr);
                    a.setContentText(bio);
                    a.showAndWait();
                    return;
                }
                // fallback: show empty biography
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Author Biography");
                a.setHeaderText(authorName == null || authorName.isEmpty() ? "Author" : authorName);
                a.setContentText("");
                a.showAndWait();
            });

            HBox row = new HBox(10, titleLabel, authorLink, dueLabel);
            rows.add(row);
        }
        listView.setItems(rows);

        // display HBox nodes properly
        listView.setCellFactory(lv -> new ListCell<HBox>() {
            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : item);
            }
        });

        // Buttons
        Button returnBtn = new Button("Return selected");
        returnBtn.setDisable(true);
        Button backButton = new Button("Quay lại");

        // enable return when selection exists
        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            returnBtn.setDisable(newV == null || newV.intValue() < 0);
        });

        // Return action uses the loanList index mapping
        returnBtn.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= loanList.size()) return;
            Loan selLoan = loanList.get(idx);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xác nhận trả sách?", ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> res = confirm.showAndWait();
            if (!(res.isPresent() && res.get() == ButtonType.OK)) return;

            boolean ok = loanDAO.returnBook(selLoan.getId());
            if (ok) {
                // show on-time/overdue info
                Date now = new Date(System.currentTimeMillis());
                Date due = selLoan.getDueDate();
                String msg = (due != null && !now.after(due)) ? "Đã trả sách. Trả đúng hạn." : "Đã trả sách. Quá hạn.";
                Alert info = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
                info.showAndWait();

                // refresh loanList and UI
                List<Loan> refreshed = loanDAO.getBorrowedLoansByUser(currentUser.getId());
                loanList.clear();
                loanList.addAll(refreshed);

                // rebuild rows
                ObservableList<HBox> newRows = FXCollections.observableArrayList();
                for (Loan ln : loanList) {
                    Book b = bookDAO.getBookById(ln.getBookId());
                    String title = b != null ? b.getTitle() : ("Book ID " + ln.getBookId());
                    String authorName = (b != null && b.getAuthorName() != null) ? b.getAuthorName() : "";
                    Label titleLabel = new Label(title);
                    Hyperlink authorLink = new Hyperlink(authorName);
                    Label dueLabel = new Label(" Due: " + (ln.getDueDate() != null ? ln.getDueDate().toString() : "N/A"));
                    final Book bookRef = b;
                    authorLink.setOnAction(ev -> {
                        String bio = "";
                        if (bookRef != null) {
                            Author author = bookDAO.getAuthorById(bookRef.getAuthorId());
                            if (author != null && author.getBiography() != null) bio = author.getBiography();
                            String hdr = (author != null && author.getName() != null && !author.getName().isEmpty()) ? author.getName() : (authorName == null || authorName.isEmpty() ? "Author" : authorName);
                            Alert a = new Alert(Alert.AlertType.INFORMATION);
                            a.setTitle("Author Biography");
                            a.setHeaderText(hdr);
                            a.setContentText(bio);
                            a.showAndWait();
                            return;
                        }
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setTitle("Author Biography");
                        a.setHeaderText(authorName == null || authorName.isEmpty() ? "Author" : authorName);
                        a.setContentText("");
                        a.showAndWait();
                    });
                    newRows.add(new HBox(10, titleLabel, authorLink, dueLabel));
                }
                listView.setItems(newRows);
                titleLbl.setText("Sách đã mượn (" + loanList.size() + ")");
                returnBtn.setDisable(true);
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "Trả sách thất bại.", ButtonType.OK);
                err.showAndWait();
            }
        });

        backButton.setOnAction(ev -> {
            if (previousScene != null) {
                Stage currentStage = (Stage) backButton.getScene().getWindow();
                currentStage.setScene(previousScene);
            }
        });

        HBox actions = new HBox(8, returnBtn, backButton);
        root.getChildren().addAll(titleLbl, listView, actions);

        // show scene
        Stage stage = (Stage) viewBorrowedButton.getScene().getWindow();
        previousScene = viewBorrowedButton.getScene();
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