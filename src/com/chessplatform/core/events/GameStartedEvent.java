// core/events/GameStartedEvent.java
package com.chessplatform.core.events;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameEvent;
import com.chessplatform.model.Player;

public class GameStartedEvent extends GameEvent {
    private Player blackPlayer;
    private Player whitePlayer;
    
    public GameStartedEvent(Game source, Player blackPlayer, Player whitePlayer) {
        super(source);
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
    }
    
    public Player getBlackPlayer() {
        return blackPlayer;
    }
    
    public Player getWhitePlayer() {
        return whitePlayer;
    }
}