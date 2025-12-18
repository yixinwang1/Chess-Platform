// core/GameEvent.java - 新增事件基类
package com.chessplatform.core;

public class GameEvent {
    protected Game source;
    
    public GameEvent(Game source) {
        this.source = source;
    }
    
    public Game getSource() {
        return source;
    }
}