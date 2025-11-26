/*
package models;

import java.util.*;

public class Admin {
    // Attributes
    private static final String username = "admin";
    private static final String password = "1";
    private static final String email = "admin@gmail.com";
    private static final String phone = "0987654321";

    // Getters
    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    // Methods
    //Create user
    public void createUser(Client user) {

    }
    // Delete user
    public void deleteUser(String userId) {

    }
    //Update user
    public void updateUser(Client user) {

    }
    //Reset password
    public void resetPassword(String userId) {

    }
    //Add book
    public void addBook(Book book) {

    }
    //Remove book
    public void removeBook(String isbn) {

    }
    //Update Book
    public void updateBook(Book book) {

    }

    public boolean checkAdminLogin(String password){
        if (password == null){
            return false;
        }
        return Admin.password.equals(password);
    }
}
/*
 */
