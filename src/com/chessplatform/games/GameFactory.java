// games/GameFactory.java
package com.chessplatform.games;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.games.gomoku.Gomoku;
import com.chessplatform.games.go.Go;

public class GameFactory {
    public static Game createGame(GameType gameType, int boardSize) {
        switch (gameType) {
            case GOMOKU:
                return new Gomoku(boardSize);
            case GO:
                return new Go(boardSize);
            default:
                throw new IllegalArgumentException("不支持的棋类类型: " + gameType);
        }
    }
}