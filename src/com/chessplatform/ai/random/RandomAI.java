// 新增 ai/random/RandomAI.java
package com.chessplatform.ai.random;

import com.chessplatform.ai.AbstractAI;
import com.chessplatform.core.Game;
import com.chessplatform.model.Point;
import java.util.List;

public class RandomAI extends AbstractAI {
    
    public RandomAI() {
        super("随机AI", 1);
    }
    
    @Override
    public Point think(Game game) {
        List<Point> validMoves = getValidMoves(game);
        System.out.println("随机AI获取到的合法落子数: " + (validMoves == null ? 0 : validMoves.size()));
        if (validMoves == null || validMoves.isEmpty()) {
            return null; // 无合法落子
        }
        
        // 随机选择一个合法位置
        int index = random.nextInt(validMoves.size());
        System.out.println("index: " + index);
        return validMoves.get(index);
    }
}