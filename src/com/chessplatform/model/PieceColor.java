// model/PieceColor.java
package com.chessplatform.model;

public enum PieceColor {
    BLACK("●", "黑"),
    WHITE("○", "白"),
    EMPTY("+", "空");
    
    private final String symbol;
    private final String chineseName;
    
    PieceColor(String symbol, String chineseName) {
        this.symbol = symbol;
        this.chineseName = chineseName;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public PieceColor getOpposite() {
        if (this == BLACK) return WHITE;
        if (this == WHITE) return BLACK;
        return EMPTY;
    }
}