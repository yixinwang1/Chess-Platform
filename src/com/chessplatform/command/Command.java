package com.chessplatform.command;

import com.chessplatform.core.Game;

public interface Command {
    boolean execute();
    void undo();
    String getDescription();
}