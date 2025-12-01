// command/LoadCommand.java
package com.chessplatform.command;

import com.chessplatform.core.Game;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.util.FileUtil;

import java.io.IOException;

public class LoadCommand implements Command {
    private Game game;
    private String filename;
    private GameMemento previousState;
    
    public LoadCommand(Game game, String filename) {
        this.game = game;
        this.filename = filename;
    }
    
    @Override
    public boolean execute() {
        try {
            previousState = game.saveToMemento();
            GameMemento loadedState = FileUtil.loadGame(filename);
            game.restoreFromMemento(loadedState);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("加载游戏失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void undo() {
        if (previousState != null) {
            game.restoreFromMemento(previousState);
        }
    }
    
    @Override
    public String getDescription() {
        return "从文件加载游戏: " + filename;
    }
}