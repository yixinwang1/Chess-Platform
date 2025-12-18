// auth/User.java
package com.chessplatform.auth;

import com.chessplatform.stats.UserStats;
import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String password;
    private String passwordHash;
    private UserStats stats;
    private boolean isAI;
    private boolean isGuest;
    
    public User(String username, String passwordHash) {
        this.username = username;
        this.password = passwordHash;
        this.passwordHash = passwordHash;
        this.stats = new UserStats();
        this.isAI = false;
        this.isGuest = false;
    }
    
    // AI用户构造方法
    public static User createAIUser(String aiName) {
        User aiUser = new User(aiName, "");
        aiUser.isAI = true;
        aiUser.stats = new UserStats();
        return aiUser;
    }
    
    // 游客构造方法
    public static User createGuestUser() {
        User guest = new User("游客", "");
        guest.isGuest = true;
        return guest;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public UserStats getStats() { return stats; }
    public boolean isAI() { return isAI; }
    public boolean isGuest() { return isGuest; }
    public boolean isRegistered() { return !isAI && !isGuest; }
    
    public boolean verifyPassword(String password) {
        System.out.println("here!");
        return this.password.equals(password);
    }
    
    public void updatePassword(String newPassword) {
        this.password = newPassword;
        this.passwordHash = hashPassword(newPassword);
    }
    
    private String hashPassword(String password) {
        // 简单哈希实现，实际项目中应该使用更安全的哈希算法
        return Integer.toHexString(password.hashCode());
    }
    
    @Override
    public String toString() {
        if (isAI) {
            return username + "(AI)";
        } else if (isGuest) {
            return username;
        } else {
            return username + "[" + stats.getWinRate() + "%]";
        }
    }
}