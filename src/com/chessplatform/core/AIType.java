// 新增 core/AIType.java
package com.chessplatform.core;

public enum AIType {
    NONE("无AI"),                    // 双人对战
    RANDOM("随机AI"),                // 一级AI
    RULE("规则AI"),                 // 二级AI
    ADVANCED("高级AI"),              // 三级AI（可选）
    MCTS("MCTS AI");                 // 三级AI（可选）
    
    private final String description;
    
    AIType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static AIType fromString(String text) {
        for (AIType type : AIType.values()) {
            if (type.name().equalsIgnoreCase(text) || 
                type.description.equals(text)) {
                return type;
            }
        }
        return NONE;
    }
}