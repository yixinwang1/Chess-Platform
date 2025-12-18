// 新增 ai/AI.java
package com.chessplatform.ai;

import com.chessplatform.core.Game;
import com.chessplatform.model.Point;
import java.util.List;

public interface AI {
    /**
     * 获取AI思考的落子位置
     * @param game 当前游戏状态
     * @return 推荐的落子位置，null表示放弃落子
     */
    Point think(Game game);
    
    /**
     * 获取AI名称
     */
    String getName();
    
    /**
     * 获取AI等级
     */
    int getLevel();
    
    /**
     * 设置思考时间限制（毫秒）
     */
    void setTimeLimit(long milliseconds);
}