// command/MoveCommand.java
package com.chessplatform.command;

import com.chessplatform.core.Game;
import com.chessplatform.memento.GameCaretaker;
import com.chessplatform.memento.GameMemento;

public class MoveCommand implements Command {
    private Game game;
    private GameCaretaker caretaker;
    private int row;
    private int col;
    private GameMemento backup;
    
    public MoveCommand(Game game, GameCaretaker caretaker, int row, int col) {
        this.game = game;
        this.caretaker = caretaker;
        this.row = row;
        this.col = col;
    }
    
    @Override
    public boolean execute() {
        backup = game.saveToMemento();
        boolean success = game.makeMove(row, col);
        if (success) {
            caretaker.saveMemento(backup);
        }
        return success;
    }
    
    @Override
    public void undo() {
        if (backup != null) {
            game.restoreFromMemento(backup);
        }
    }
    
    @Override
    public String getDescription() {
        return "落子于 (" + row + ", " + col + ")";
    }
}