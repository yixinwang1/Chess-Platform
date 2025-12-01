// command/SaveCommand.java
package com.chessplatform.command;

import com.chessplatform.core.Game;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.util.FileUtil;

import java.io.IOException;

public class SaveCommand implements Command {
    private Game game;
    private String filename;
    private GameMemento savedState;
    
    public SaveCommand(Game game, String filename) {
        this.game = game;
        this.filename = filename;
    }
    
    @Override
    public boolean execute() {
        try {
            savedState = game.saveToMemento();
            FileUtil.saveGame(savedState, filename);
            return true;
        } catch (IOException e) {
            System.err.println("保存游戏失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void undo() {
        // 保存操作不可撤销
    }
    
    @Override
    public String getDescription() {
        return "保存游戏到文件: " + filename;
    }
}