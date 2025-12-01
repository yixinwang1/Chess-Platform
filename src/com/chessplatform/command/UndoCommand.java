// command/UndoCommand.java
package com.chessplatform.command;

import com.chessplatform.core.Game;
import com.chessplatform.memento.GameCaretaker;
import com.chessplatform.memento.GameMemento;

public class UndoCommand implements Command {
    private Game game;
    private GameCaretaker caretaker;
    private GameMemento undoneState;
    
    public UndoCommand(Game game, GameCaretaker caretaker) {
        this.game = game;
        this.caretaker = caretaker;
    }
    
    @Override
    public boolean execute() {
        if (!caretaker.canUndo()) {
            return false;
        }
        
        undoneState = game.saveToMemento();
        GameMemento lastState = caretaker.getLastMemento();
        if (lastState != null) {
            game.restoreFromMemento(lastState);
            return true;
        }
        return false;
    }
    
    @Override
    public void undo() {
        if (undoneState != null) {
            game.restoreFromMemento(undoneState);
            caretaker.saveMemento(undoneState);
        }
    }
    
    @Override
    public String getDescription() {
        return "悔棋";
    }
}