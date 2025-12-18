// auth/UserManager.java
package com.chessplatform.auth;

import com.chessplatform.util.FileUtil;
import java.io.*;
import java.util.*;

public class UserManager {
    private static final String USERS_FILE = "data/users.dat";
    private Map<String, User> users;
    private User currentUser;
    
    public UserManager() {
        users = new HashMap<>();
        currentUser = null;
        loadUsers();
    }
    
    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (users.containsKey(username)) {
            return false;
        }
        
        User newUser = new User(username.trim(), password);
        users.put(username.trim(), newUser);
        saveUsers();
        return true;
    }
    
    public boolean login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.verifyPassword(password)) {
            currentUser = user;
            return true;
        }
        return false;
    }
    
    public void logout() {
        saveUsers();
        currentUser = null;
    }
    
    public User getCurrentUser() {
        if (currentUser == null) {
            return User.createGuestUser();
        }
        return currentUser;
    }
    
    public User getUser(String username) {
        return users.get(username);
    }
    
    public boolean isUsernameTaken(String username) {
        return users.containsKey(username);
    }
    
    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            users = (Map<String, User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("加载用户数据失败: " + e.getMessage());
            users = new HashMap<>();
        }
    }
    
    private void saveUsers() {
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("保存用户数据失败: " + e.getMessage());
        }
    }
    
    public void updateUserStats(User user, boolean isWin, boolean isDraw) {
        if (user == null || user.isGuest() || user.isAI()) {
            return;
        }
        
        if (isDraw) {
            user.getStats().recordDraw();
        } else if (isWin) {
            user.getStats().recordWin();
        } else {
            user.getStats().recordLoss();
        }
        
        saveUsers();
    }
    
    public List<User> getAllUsersSortedByWinRate() {
        List<User> userList = new ArrayList<>(users.values());
        userList.sort((u1, u2) -> 
            Double.compare(u2.getStats().getWinRate(), u1.getStats().getWinRate()));
        return userList;
    }
}