package models;

public class Author {
    // minimal model used for biography handling
    private Integer id;
    private String name;
    private String biography;

    public Author(Integer id, String name, String biography) {
        this.id = id;
        this.name = name;
        this.biography = biography;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getBiography() { return biography; }
}
