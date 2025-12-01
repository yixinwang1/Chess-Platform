// command/ResignCommand.java
package com.chessplatform.command;

import com.chessplatform.core.Game;
import com.chessplatform.model.Player;

public class ResignCommand implements Command {
    private Game game;
    private Player player;
    private boolean wasGameOver;
    
    public ResignCommand(Game game, Player player) {
        this.game = game;
        this.player = player;
    }
    
    @Override
    public boolean execute() {
        wasGameOver = game.isGameOver();
        return game.resign(player);
    }
    
    @Override
    public void undo() {
        // 认输操作不可撤销
    }
    
    @Override
    public String getDescription() {
        return player.getName() + " 认输";
    }
}