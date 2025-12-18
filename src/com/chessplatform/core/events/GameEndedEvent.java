// core/events/GameEndedEvent.java
package com.chessplatform.core.events;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameEvent;
import com.chessplatform.model.Player;

public class GameEndedEvent extends GameEvent {
    private Player winner;
    private Player loser;
    private boolean isDraw;
    
    public GameEndedEvent(Game source, Player winner, boolean isDraw) {
        super(source);
        this.winner = winner;
        this.isDraw = isDraw;
        
        if (!isDraw && winner != null) {
            this.loser = (winner == source.getBlackPlayer()) ? 
                        source.getWhitePlayer() : source.getBlackPlayer();
        } else {
            this.loser = null;
        }
    }
    
    public Player getWinner() {
        return winner;
    }
    
    public Player getLoser() {
        return loser;
    }
    
    public boolean isDraw() {
        return isDraw;
    }
}
