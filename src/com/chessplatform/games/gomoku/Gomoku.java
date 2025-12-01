// games/gomoku/Gomoku.java
package com.chessplatform.games.gomoku;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.*;
import java.io.Serializable;
import java.util.Stack;

public class Gomoku implements Game, Serializable {
    private Board board;
    private Player blackPlayer;
    private Player whitePlayer;
    private Player currentPlayer;
    private boolean gameOver;
    private Player winner;
    private Stack<Move> moveHistory;
    private int consecutivePasses;
    
    public Gomoku(int boardSize) {
        this.board = new Board(boardSize);
        this.blackPlayer = new Player("黑方", PieceColor.BLACK);
        this.whitePlayer = new Player("白方", PieceColor.WHITE);
        this.currentPlayer = blackPlayer;
        this.gameOver = false;
        this.winner = null;
        this.moveHistory = new Stack<>();
        this.consecutivePasses = 0;
    }
    
    @Override
    public boolean makeMove(int row, int col) {
        if (gameOver || !isValidMove(row, col)) {
            return false;
        }
        
        // 落子
        board.setPiece(row, col, new Piece(currentPlayer.getColor()));
        Move move = new Move(currentPlayer, row, col);
        moveHistory.push(move);
        consecutivePasses = 0;
        
        // 检查胜负
        if (checkWin(row, col)) {
            gameOver = true;
            winner = currentPlayer;
            return true;
        }
        
        // 检查平局
        if (isBoardFull()) {
            gameOver = true;
            winner = null;
            return true;
        }
        
        // 切换玩家
        switchPlayer();
        return true;
    }
    
    @Override
    public boolean pass() {
        // 五子棋不允许虚着
        return false;
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
        
        Move lastMove = moveHistory.pop();
        if (lastMove.isNormalMove()) {
            board.clearPosition(lastMove.getRow(), lastMove.getCol());
            switchPlayer(); // 切换回上一个玩家
            return true;
        } else if (lastMove.isResign()) {
            gameOver = false;
            winner = null;
            lastMove.getPlayer().resign();
            return true;
        }
        return false;
    }
    
    private boolean checkWin(int row, int col) {
        PieceColor color = board.getPiece(row, col).getColor();
        
        // 检查8个方向
        int[][] directions = {
            {1, 0},   // 水平
            {0, 1},   // 垂直
            {1, 1},   // 对角线
            {1, -1}   // 反对角线
        };
        
        for (int[] dir : directions) {
            int count = 1;
            
            // 正向检查
            for (int i = 1; i < 5; i++) {
                int newRow = row + dir[0] * i;
                int newCol = col + dir[1] * i;
                if (!board.isValidPosition(newRow, newCol) || 
                    board.getPiece(newRow, newCol).getColor() != color) {
                    break;
                }
                count++;
            }
            
            // 反向检查
            for (int i = 1; i < 5; i++) {
                int newRow = row - dir[0] * i;
                int newCol = col - dir[1] * i;
                if (!board.isValidPosition(newRow, newCol) || 
                    board.getPiece(newRow, newCol).getColor() != color) {
                    break;
                }
                count++;
            }
            
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isBoardFull() {
        for (int i = 0; i < board.getSize(); i++) {
            for (int j = 0; j < board.getSize(); j++) {
                if (board.getPiece(i, j).isEmpty()) {
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
        return board.isPositionEmpty(row, col);
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
        return GameType.GOMOKU;
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
        return "当前玩家: " + currentPlayer.getName();
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
        Gomoku savedState = (Gomoku) memento.getSavedState();
        this.board = savedState.board.copy();
        this.currentPlayer = savedState.currentPlayer;
        this.gameOver = savedState.gameOver;
        this.winner = savedState.winner;
        this.moveHistory = new Stack<>();
        this.moveHistory.addAll(savedState.moveHistory);
    }
}