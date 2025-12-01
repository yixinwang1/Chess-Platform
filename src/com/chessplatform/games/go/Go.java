// games/go/Go.java
package com.chessplatform.games.go;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.*;
import java.io.Serializable;
import java.util.*;

public class Go implements Game, Serializable {
    private Board board;
    private Player blackPlayer;
    private Player whitePlayer;
    private Player currentPlayer;
    private boolean gameOver;
    private Player winner;
    private Stack<Move> moveHistory;
    private int consecutivePasses;
    private Point lastKoPoint;
    
    public Go(int boardSize) {
        this.board = new Board(boardSize);
        this.blackPlayer = new Player("黑方", PieceColor.BLACK);
        this.whitePlayer = new Player("白方", PieceColor.WHITE);
        this.currentPlayer = blackPlayer;
        this.gameOver = false;
        this.winner = null;
        this.moveHistory = new Stack<>();
        this.consecutivePasses = 0;
        this.lastKoPoint = null;
    }
    
    @Override
    public boolean makeMove(int row, int col) {
        if (gameOver || !isValidMove(row, col)) {
            return false;
        }
        
        // 检查劫争
        if (lastKoPoint != null && lastKoPoint.getX() == row && lastKoPoint.getY() == col) {
            return false;
        }
        
        // 落子
        board.setPiece(row, col, new Piece(currentPlayer.getColor()));
        Move move = new Move(currentPlayer, row, col);
        moveHistory.push(move);
        consecutivePasses = 0;
        
        // 提子
        List<Point> capturedStones = captureStones(row, col);
        
        // 检查自尽
        if (!hasLiberties(row, col)) {
            // 撤销落子
            board.clearPosition(row, col);
            moveHistory.pop();
            return false;
        }
        
        // 设置劫点
        if (capturedStones.size() == 1) {
            lastKoPoint = capturedStones.get(0);
        } else {
            lastKoPoint = null;
        }
        
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
        
        // 围棋悔棋比较复杂，这里简化实现
        // 在实际项目中需要更复杂的实现来恢复完整状态
        if (!moveHistory.isEmpty()) {
            moveHistory.pop();
            // 重新开始游戏并重放所有走法（简化实现）
            resetAndReplay();
            return true;
        }
        return false;
    }
    
    private void resetAndReplay() {
        // 保存走法历史
        List<Move> moves = new ArrayList<>(moveHistory);
        // 重置游戏
        board.clear();
        gameOver = false;
        winner = null;
        currentPlayer = blackPlayer;
        consecutivePasses = 0;
        lastKoPoint = null;
        moveHistory.clear();
        
        // 重放走法
        for (Move move : moves) {
            if (move.isNormalMove()) {
                makeMove(move.getRow(), move.getCol());
            } else if (move.isPass()) {
                pass();
            }
        }
    }
    
    private List<Point> captureStones(int row, int col) {
        List<Point> captured = new ArrayList<>();
        PieceColor currentColor = currentPlayer.getColor();
        PieceColor opponentColor = currentColor.getOpposite();
        
        // 检查四个方向的对手棋子
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (board.isValidPosition(newRow, newCol)) {
                Piece piece = board.getPiece(newRow, newCol);
                if (piece.getColor() == opponentColor) {
                    Set<Point> group = findGroup(newRow, newCol);
                    if (!hasLiberties(group)) {
                        captured.addAll(group);
                        for (Point p : group) {
                            board.clearPosition(p.getX(), p.getY());
                        }
                    }
                }
            }
        }
        
        return captured;
    }
    
    private Set<Point> findGroup(int startRow, int startCol) {
        Set<Point> group = new HashSet<>();
        Stack<Point> stack = new Stack<>();
        PieceColor color = board.getPiece(startRow, startCol).getColor();
        
        stack.push(new Point(startRow, startCol));
        
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            if (group.contains(p)) continue;
            
            group.add(p);
            
            int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : directions) {
                int newRow = p.getX() + dir[0];
                int newCol = p.getY() + dir[1];
                
                if (board.isValidPosition(newRow, newCol)) {
                    Point neighbor = new Point(newRow, newCol);
                    if (!group.contains(neighbor) && 
                        board.getPiece(newRow, newCol).getColor() == color) {
                        stack.push(neighbor);
                    }
                }
            }
        }
        
        return group;
    }
    
    private boolean hasLiberties(int row, int col) {
        Set<Point> group = findGroup(row, col);
        return hasLiberties(group);
    }
    
    private boolean hasLiberties(Set<Point> group) {
        for (Point p : group) {
            int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : directions) {
                int newRow = p.getX() + dir[0];
                int newCol = p.getY() + dir[1];
                
                if (board.isValidPosition(newRow, newCol)) {
                    if (board.getPiece(newRow, newCol).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void calculateWinner() {
        // 使用简化版的数子法
        int blackScore = 0;
        int whiteScore = 0;
        double komi = 6.5; // 贴目
        
        // 计算地盘和子数
        for (int i = 0; i < board.getSize(); i++) {
            for (int j = 0; j < board.getSize(); j++) {
                Piece piece = board.getPiece(i, j);
                if (piece.getColor() == PieceColor.BLACK) {
                    blackScore++;
                } else if (piece.getColor() == PieceColor.WHITE) {
                    whiteScore++;
                } else {
                    // 空点，判断属于谁的地盘（简化实现）
                    // 在实际项目中需要更复杂的判断逻辑
                    if (isTerritoryFor(i, j, PieceColor.BLACK)) {
                        blackScore++;
                    } else if (isTerritoryFor(i, j, PieceColor.WHITE)) {
                        whiteScore++;
                    }
                }
            }
        }
        
        // 白方加贴目
        whiteScore += komi;
        
        if (blackScore > whiteScore) {
            winner = blackPlayer;
        } else if (whiteScore > blackScore) {
            winner = whitePlayer;
        } else {
            winner = null; // 平局
        }
    }
    
    private boolean isTerritoryFor(int row, int col, PieceColor color) {
        // 简化实现：检查周围的棋子
        // 在实际项目中需要更复杂的判断逻辑
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (board.isValidPosition(newRow, newCol)) {
                Piece piece = board.getPiece(newRow, newCol);
                if (piece.getColor() == color.getOpposite()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void switchPlayer() {
        currentPlayer = (currentPlayer == blackPlayer) ? whitePlayer : blackPlayer;
    }
    
    @Override
    public boolean isValidMove(int row, int col) {
        if (gameOver) return false;
        if (!board.isValidPosition(row, col)) return false;
        if (!board.isPositionEmpty(row, col)) return false;
        
        // 检查劫争
        if (lastKoPoint != null && lastKoPoint.getX() == row && lastKoPoint.getY() == col) {
            return false;
        }
        
        return true;
    }
    
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
        return GameType.GO;
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
        return "当前玩家: " + currentPlayer.getName() + 
               " (连续虚着: " + consecutivePasses + "/2)";
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
        Go savedState = (Go) memento.getSavedState();
        this.board = savedState.board.copy();
        this.currentPlayer = savedState.currentPlayer;
        this.gameOver = savedState.gameOver;
        this.winner = savedState.winner;
        this.moveHistory = new Stack<>();
        this.moveHistory.addAll(savedState.moveHistory);
        this.consecutivePasses = savedState.consecutivePasses;
        this.lastKoPoint = savedState.lastKoPoint;
    }
}