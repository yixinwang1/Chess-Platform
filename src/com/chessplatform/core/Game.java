// 修改 core/Game.java
package com.chessplatform.core;

import com.chessplatform.model.Board;
import com.chessplatform.model.Player;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.recorder.GameRecorder;  // 新增导入

import java.io.Serializable;

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
}