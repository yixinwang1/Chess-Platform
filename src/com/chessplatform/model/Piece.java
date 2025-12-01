// model/Piece.java
package com.chessplatform.model;

import java.io.Serializable;

public class Piece implements Serializable {
    private PieceColor color;
    
    public Piece(PieceColor color) {
        this.color = color;
    }
    
    public PieceColor getColor() {
        return color;
    }
    
    public void setColor(PieceColor color) {
        this.color = color;
    }
    
    public boolean isEmpty() {
        return color == PieceColor.EMPTY;
    }
    
    @Override
    public String toString() {
        return color.getSymbol();
    }
}