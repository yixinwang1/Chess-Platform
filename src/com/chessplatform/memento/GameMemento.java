// 修改 memento/GameMemento.java
package com.chessplatform.memento;

import com.chessplatform.core.Game;
import com.chessplatform.record.GameRecorder;  // 新增导入

import java.io.*;

public class GameMemento implements Serializable {
    private static final long serialVersionUID = 1L;
    private Game savedState;
    private GameRecorder gameRecorder;  // 新增：保存录像
    
    public GameMemento(Game game) {
        // 深度复制游戏状态
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(game);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            this.savedState = (Game) ois.readObject();
            
            // 保存录像
            if (game.getGameRecorder() != null) {
                this.gameRecorder = game.getGameRecorder();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("保存游戏状态失败: " + e.getMessage());
            throw new RuntimeException("保存游戏状态失败", e);
        }
    }
    
    public Game getSavedState() {
        return savedState;
    }
    
    public GameRecorder getGameRecorder() {
        return gameRecorder;
    }
    
    // 设置录像到恢复的游戏
    public void applyRecorderToGame(Game game) {
        if (gameRecorder != null && game != null) {
            game.setGameRecorder(gameRecorder);
        }
    }
}