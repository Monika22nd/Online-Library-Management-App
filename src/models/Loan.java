package models;
import java.sql.Date;

public class Loan {
    private int id;
    private int userId;
    private int bookId;
    private Date borrowDate;
    private Date dueDate;
    private Date returnDate;
    private String status; // "BORROWED" hoặc "RETURNED"

    private String bookTitle;
    private String userName;

    public Loan(int id, int userId, int bookId, Date borrowDate, Date dueDate, Date returnDate, String status) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    public Loan(int id, int userId, String userName, int bookId, String bookTitle, Date borrowDate, Date dueDate, Date returnDate, String status){
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getBookId() { return bookId; }
    public Date getBorrowDate() { return borrowDate; }
    public Date getDueDate() { return dueDate; }
    public Date getReturnDate() { return returnDate; }
    public String getStatus() { return status; }

    public String getBookTitle() { return bookTitle; }
    public String getUserName() { return userName; }
}