package models;
import java.util.*;

public class Book {
    private String bookTitle;
    private String bookISBN; // book_id
    private String bookAuthor;
    private String bookPublicationDate;
    private int bookCopies;
    private String bookDesc;
    private double borrowPrice;
    private String bookGenre;
    private boolean isBorrowed;

    public Book(String bookTitle, String bookISBN, String bookAuthor, String bookPublicationDate, String bookDesc, String bookGenre){
        this.bookTitle = bookTitle;
        this.bookISBN = bookISBN;
        this.bookAuthor = bookAuthor;
        this.bookPublicationDate = bookPublicationDate;
        this.bookDesc = bookDesc;
        this.bookGenre = bookGenre;
    }

    // Getters
    public String getBookTitle() {
        return bookTitle;
    }

    public String getBookISBN() {
        return bookISBN;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public double getBorrowPrice() {
        return borrowPrice;
    }

    public String getBookDesc() {
        return bookDesc;
    }

    public String getBookPublicationDate() {
        return bookPublicationDate;
    }

    public String getBookGenre() {
        return bookGenre;
    }

    public int getBookCopies() {
        return bookCopies;
    }

    public boolean checkIsBorrowed() {
        return isBorrowed;
    }

    // Methods
    public void borrow(Client client){
        this.isBorrowed = true;
        this.bookCopies -= 1;
        client.getBooksBorrowed().add(this);
    }

    public void returnBook(Client client){
        this.isBorrowed = false;
        this.bookCopies += 1;
        client.getBooksBorrowed().remove(this);
    }
}
