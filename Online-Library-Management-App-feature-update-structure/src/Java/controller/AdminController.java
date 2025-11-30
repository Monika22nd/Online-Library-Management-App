package Java.controller;

import Java.database.UserDAO;
import Java.database.BookDAO;
import Java.database.LoanDAO;
import Java.models.User;
import Java.models.Book;
import Java.models.Role;
import Java.models.Loan;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // Cần thiết để load lại Home
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public class AdminController {

    @FXML private TableView<User> userTable;
    @FXML private TableView<Book> bookTable;
    @FXML private TableView<Loan> loanTable;

    private LoanDAO loanDAO = new LoanDAO();
    private UserDAO userDAO = new UserDAO();
    private BookDAO bookDAO = new BookDAO();

    private User currentAdmin;

    public void initData(User admin) {
        this.currentAdmin = admin;
        loadUsers();
        loadBooks();
        loadLoans();
    }

    // ---------------- Users (GIỮ NGUYÊN) ----------------
    private void loadUsers() {
        List<User> users = userDAO.getAllUsers();
        ObservableList<User> obs = FXCollections.observableArrayList(users);
        userTable.setItems(obs);

        if (userTable.getColumns().isEmpty()) {
            TableColumn<User, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
            TableColumn<User, String> nameCol = new TableColumn<>("Name");
            nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
            TableColumn<User, String> usernameCol = new TableColumn<>("Username");
            usernameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("username"));
            TableColumn<User, String> emailCol = new TableColumn<>("Email");
            emailCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("email"));
            TableColumn<User, String> phoneCol = new TableColumn<>("Phone");
            phoneCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("phone"));
            TableColumn<User, Role> roleCol = new TableColumn<>("Role");
            roleCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("role"));
            userTable.getColumns().addAll(idCol, nameCol, usernameCol, emailCol, phoneCol, roleCol);
        }
    }

    @FXML
    private void handleAddUser() {
        User u = showUserDialog(null);
        if (u != null) {
            boolean ok = userDAO.registerUser(u);
            if (ok) loadUsers();
            else showAlert("Lỗi", "Không thể thêm user.");
        }
    }

    @FXML
    private void handleEditUser() {
        User sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Thông báo", "Chọn user để sửa."); return; }
        User updated = showUserDialog(sel);
        if (updated != null) {
            boolean ok = userDAO.updateUser(updated);
            if (ok) loadUsers();
            else showAlert("Lỗi", "Cập nhật thất bại.");
        }
    }

    @FXML
    private void handleDeleteUser() {
        User sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Thông báo", "Chọn user để xóa."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa user " + sel.getUsername() + "?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean ok = userDAO.deleteUser(sel.getId());
            if (ok) loadUsers();
            else showAlert("Lỗi", "Xóa thất bại.");
        }
    }

    private User showUserDialog(User existing) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add User" : "Edit User");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setVgap(8);
        grid.setHgap(10);
        TextField nameField = new TextField();
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        ChoiceBox<Role> roleChoice = new ChoiceBox<>(FXCollections.observableArrayList(Role.ADMIN, Role.CLIENT));
        if (existing != null) {
            nameField.setText(existing.getName());
            usernameField.setText(existing.getUsername());
            passwordField.setText(existing.getPassword());
            emailField.setText(existing.getEmail());
            phoneField.setText(existing.getPhone());
            roleChoice.setValue(existing.getRole());
        } else {
            roleChoice.setValue(Role.CLIENT);
        }

        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Username:"), usernameField);
        grid.addRow(2, new Label("Password:"), passwordField);
        grid.addRow(3, new Label("Email:"), emailField);
        grid.addRow(4, new Label("Phone:"), phoneField);
        grid.addRow(5, new Label("Role:"), roleChoice);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                if (existing != null) {
                    return new User(existing.getId(), nameField.getText(), usernameField.getText(), passwordField.getText(), emailField.getText(), phoneField.getText(), roleChoice.getValue());
                } else {
                    return new User(nameField.getText(), usernameField.getText(), passwordField.getText(), emailField.getText(), phoneField.getText(), roleChoice.getValue());
                }
            }
            return null;
        });

        Optional<User> res = dialog.showAndWait();
        return res.orElse(null);
    }

    // ---------------- Books (GIỮ NGUYÊN) ----------------
    private void loadBooks() {
        List<Book> books = bookDAO.getAllBooks();
        ObservableList<Book> obs = FXCollections.observableArrayList(books);
        bookTable.setItems(obs);

        if (bookTable.getColumns().isEmpty()) {
            TableColumn<Book, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
            TableColumn<Book, String> titleCol = new TableColumn<>("Title");
            titleCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
            TableColumn<Book, String> authorCol = new TableColumn<>("Author");
            authorCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("authorName"));
            TableColumn<Book, Integer> copiesCol = new TableColumn<>("Copies");
            copiesCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("copiesAvailable"));
            TableColumn<Book, Double> priceCol = new TableColumn<>("Price");
            priceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
            bookTable.getColumns().addAll(idCol, titleCol, authorCol, copiesCol, priceCol);
        }
    }

    @FXML
    private void handleAddBook() {
        Book b = showBookDialog(null);
        if (b != null) {
            boolean ok = bookDAO.addBook(b);
            if (ok) loadBooks();
            else showAlert("Lỗi", "Không thể thêm sách.");
        }
    }

    @FXML
    private void handleEditBook() {
        Book sel = bookTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Thông báo", "Chọn sách để sửa."); return; }
        Book updated = showBookDialog(sel);
        if (updated != null) {
            boolean ok = bookDAO.updateBook(updated);
            if (ok) loadBooks();
            else showAlert("Lỗi", "Cập nhật thất bại.");
        }
    }

    @FXML
    private void handleDeleteBook() {
        Book sel = bookTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Thông báo", "Chọn sách để xóa."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa sách " + sel.getTitle() + "?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean ok = bookDAO.deleteBook(sel.getId());
            if (ok) loadBooks();
            else showAlert("Lỗi", "Xóa thất bại.");
        }
    }

    private Book showBookDialog(Book existing) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Book" : "Edit Book");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setVgap(8);
        grid.setHgap(10);
        TextField isbnField = new TextField();
        TextField titleField = new TextField();
        TextField authorIdField = new TextField();
        TextField authorNameField = new TextField();
        TextField genreField = new TextField();
        TextField priceField = new TextField();
        TextField copiesField = new TextField();
        TextField imagePathField = new TextField();
        imagePathField.setPromptText("/img/ten_anh.jpg");
        TextArea descField = new TextArea();
        descField.setPrefRowCount(3);

        if (existing != null) {
            isbnField.setText(existing.getIsbn());
            titleField.setText(existing.getTitle());
            authorIdField.setText(String.valueOf(existing.getAuthorId()));
            authorNameField.setText(existing.getAuthorName());
            genreField.setText(existing.getGenre());
            priceField.setText(String.valueOf(existing.getPrice()));
            copiesField.setText(String.valueOf(existing.getCopiesAvailable()));
            imagePathField.setText(existing.getImagePath());
            descField.setText(existing.getDescription());
        }

        grid.addRow(0, new Label("ISBN:"), isbnField);
        grid.addRow(1, new Label("Title:"), titleField);
        grid.addRow(2, new Label("Author ID:"), authorIdField);
        grid.addRow(3, new Label("Author Name:"), authorNameField);
        grid.addRow(4, new Label("Genre:"), genreField);
        grid.addRow(5, new Label("Price:"), priceField);
        grid.addRow(6, new Label("Copies:"), copiesField);
        grid.addRow(7, new Label("Image Path:"), imagePathField);
        grid.addRow(8, new Label("Description:"), descField);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                int authorId = 0;
                try { authorId = Integer.parseInt(authorIdField.getText().trim()); } catch (Exception ex) { authorId = 0; }
                double price = 0;
                try { price = Double.parseDouble(priceField.getText().trim()); } catch (Exception ex) { price = 0; }
                int copies = 0;
                try { copies = Integer.parseInt(copiesField.getText().trim()); } catch (Exception ex) { copies = 0; }
                String imgPath = imagePathField.getText().trim();

                if (existing != null) {
                    return new Book(existing.getId(), isbnField.getText(), titleField.getText(), authorId, authorNameField.getText(), genreField.getText(), price, copies, descField.getText(), imgPath);
                } else {
                    return new Book(0, isbnField.getText(), titleField.getText(), authorId, authorNameField.getText(), genreField.getText(), price, copies, descField.getText(), imgPath);
                }
            }
            return null;
        });

        Optional<Book> res = dialog.showAndWait();
        return res.orElse(null);
    }

    // ---------------- LOANS (CẬP NHẬT MỚI) ----------------

    @FXML
    private void handleRefreshLoans() {
        loadLoans();
    }

    private void loadLoans() {
        List<Loan> loans = loanDAO.getAllLoans();
        ObservableList<Loan> obs = FXCollections.observableArrayList(loans);
        loanTable.setItems(obs);

        if (loanTable.getColumns().isEmpty()) {
            TableColumn<Loan, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
            idCol.setPrefWidth(50);

            TableColumn<Loan, String> userCol = new TableColumn<>("Người mượn");
            userCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("userName"));

            TableColumn<Loan, String> bookCol = new TableColumn<>("Sách");
            bookCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("bookTitle"));

            TableColumn<Loan, Date> borrowCol = new TableColumn<>("Ngày mượn");
            borrowCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("borrowDate"));

            TableColumn<Loan, Date> dueCol = new TableColumn<>("Hạn trả");
            dueCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dueDate"));

            // CỘT TRẠNG THÁI (CÓ TÔ MÀU)
            TableColumn<Loan, String> statusCol = new TableColumn<>("Trạng thái");
            statusCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
            statusCol.setCellFactory(column -> new TableCell<Loan, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        // Logic tô màu
                        if (item.equals("PENDING")) {
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        } else if (item.equals("BORROWED")) {
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else if (item.equals("RETURNED")) {
                            setStyle("-fx-text-fill: blue;");
                        } else if (item.equals("REJECTED")) {
                            setStyle("-fx-text-fill: red;");
                        }
                    }
                }
            });

            loanTable.getColumns().addAll(idCol, userCol, bookCol, borrowCol, dueCol, statusCol);
        }
    }

    // --- 3 HÀM XỬ LÝ MỚI ---

    @FXML
    private void handleApproveLoan() {
        Loan sel = loanTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Lỗi", "Vui lòng chọn phiếu để duyệt."); return; }

        // Kiểm tra status phải là PENDING
        if (!"PENDING".equals(sel.getStatus())) {
            showAlert("Thông báo", "Chỉ có thể duyệt các phiếu đang chờ (PENDING).");
            return;
        }

        // Gọi DAO để duyệt
        String result = loanDAO.approveLoan(sel.getId());
        if ("SUCCESS".equals(result)) {
            showAlert("Thành công", "Đã duyệt phiếu mượn thành công!");
            loadLoans(); // Refresh bảng loan
            loadBooks(); // Refresh bảng sách (để thấy số lượng tồn kho giảm)
        } else {
            showAlert("Thất bại", "Lỗi: " + result);
        }
    }

    @FXML
    private void handleRejectLoan() {
        Loan sel = loanTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Lỗi", "Vui lòng chọn phiếu để từ chối."); return; }

        if (!"PENDING".equals(sel.getStatus())) {
            showAlert("Thông báo", "Chỉ có thể từ chối các phiếu đang chờ (PENDING).");
            return;
        }

        boolean ok = loanDAO.rejectLoan(sel.getId());
        if (ok) {
            showAlert("Thành công", "Đã từ chối phiếu mượn.");
            loadLoans();
        } else {
            showAlert("Thất bại", "Có lỗi xảy ra khi từ chối.");
        }
    }

    @FXML
    private void handleReturnBook() {
        Loan sel = loanTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Lỗi", "Vui lòng chọn phiếu để trả sách."); return; }

        if (!"BORROWED".equals(sel.getStatus())) {
            showAlert("Thông báo", "Chỉ có thể trả sách đang mượn (BORROWED).");
            return;
        }

        boolean ok = loanDAO.adminConfirmReturn(sel.getId());
        if (ok) {
            showAlert("Thành công", "Đã xác nhận trả sách thành công!");
            loadLoans();
            loadBooks(); // Refresh bảng sách (số lượng tăng lên)
        } else {
            showAlert("Thất bại", "Có lỗi xảy ra khi trả sách.");
        }
    }

    // ---------------- Navigation & Helpers ----------------

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) userTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/ui/UserHomeScreen.fxml"));
            Parent root = loader.load();
            Java.controller.HomescreenController hc = loader.getController();
            hc.initData(currentAdmin);
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Lỗi", "Không thể quay lại: " + ex.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}