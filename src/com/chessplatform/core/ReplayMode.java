// 新增 core/ReplayMode.java
package com.chessplatform.core;

public enum ReplayMode {
    NORMAL("正常模式"),      // 正常游戏模式
    REPLAY("回放模式");     // 回放观看模式
    
    private final String description;
    
    ReplayMode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}