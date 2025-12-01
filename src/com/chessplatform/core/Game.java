// core/Game.java
package com.chessplatform.core;

import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.Board;
import com.chessplatform.model.Player;
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
}