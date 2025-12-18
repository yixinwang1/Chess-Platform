// core/events/PlayerResignedEvent.java
package com.chessplatform.core.events;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameEvent;
import com.chessplatform.model.Player;

public class PlayerResignedEvent extends GameEvent {
    private Player resignedPlayer;
    private Player winner;
    
    public PlayerResignedEvent(Game source, Player resignedPlayer) {
        super(source);
        this.resignedPlayer = resignedPlayer;
        this.winner = (resignedPlayer == source.getBlackPlayer()) ? 
                     source.getWhitePlayer() : source.getBlackPlayer();
    }
    
    public Player getResignedPlayer() {
        return resignedPlayer;
    }
    
    public Player getWinner() {
        return winner;
    }
}