// 创建 games/reversi/Reversi.java
package com.chessplatform.games.reversi;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.model.*;
import com.chessplatform.memento.GameMemento;

import java.io.Serializable;
import java.util.*;

public class Reversi implements Game, Serializable {
    private static final long serialVersionUID = 1L;
    
    // 8个方向向量
    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1},           {0, 1},
        {1, -1},  {1, 0},  {1, 1}
    };
    
    private Board board;
    private Player blackPlayer;
    private Player whitePlayer;
    private Player currentPlayer;
    private boolean gameOver;
    private Player winner;
    private Stack<Move> moveHistory;
    private int consecutivePasses;
    private List<Point> lastFlippedStones;  // 记录上次翻转的棋子
    
    public Reversi() {
        // 黑白棋固定8×8棋盘
        this.board = new Board(8);
        this.blackPlayer = new Player("黑方", PieceColor.BLACK);
        this.whitePlayer = new Player("白方", PieceColor.WHITE);
        this.currentPlayer = blackPlayer;
        this.gameOver = false;
        this.winner = null;
        this.moveHistory = new Stack<>();
        this.consecutivePasses = 0;
        this.lastFlippedStones = new ArrayList<>();
        
        initializeBoard();
    }
    
    private void initializeBoard() {
        // 清空棋盘
        board.clear();
        
        // 设置初始4子
        int center = board.getSize() / 2 - 1;
        board.setPiece(center, center, new Piece(PieceColor.WHITE));
        board.setPiece(center, center + 1, new Piece(PieceColor.BLACK));
        board.setPiece(center + 1, center, new Piece(PieceColor.BLACK));
        board.setPiece(center + 1, center + 1, new Piece(PieceColor.WHITE));
    }
    
    @Override
    public boolean makeMove(int row, int col) {
        if (gameOver || !isValidMove(row, col)) {
            return false;
        }
        
        // 执行落子
        board.setPiece(row, col, new Piece(currentPlayer.getColor()));
        Move move = new Move(currentPlayer, row, col);
        moveHistory.push(move);
        
        // 翻转对方棋子
        lastFlippedStones.clear();
        flipOpponentStones(row, col);
        
        consecutivePasses = 0;
        
        // 检查游戏是否结束
        checkGameEnd();
        
        // 切换玩家
        switchPlayer();
        
        return true;
    }
    
    @Override
    public boolean pass() {
        if (gameOver) return false;
        
        consecutivePasses++;
        Move move = Move.createPassMove(currentPlayer);
        moveHistory.push(move);
        
        if (consecutivePasses >= 2) {
            gameOver = true;
            calculateWinner();
        } else {
            switchPlayer();
        }
        return true;
    }
    
    @Override
    public boolean resign(Player player) {
        if (gameOver) return false;
        
        gameOver = true;
        winner = (player == blackPlayer) ? whitePlayer : blackPlayer;
        player.resign();
        
        Move move = Move.createResignMove(player);
        moveHistory.push(move);
        return true;
    }
    
    @Override
    public boolean undo() {
        if (moveHistory.isEmpty() || gameOver) {
            return false;
        }
        
        // 弹出最后一步
        Move lastMove = moveHistory.pop();
        
        if (lastMove.isNormalMove()) {
            // 移除落子
            board.clearPosition(lastMove.getRow(), lastMove.getCol());
            
            // 恢复被翻转的棋子
            // 注意：需要记录每次翻转的棋子以便恢复
            // 这里简化实现，实际需要更复杂的记录
            
            // 切换回上一个玩家
            switchPlayer();
            return true;
        } else if (lastMove.isPass()) {
            // 撤销pass
            consecutivePasses--;
            switchPlayer();
            return true;
        }
        return false;
    }
    
    private void flipOpponentStones(int row, int col) {
        PieceColor currentColor = currentPlayer.getColor();
        PieceColor opponentColor = currentColor.getOpposite();
        
        for (int[] dir : DIRECTIONS) {
            List<Point> toFlip = new ArrayList<>();
            
            int r = row + dir[0];
            int c = col + dir[1];
            
            // 沿着方向搜索
            while (board.isValidPosition(r, c)) {
                Piece piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    break;  // 遇到空位，停止
                }
                
                if (piece.getColor() == opponentColor) {
                    // 对方棋子，添加到待翻转列表
                    toFlip.add(new Point(r, c));
                } else if (piece.getColor() == currentColor) {
                    // 遇到己方棋子，翻转之间的所有对方棋子
                    if (!toFlip.isEmpty()) {
                        for (Point p : toFlip) {
                            board.setPiece(p.getX(), p.getY(), new Piece(currentColor));
                            lastFlippedStones.add(p);
                        }
                    }
                    break;
                }
                
                r += dir[0];
                c += dir[1];
            }
        }
    }
    
    @Override
    public boolean isValidMove(int row, int col) {
        if (gameOver) return false;
        if (!board.isValidPosition(row, col)) return false;
        if (!board.isPositionEmpty(row, col)) return false;
        
        // 检查8个方向是否可以翻转对方棋子
        PieceColor currentColor = currentPlayer.getColor();
        PieceColor opponentColor = currentColor.getOpposite();
        
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0];
            int c = col + dir[1];
            
            boolean foundOpponent = false;
            
            while (board.isValidPosition(r, c)) {
                Piece piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    break;  // 遇到空位，无效
                }
                
                if (piece.getColor() == opponentColor) {
                    foundOpponent = true;
                } else if (piece.getColor() == currentColor) {
                    if (foundOpponent) {
                        return true;  // 找到可以翻转的序列
                    }
                    break;
                }
                
                r += dir[0];
                c += dir[1];
            }
        }
        
        return false;
    }
    
    private void checkGameEnd() {
        // 检查双方是否都无合法落子
        boolean blackHasMove = playerHasMove(blackPlayer);
        boolean whiteHasMove = playerHasMove(whitePlayer);
        
        if (!blackHasMove && !whiteHasMove) {
            gameOver = true;
            calculateWinner();
        } else if (!hasValidMoves()) {
            // 当前玩家无合法落子，自动pass
            pass();
        }
    }
    
    private boolean hasValidMoves() {
        // 检查当前玩家是否有合法落子
        int size = board.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (isValidMove(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean playerHasMove(Player player) {
        // 保存当前玩家
        Player savedPlayer = currentPlayer;
        currentPlayer = player;
        
        boolean hasMove = hasValidMoves();
        
        // 恢复当前玩家
        currentPlayer = savedPlayer;
        
        return hasMove;
    }
    
    private void calculateWinner() {
        int blackCount = 0;
        int whiteCount = 0;
        
        int size = board.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Piece piece = board.getPiece(i, j);
                if (piece.getColor() == PieceColor.BLACK) {
                    blackCount++;
                } else if (piece.getColor() == PieceColor.WHITE) {
                    whiteCount++;
                }
            }
        }
        
        if (blackCount > whiteCount) {
            winner = blackPlayer;
        } else if (whiteCount > blackCount) {
            winner = whitePlayer;
        } else {
            winner = null;  // 平局
        }
    }
    
    private void switchPlayer() {
        currentPlayer = (currentPlayer == blackPlayer) ? whitePlayer : blackPlayer;
    }
    
    // 添加一个方法获取合法落子位置（用于界面提示）
    public List<Point> getValidMoves() {
        List<Point> validMoves = new ArrayList<>();
        int size = board.getSize();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (isValidMove(i, j)) {
                    validMoves.add(new Point(i, j));
                }
            }
        }
        
        return validMoves;
    }
    
    // 添加一个方法获取棋子统计
    public Map<PieceColor, Integer> getPieceCount() {
        Map<PieceColor, Integer> counts = new HashMap<>();
        counts.put(PieceColor.BLACK, 0);
        counts.put(PieceColor.WHITE, 0);
        
        int size = board.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Piece piece = board.getPiece(i, j);
                if (!piece.isEmpty()) {
                    counts.put(piece.getColor(), counts.get(piece.getColor()) + 1);
                }
            }
        }
        
        return counts;
    }
    
    // Game接口方法的实现
    @Override
    public boolean isGameOver() {
        return gameOver;
    }
    
    @Override
    public Player getWinner() {
        return winner;
    }
    
    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    
    @Override
    public Board getBoard() {
        return board;
    }
    
    @Override
    public GameType getGameType() {
        return GameType.REVERSI;
    }
    
    @Override
    public String getGameStatus() {
        if (gameOver) {
            if (winner != null) {
                return "游戏结束! 获胜者: " + winner.getName();
            } else {
                return "游戏结束! 平局";
            }
        }
        
        Map<PieceColor, Integer> counts = getPieceCount();
        return "当前玩家: " + currentPlayer.getName() + 
               " (黑子:" + counts.get(PieceColor.BLACK) + 
               " 白子:" + counts.get(PieceColor.WHITE) + ")";
    }
    
    @Override
    public int getMoveCount() {
        return moveHistory.size();
    }
    
    @Override
    public GameMemento saveToMemento() {
        return new GameMemento(this);
    }
    
    @Override
    public void restoreFromMemento(GameMemento memento) {
        Reversi savedState = (Reversi) memento.getSavedState();
        this.board = savedState.board.copy();
        this.currentPlayer = savedState.currentPlayer;
        this.gameOver = savedState.gameOver;
        this.winner = savedState.winner;
        this.moveHistory = new Stack<>();
        this.moveHistory.addAll(savedState.moveHistory);
        this.consecutivePasses = savedState.consecutivePasses;
        this.lastFlippedStones = new ArrayList<>(savedState.lastFlippedStones);
    }
}