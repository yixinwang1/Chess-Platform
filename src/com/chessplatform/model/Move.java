// model/Move.java
package com.chessplatform.model;

import java.io.Serializable;

public class Move implements Serializable {
    private Player player;
    private int row;
    private int col;
    private boolean isPass;
    private boolean isResign;
    
    public Move(Player player, int row, int col) {
        this.player = player;
        this.row = row;
        this.col = col;
        this.isPass = false;
        this.isResign = false;
    }
    
    public static Move createPassMove(Player player) {
        Move move = new Move(player, -1, -1);
        move.isPass = true;
        return move;
    }
    
    public static Move createResignMove(Player player) {
        Move move = new Move(player, -1, -1);
        move.isResign = true;
        return move;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public int getRow() {
        return row;
    }
    
    public int getCol() {
        return col;
    }
    
    public boolean isPass() {
        return isPass;
    }
    
    public boolean isResign() {
        return isResign;
    }
    
    public boolean isNormalMove() {
        return !isPass && !isResign;
    }
    
    @Override
    public String toString() {
        if (isPass) return player.getName() + " 虚着";
        if (isResign) return player.getName() + " 认输";
        return player.getName() + " 落子于 (" + row + ", " + col + ")";
    }
}