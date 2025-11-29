package Java.controller;

import Java.database.BookDAO;
import Java.database.LoanDAO;
import Java.models.Author;
import Java.models.Book;
import Java.models.Loan;
import Java.models.User;
import Java.util.SceneHelper; // Sử dụng SceneHelper vừa tạo
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BorrowedBooksController {

    @FXML private Label titleLabel;
    @FXML private ListView<HBox> listView;
    @FXML private Button returnBtn;
    @FXML private Button backBtn;

    private User currentUser;
    private javafx.scene.Scene previousScene;
    private BookDAO bookDAO = new BookDAO();
    private LoanDAO loanDAO = new LoanDAO();
    private List<Loan> loanList = new ArrayList<>();

    public void initData(User user, javafx.scene.Scene previousScene) {
        this.currentUser = user;
        this.previousScene = previousScene;
        loadLoans();
    }

    private void loadLoans() {
        if (currentUser == null) return;
        loanList = loanDAO.getBorrowedLoansByUser(currentUser.getId());
        titleLabel.setText("Sách đã mượn (" + loanList.size() + ")");

        List<HBox> rows = loanList.stream().map(ln -> {
            Book b = bookDAO.getBookById(ln.getBookId());
            String title = b != null ? b.getTitle() : ("Book ID " + ln.getBookId());
            String authorName = (b != null && b.getAuthorName() != null) ? b.getAuthorName() : "";

            Label titleLabel = new Label(title + " - ");
            Hyperlink authorLink = new Hyperlink(authorName);
            Label dueLabel = new Label(" (Hạn: " + (ln.getDueDate() != null ? ln.getDueDate().toString() : "N/A") + ")");

            // Xử lý bấm vào tên tác giả -> Hiện tiểu sử
            final Book bookRef = b;
            authorLink.setOnAction(ev -> {
                String bio = "Không có thông tin.";
                if (bookRef != null) {
                    Author author = bookDAO.getAuthorById(bookRef.getAuthorId());
                    if (author != null && author.getBiography() != null) bio = author.getBiography();
                }
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Tiểu sử tác giả");
                a.setHeaderText("Tác giả: " + authorName);
                a.setContentText(bio);
                a.showAndWait();
            });

            return new HBox(5, titleLabel, authorLink, dueLabel);
        }).collect(Collectors.toList());

        ObservableList<HBox> obsRows = FXCollections.observableArrayList(rows);
        listView.setItems(obsRows);

        // Chỉ cho phép trả sách khi chọn 1 dòng
        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            returnBtn.setDisable(newV == null || newV.intValue() < 0);
        });

        // Xử lý trả sách
        returnBtn.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= loanList.size()) return;
            Loan selLoan = loanList.get(idx);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xác nhận trả sách?", ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                boolean ok = loanDAO.returnBook(selLoan.getId());
                if (ok) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION, "Đã trả sách thành công!", ButtonType.OK);
                    info.showAndWait();
                    loadLoans(); // Reload lại danh sách
                } else {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Trả sách thất bại.", ButtonType.OK);
                    err.showAndWait();
                }
            }
        });

        backBtn.setOnAction(ev -> {
            if (previousScene != null) {
                Stage currentStage = SceneHelper.getStage(backBtn);
                currentStage.setScene(previousScene);
            }
        });
    }
}