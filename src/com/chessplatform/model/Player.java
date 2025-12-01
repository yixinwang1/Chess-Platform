// model/Player.java
package com.chessplatform.model;

import java.io.Serializable;

public class Player implements Serializable {
    private String name;
    private PieceColor color;
    private boolean hasResigned;
    
    public Player(String name, PieceColor color) {
        this.name = name;
        this.color = color;
        this.hasResigned = false;
    }
    
    public String getName() {
        return name;
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
        return name + "(" + color.getChineseName() + ")";
    }
}