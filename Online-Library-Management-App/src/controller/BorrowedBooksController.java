package controller;

import database.BookDAO;
import database.LoanDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import models.Author;
import models.Book;
import models.Loan;
import models.User;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // Called by HomescreenController after loading the FXML
    public void initData(User user, javafx.scene.Scene previousScene) {
        this.currentUser = user;
        this.previousScene = previousScene;
        loadLoans();
    }

    private void loadLoans() {
        if (currentUser == null) return;
        loanList = loanDAO.getBorrowedLoansByUser(currentUser.getId());
        titleLabel.setText("Sách đã mượn (" + loanList.size() + ")");

        ObservableList<HBox> rows = FXCollections.observableArrayList();
        for (Loan ln : loanList) {
            Book b = bookDAO.getBookById(ln.getBookId());
            String title = b != null ? b.getTitle() : ("Book ID " + ln.getBookId());
            String authorName = (b != null && b.getAuthorName() != null) ? b.getAuthorName() : "";

            Label titleLabel = new Label(title);
            Hyperlink authorLink = new Hyperlink(authorName);
            Label dueLabel = new Label(" Due: " + (ln.getDueDate() != null ? ln.getDueDate().toString() : "N/A"));

            final Book bookRef = b;
            final String authorLbl = authorName;
            authorLink.setOnAction(ev -> {
                String bio = "";
                if (bookRef != null) {
                    Author author = bookDAO.getAuthorById(bookRef.getAuthorId());
                    if (author != null && author.getBiography() != null) bio = author.getBiography();
                    String hdr = (author != null && author.getName() != null && !author.getName().isEmpty()) ? author.getName() : (authorLbl == null || authorLbl.isEmpty() ? "Author" : authorLbl);
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Author Biography");
                    a.setHeaderText(hdr);
                    a.setContentText(bio);
                    a.showAndWait();
                    return;
                }
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Author Biography");
                a.setHeaderText(authorLbl == null || authorLbl.isEmpty() ? "Author" : authorLbl);
                a.setContentText("");
                a.showAndWait();
            });

            rows.add(new HBox(10, titleLabel, authorLink, dueLabel));
        }

        listView.setItems(rows);
        listView.setCellFactory(lv -> new javafx.scene.control.ListCell<HBox>() {
            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : item);
            }
        });

        // selection -> enable return button
        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            returnBtn.setDisable(newV == null || newV.intValue() < 0);
        });

        // return action
        returnBtn.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= loanList.size()) return;
            Loan selLoan = loanList.get(idx);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xác nhận trả sách?", ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> res = confirm.showAndWait();
            if (!(res.isPresent() && res.get() == ButtonType.OK)) return;

            boolean ok = loanDAO.returnBook(selLoan.getId());
            if (ok) {
                Date now = new Date(System.currentTimeMillis());
                Date due = selLoan.getDueDate();
                String msg = (due != null && !now.after(due)) ? "Đã trả sách. Trả đúng hạn." : "Đã trả sách. Quá hạn.";
                Alert info = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
                info.showAndWait();
                loadLoans(); // refresh list & title
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "Trả sách thất bại.", ButtonType.OK);
                err.showAndWait();
            }
        });

        backBtn.setOnAction(ev -> {
            if (previousScene != null) {
                Stage currentStage = (Stage) backBtn.getScene().getWindow();
                currentStage.setScene(previousScene);
            }
        });
    }
}
