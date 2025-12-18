// model/Player.java
package com.chessplatform.model;

import com.chessplatform.auth.User;
import java.io.Serializable;

public class Player implements Serializable {
    private User user;
    private PieceColor color;
    private boolean hasResigned;
    
    public Player(User user, PieceColor color) {
        this.user = user;
        this.color = color;
        this.hasResigned = false;
    }
    
    // 兼容旧构造方法
    public Player(String name, PieceColor color) {
        this(new com.chessplatform.auth.User(name, ""), color);
    }
    
    public String getName() {
        return user.getUsername();
    }
    
    public User getUser() {
        return user;
    }
    
    public PieceColor getColor() {
        return color;
    }
    
    public boolean hasResigned() {
        return hasResigned;
    }
    
    public void resign() {
        this.hasResigned = true;
    }
    
    @Override
    public String toString() {
        String stats = user.isRegistered() ? user.getStats().getFormattedStats() : "";
        return getName() + "(" + color.getChineseName() + ")" + 
               (stats.isEmpty() ? "" : " " + stats);
    }
}