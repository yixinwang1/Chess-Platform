package com.chessplatform.record;

import com.chessplatform.model.Move;
import com.chessplatform.core.Game;
import com.chessplatform.model.Board;
import com.chessplatform.model.Player;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameRecorder implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Move> moveHistory;          // 完整的走棋记录
    private List<Board> boardSnapshots;      // 每一步后的棋盘快照（可选，用于快速回放）
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String gameTitle;
    private List<String> annotations;        // 注解/评论
    
    public GameRecorder() {
        this.moveHistory = new ArrayList<>();
        this.boardSnapshots = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.startTime = LocalDateTime.now();
    }
    
    public void recordMove(Move move, Board currentBoard) {
        move.setMoveNumber(moveHistory.size() + 1);
        moveHistory.add(move);
        
        // 可选：保存棋盘快照（会增加存储空间）
        if (shouldSaveSnapshot()) {
            boardSnapshots.add(currentBoard.copy());
        }
    }
    
    public void recordInitialState(Board initialBoard, Player firstPlayer) {
        // 记录初始状态
        boardSnapshots.clear();
        boardSnapshots.add(initialBoard.copy());
        
        // 添加初始注解
        addAnnotation("游戏开始，初始棋盘已设置");
        addAnnotation("先手玩家: " + firstPlayer.getName());
    }
    
    public void recordGameEnd(Game game) {
        this.endTime = LocalDateTime.now();
        
        if (game.isGameOver()) {
            Player winner = game.getWinner();
            if (winner != null) {
                addAnnotation("游戏结束，获胜者: " + winner.getName());
            } else {
                addAnnotation("游戏结束，平局");
            }
        }
    }
    
    public void addAnnotation(String annotation) {
        annotations.add(LocalDateTime.now() + ": " + annotation);
    }
    
    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }
    
    public List<Board> getBoardSnapshots() {
        return new ArrayList<>(boardSnapshots);
    }
    
    public int getTotalMoves() {
        return moveHistory.size();
    }
    
    public Move getMove(int index) {
        if (index >= 0 && index < moveHistory.size()) {
            return moveHistory.get(index);
        }
        return null;
    }
    
    public Board getBoardAtMove(int moveIndex) {
        if (moveIndex >= 0 && moveIndex < boardSnapshots.size()) {
            return boardSnapshots.get(moveIndex);
        }
        return null;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public String getGameTitle() {
        return gameTitle;
    }
    
    public void setGameTitle(String gameTitle) {
        this.gameTitle = gameTitle;
    }
    
    public List<String> getAnnotations() {
        return new ArrayList<>(annotations);
    }
    
    public long getDurationInSeconds() {
        if (endTime != null) {
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
        return java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
    }
    
    private boolean shouldSaveSnapshot() {
        // 策略：每10步保存一个完整快照，或者关键步骤保存
        // 可以根据需要调整策略
        return moveHistory.size() % 10 == 0 || moveHistory.size() < 20;
    }
    
    // 回放相关方法
    public ReplayController createReplayController() {
        return new ReplayController(this);
    }
}