// auth/AuthenticationService.java
package com.chessplatform.auth;

public class AuthenticationService {
    private UserManager userManager;
    
    public AuthenticationService(UserManager userManager) {
        this.userManager = userManager;
    }
    
    public boolean register(String username, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return false;
        }
        
        return userManager.register(username, password);
    }
    
    public boolean login(String username, String password) {
        return userManager.login(username, password);
    }
    
    // 添加 logout 方法
    public void logout() {
        userManager.logout();
    }
    
    public User getCurrentUser() {
        return userManager.getCurrentUser();
    }
    
    public boolean isLoggedIn() {
        return userManager.getCurrentUser().isRegistered();
    }
}