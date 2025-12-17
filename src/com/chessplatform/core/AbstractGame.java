// 新增 games/AbstractGame.java（可选）
package com.chessplatform.core;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.core.ReplayMode;
import com.chessplatform.model.*;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.recorder.GameRecorder;

import java.io.Serializable;

public abstract class AbstractGame implements Game, Serializable {
    protected Board board;
    protected Player blackPlayer;
    protected Player whitePlayer;
    protected Player currentPlayer;
    protected boolean gameOver;
    protected Player winner;
    protected GameType gameType;
    
    // 新增：录像和回放相关字段
    protected GameRecorder gameRecorder;
    protected ReplayMode replayMode;
    protected int replayStep;
    
    public AbstractGame(int boardSize, GameType gameType) {
        this.board = new Board(boardSize);
        this.blackPlayer = new Player("黑方", PieceColor.BLACK);
        this.whitePlayer = new Player("白方", PieceColor.WHITE);
        this.currentPlayer = blackPlayer;
        this.gameOver = false;
        this.winner = null;
        this.gameType = gameType;
        
        // 初始化录像和回放
        this.gameRecorder = new GameRecorder();
        this.replayMode = ReplayMode.NORMAL;
        this.replayStep = 0;
        
        // 记录初始状态
        gameRecorder.recordInitialState(board, currentPlayer);
    }
    
    // 录像相关方法实现
    @Override
    public GameRecorder getGameRecorder() {
        return gameRecorder;
    }
    
    @Override
    public void setGameRecorder(GameRecorder recorder) {
        this.gameRecorder = recorder;
    }
    
    @Override
    public void recordMove(int row, int col) {
        if (gameRecorder != null && !isReplayMode()) {
            Move move = new Move(currentPlayer, row, col);
            gameRecorder.recordMove(move, board);
        }
    }
    
    @Override
    public void recordPass() {
        if (gameRecorder != null && !isReplayMode()) {
            Move move = Move.createPassMove(currentPlayer);
            gameRecorder.recordMove(move, board);
        }
    }
    
    @Override
    public void recordResign(Player player) {
        if (gameRecorder != null && !isReplayMode()) {
            Move move = Move.createResignMove(player);
            gameRecorder.recordMove(move, board);
        }
    }
    
    // 回放相关方法实现
    @Override
    public boolean isReplayMode() {
        return replayMode == ReplayMode.REPLAY;
    }
    
    @Override
    public void setReplayMode(boolean replayMode) {
        this.replayMode = replayMode ? ReplayMode.REPLAY : ReplayMode.NORMAL;
        if (replayMode) {
            this.replayStep = 0;
        }
    }
    
    @Override
    public void setReplayStep(int step) {
        if (isReplayMode()) {
            this.replayStep = Math.max(0, Math.min(step, gameRecorder.getTotalMoves()));
            // 更新棋盘到指定步数
            updateBoardToReplayStep();
        }
    }
    
    @Override
    public Board getBoardAtStep(int step) {
        if (step >= 0 && step <= gameRecorder.getTotalMoves()) {
            // 简化实现：返回当前棋盘
            // 实际应该根据step重建棋盘
            return board;
        }
        return null;
    }
    
    // 抽象方法，子类实现
    protected abstract void updateBoardToReplayStep();
    
    // 原有抽象方法...
}