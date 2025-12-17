// 修改 model/Move.java
package com.chessplatform.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Move implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Player player;
    private int row;
    private int col;
    private boolean isPass;
    private boolean isResign;
    private LocalDateTime timestamp;      // 新增：时间戳
    private int moveNumber;               // 新增：步数编号
    private String comment;               // 新增：注释（可选）
    
    public Move(Player player, int row, int col) {
        this.player = player;
        this.row = row;
        this.col = col;
        this.isPass = false;
        this.isResign = false;
        this.timestamp = LocalDateTime.now();
        this.comment = "";
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
    
    // 新增：带注释的构造函数
    public static Move createMoveWithComment(Player player, int row, int col, String comment) {
        Move move = new Move(player, row, col);
        move.comment = comment;
        return move;
    }
    
    // Getters and Setters
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
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public int getMoveNumber() {
        return moveNumber;
    }
    
    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    @Override
    public String toString() {
        String base = String.format("第%d步 %s ", moveNumber, timestamp.toLocalTime());
        if (isPass) return base + player.getName() + " 虚着";
        if (isResign) return base + player.getName() + " 认输";
        return base + player.getName() + " 落子于 (" + row + ", " + col + ")" + 
               (comment.isEmpty() ? "" : " [" + comment + "]");
    }
}