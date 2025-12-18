// stats/UserStats.java
package com.chessplatform.stats;

import java.io.Serializable;

public class UserStats implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int totalGames;
    private int wins;
    private int losses;
    private int draws;
    
    public UserStats() {
        this.totalGames = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
    }
    
    public void recordWin() {
        totalGames++;
        wins++;
    }
    
    public void recordLoss() {
        totalGames++;
        losses++;
    }
    
    public void recordDraw() {
        totalGames++;
        draws++;
    }
    
    public int getTotalGames() { return totalGames; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
    
    public double getWinRate() {
        if (totalGames == 0) return 0.0;
        return Math.round((wins * 100.0) / totalGames * 10.0) / 10.0;
    }
    
    public String getFormattedStats() {
        return String.format("战绩: %d战 %d胜 %d负 %d平 (%.1f%%)", 
            totalGames, wins, losses, draws, getWinRate());
    }
    
    @Override
    public String toString() {
        return getFormattedStats();
    }
}