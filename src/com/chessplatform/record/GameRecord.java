package com.chessplatform.record;

import com.chessplatform.model.Move;
import com.chessplatform.core.Game;
import com.chessplatform.model.Board;
import com.chessplatform.model.Player;
import com.chessplatform.memento.GameMemento;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameRecord implements Serializable {
    private String username;
    private String gameType;
    private String timestamp;
    private GameMemento memento;

    public GameRecord(String username, String gameType, String timestamp, GameMemento memento) {
        this.username = username;
        this.gameType = gameType;
        this.timestamp = timestamp;
        this.memento = memento;
    }
}