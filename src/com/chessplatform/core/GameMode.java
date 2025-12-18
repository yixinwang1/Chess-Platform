// 新增 core/GameMode.java
package com.chessplatform.core;

public enum GameMode {
    PLAYER_VS_PLAYER("玩家对战"),
    PLAYER_VS_AI("人机对战"),
    AI_VS_AI("AI对战");
    
    private final String description;
    
    GameMode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}