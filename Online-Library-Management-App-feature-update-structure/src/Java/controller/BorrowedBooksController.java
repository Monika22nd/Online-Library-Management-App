package Java.controller;

import Java.database.BookDAO;
import Java.database.LoanDAO;
import Java.models.Book;
import Java.models.Loan;
import Java.models.User;
import Java.util.SceneHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class BorrowedBooksController {

    @FXML private Label titleLabel;
    @FXML private ListView<HBox> listView;
    @FXML private Button returnBtn; // Nút này sẽ bị ẩn hoặc đổi tác dụng
    @FXML private Button backBtn;

    private User currentUser;
    private javafx.scene.Scene previousScene;
    private BookDAO bookDAO = new BookDAO();
    private LoanDAO loanDAO = new LoanDAO();

    public void initData(User user, javafx.scene.Scene previousScene) {
        this.currentUser = user;
        this.previousScene = previousScene;

        // Ẩn nút trả sách đối với User thường
        returnBtn.setVisible(false);

        loadLoans();
    }

    private void loadLoans() {
        if (currentUser == null) return;
        List<Loan> loanList = loanDAO.getActiveLoansByUser(currentUser.getId());
        titleLabel.setText("Sách đang mượn / Chờ duyệt (" + loanList.size() + ")");

        List<HBox> rows = loanList.stream().map(ln -> {
            String status = ln.getStatus();
            String statusText = status.equals("PENDING") ? "⏳ Chờ duyệt" : "✅ Đang mượn";
            Color statusColor = status.equals("PENDING") ? Color.ORANGE : Color.GREEN;

            Label titleLabel = new Label(ln.getBookTitle() + " - ");
            Label statusLabel = new Label(statusText);
            statusLabel.setTextFill(statusColor);
            statusLabel.setStyle("-fx-font-weight: bold;");

            Label dateLabel = new Label(" (Ngày mượn: " + ln.getBorrowDate() + ")");

            return new HBox(5, titleLabel, statusLabel, dateLabel);
        }).collect(Collectors.toList());

        listView.setItems(FXCollections.observableArrayList(rows));

        backBtn.setOnAction(ev -> {
            if (previousScene != null) {
                Stage currentStage = SceneHelper.getStage(backBtn);
                currentStage.setScene(previousScene);
            }
        });
    }
}