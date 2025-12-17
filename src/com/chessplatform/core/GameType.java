// core/GameType.java
package com.chessplatform.core;

public enum GameType {
    GOMOKU("五子棋"),
    GO("围棋"),
    REVERSI("黑白棋");  // 新增
    
    private final String chineseName;
    
    GameType(String chineseName) {
        this.chineseName = chineseName;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public static GameType fromString(String text) {
        for (GameType type : GameType.values()) {
            if (type.name().equalsIgnoreCase(text) || 
                type.chineseName.equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的游戏类型: " + text);
    }
}