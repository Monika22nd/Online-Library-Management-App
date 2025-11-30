package Java.models;

public class Book {
    private int id;
    private String isbn;
    private String title;

    private int authorId;
    private String authorName;

    private String genre;
    private double price;
    private int copiesAvailable;
    private String description;
    private String imagePath;


    public Book(int id, String isbn, String title, int authorId, String authorName,
                String genre, double price, int copiesAvailable, String description, String imagePath) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.authorId = authorId;
        this.authorName = authorName;
        this.genre = genre;
        this.price = price;
        this.copiesAvailable = copiesAvailable;
        this.description = description;
        this.imagePath = imagePath;
    }

    public Book(int id, String isbn, String title, int authorId, String authorName,
                String genre, double price, int copiesAvailable, String description) {
        this(id, isbn, title, authorId, authorName, genre, price, copiesAvailable, description, null);
    }

    public int getId() { return id; }
    public String getIsbn() { return isbn; }

    public String getTitle() {
        return title != null ? title : "";
    }

    public int getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName != null ? authorName : "Unknown"; }

    public String getGenre() {
        return genre != null? genre : "";
    }

    public double getPrice() { return price; }
    public int getCopiesAvailable() { return copiesAvailable; }
    public String getDescription() { return description; }
    public String getImagePath() { return imagePath; }

    public boolean isAvailable() {
        return copiesAvailable > 0;
    }
}