// 新增 ai/AbstractAI.java
package com.chessplatform.ai;

import com.chessplatform.core.Game;
import com.chessplatform.model.Point;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

public abstract class AbstractAI implements AI, Serializable {
    protected String name;
    protected int level;
    protected long timeLimit = 1000; // 默认1秒
    protected Random random = new Random();
    
    public AbstractAI(String name, int level) {
        this.name = name;
        this.level = level;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int getLevel() {
        return level;
    }
    
    @Override
    public void setTimeLimit(long milliseconds) {
        this.timeLimit = milliseconds;
    }
    
    /**
     * 获取所有合法落子位置
     */
    protected List<Point> getValidMoves(Game game) {
        List<Point> validMoves = game.getValidMoves();
        if (validMoves.isEmpty()) {
            return null;
        }
        return validMoves;
    }
}