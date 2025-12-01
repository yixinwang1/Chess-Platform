// model/Board.java
package com.chessplatform.model;

import java.io.Serializable;

public class Board implements Serializable {
    private int size;
    private Piece[][] grid;
    
    public Board(int size) {
        if (size < 8 || size > 19) {
            throw new IllegalArgumentException("棋盘大小必须在8-19之间");
        }
        this.size = size;
        initializeGrid();
    }
    
    private void initializeGrid() {
        grid = new Piece[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = new Piece(PieceColor.EMPTY);
            }
        }
    }
    
    public int getSize() {
        return size;
    }
    
    public Piece getPiece(int row, int col) {
        if (!isValidPosition(row, col)) {
            throw new IllegalArgumentException("位置超出棋盘范围");
        }
        return grid[row][col];
    }
    
    public void setPiece(int row, int col, Piece piece) {
        if (!isValidPosition(row, col)) {
            throw new IllegalArgumentException("位置超出棋盘范围");
        }
        grid[row][col] = piece;
    }
    
    public void clearPosition(int row, int col) {
        if (!isValidPosition(row, col)) {
            throw new IllegalArgumentException("位置超出棋盘范围");
        }
        grid[row][col] = new Piece(PieceColor.EMPTY);
    }
    
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }
    
    public boolean isPositionEmpty(int row, int col) {
        return getPiece(row, col).isEmpty();
    }
    
    public void clear() {
        initializeGrid();
    }
    
    public Board copy() {
        Board copy = new Board(size);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                copy.grid[i][j] = new Piece(grid[i][j].getColor());
            }
        }
        return copy;
    }
}