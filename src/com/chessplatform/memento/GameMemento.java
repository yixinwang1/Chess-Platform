// memento/GameMemento.java
package com.chessplatform.memento;

import com.chessplatform.core.Game;
import java.io.*;

public class GameMemento implements Serializable {
    private static final long serialVersionUID = 1L;
    private Game savedState;
    
    public GameMemento(Game game) {
        // 深度复制游戏状态
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(game);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            this.savedState = (Game) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
            throw new RuntimeException("保存游戏状态失败", e);
        }
    }
    
    public Game getSavedState() {
        return savedState;
    }
}