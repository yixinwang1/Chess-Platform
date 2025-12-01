// util/FileUtil.java
package com.chessplatform.util;

import com.chessplatform.memento.GameMemento;

import java.io.*;

public class FileUtil {
    private static final String SAVE_DIRECTORY = "saves/";
    
    static {
        // 确保保存目录存在
        File dir = new File(SAVE_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public static void saveGame(GameMemento memento, String filename) throws IOException {
        String filepath = SAVE_DIRECTORY + filename;
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filepath))) {
            oos.writeObject(memento);
        }
    }
    
    public static GameMemento loadGame(String filename) 
            throws IOException, ClassNotFoundException {
        String filepath = SAVE_DIRECTORY + filename;
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filepath))) {
            return (GameMemento) ois.readObject();
        }
    }
    
    public static boolean saveFileExists(String filename) {
        return new File(SAVE_DIRECTORY + filename).exists();
    }
    
    public static String[] listSaveFiles() {
        File dir = new File(SAVE_DIRECTORY);
        return dir.list((d, name) -> name.endsWith(".save"));
    }
}