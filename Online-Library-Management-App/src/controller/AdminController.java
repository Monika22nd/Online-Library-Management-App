package controller;

import database.UserDAO;
import database.BookDAO;
import models.User;
import models.Book;
import models.Role;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import util.SceneHelper; // NEW

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AdminController {

    @FXML private TableView<User> userTable;
    @FXML private TableView<Book> bookTable;

    private UserDAO userDAO = new UserDAO();
    private BookDAO bookDAO = new BookDAO();

    private User currentAdmin;

    public void initData(User admin) {
        this.currentAdmin = admin;
        loadUsers();
        loadBooks();
    }

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

    // ---------------- Users ----------------

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

    // Simple dialog for user add/edit
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

    // ---------------- Books ----------------

    @FXML
    private void handleAddBook() {
        Book b = showBookDialog(null);
        if (b != null) {
            boolean ok = bookDAO.addBook(b); // existing addBook might expect authorName — works for simple cases
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

    // Simple dialog for book add/edit
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
            descField.setText(existing.getDescription());
        }

        grid.addRow(0, new Label("ISBN:"), isbnField);
        grid.addRow(1, new Label("Title:"), titleField);
        grid.addRow(2, new Label("Author ID:"), authorIdField);
        grid.addRow(3, new Label("Author Name:"), authorNameField);
        grid.addRow(4, new Label("Genre:"), genreField);
        grid.addRow(5, new Label("Price:"), priceField);
        grid.addRow(6, new Label("Copies:"), copiesField);
        grid.addRow(7, new Label("Description:"), descField);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                int authorId = 0;
                try { authorId = Integer.parseInt(authorIdField.getText().trim()); } catch (Exception ex) { authorId = 0; }
                double price = 0;
                try { price = Double.parseDouble(priceField.getText().trim()); } catch (Exception ex) { price = 0; }
                int copies = 0;
                try { copies = Integer.parseInt(copiesField.getText().trim()); } catch (Exception ex) { copies = 0; }

                if (existing != null) {
                    return new Book(existing.getId(), isbnField.getText(), titleField.getText(), authorId, authorNameField.getText(), genreField.getText(), price, copies, descField.getText());
                } else {
                    return new Book(0, isbnField.getText(), titleField.getText(), authorId, authorNameField.getText(), genreField.getText(), price, copies, descField.getText());
                }
            }
            return null;
        });

        Optional<Book> res = dialog.showAndWait();
        return res.orElse(null);
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = SceneHelper.getStage(userTable);
            SceneHelper.switchScene(stage, "/ui/UserHomeScreen.fxml", "Home", controller -> {
                if (controller instanceof HomescreenController) {
                    ((HomescreenController) controller).initData(currentAdmin);
                }
            });
        } catch (IOException ex) {
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
