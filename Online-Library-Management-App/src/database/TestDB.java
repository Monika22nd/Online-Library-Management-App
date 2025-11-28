/*package database;

import java.util.*;

public class TestDB {
    private static final TestDB instance = new TestDB();
    private HashMap<String, String> mp = new HashMap<>();

    public TestDB(){
        mp = new HashMap<>();
        mp.put("Alice", "p4a");
        mp.put("Bob", "p4b");
        mp.put("Candice", "p4c");
    }

    public static TestDB getInstance(){
        return instance;
    }

    public boolean checkLogin(String username, String password){
        String dbPassword = mp.get(username);
        return dbPassword != null && dbPassword.equals(password);
    }

    public boolean checkUserExists(String username){
        return mp.containsKey((username));
    }

    public void registerUser(String username, String password){
        if (mp.containsKey(username)){
            return;
        }
        mp.put(username, password);
    }
}
*/