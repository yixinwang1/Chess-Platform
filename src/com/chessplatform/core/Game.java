// 修改 core/Game.java
package com.chessplatform.core;

import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.Board;
import com.chessplatform.model.PieceColor;
import com.chessplatform.model.Player;  // 新增导入
import com.chessplatform.model.Point;
import com.chessplatform.recorder.GameRecorder;
import java.io.Serializable;
import java.util.List;

public interface Game extends Serializable {
    // 游戏操作
    boolean makeMove(int row, int col);
    boolean pass();
    boolean resign(Player player);
    boolean undo();
    
    // 游戏状态查询
    boolean isValidMove(int row, int col);
    boolean isGameOver();
    Player getWinner();
    Player getCurrentPlayer();
    Board getBoard();
    GameType getGameType();
    
    // 存档相关
    GameMemento saveToMemento();
    void restoreFromMemento(GameMemento memento);
    
    // 游戏信息
    String getGameStatus();
    int getMoveCount();
    
    // 新增：录像相关方法
    GameRecorder getGameRecorder();
    void setGameRecorder(GameRecorder recorder);
    void recordMove(int row, int col);
    void recordPass();
    void recordResign(Player player);
    
    // 新增：回放相关方法
    boolean isReplayMode();
    void setReplayMode(boolean replayMode);
    void setReplayStep(int step);
    Board getBoardAtStep(int step);
    
    // 新增AI相关方法
    boolean isAIMove();  // 当前是否为AI走棋
    void setAITypeForPlayer(Player player, AIType aiType);
    AIType getAITypeForPlayer(Player player);
    
    // 新增游戏复制方法（用于AI模拟）
    Game copy();
    
    // 新增获取合法位置方法
    List<Point> getValidMoves();
    
    // 新增玩家颜色获取
    PieceColor getPlayerColor(Player player);
}