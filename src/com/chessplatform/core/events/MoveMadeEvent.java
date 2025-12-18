// core/events/MoveMadeEvent.java
package com.chessplatform.core.events;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameEvent;
import com.chessplatform.model.Move;

public class MoveMadeEvent extends GameEvent {
    private Move move;
    
    public MoveMadeEvent(Game source, Move move) {
        super(source);
        this.move = move;
    }
    
    public Move getMove() {
        return move;
    }
}