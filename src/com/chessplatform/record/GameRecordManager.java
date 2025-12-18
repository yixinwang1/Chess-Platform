// record/GameRecordManager.java
package com.chessplatform.record;

import com.chessplatform.auth.User;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.util.FileUtil;
import java.io.*;
import java.util.*;

public class GameRecordManager {
    private static final String RECORDS_DIR = "records/";
    
    public static void saveRecord(User user, String gameType, GameMemento memento) {
        if (user.isGuest()) {
            return; // 不保存游客的录像
        }
        
        String userDir = RECORDS_DIR + user.getUsername() + "/";
        File dir = new File(userDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = userDir + gameType + "_" + timestamp + ".record";
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(new GameRecord(user.getUsername(), gameType, timestamp, memento));
        } catch (IOException e) {
            System.err.println("保存录像失败: " + e.getMessage());
        }
    }
    
    public static List<GameRecord> getUserRecords(String username) {
        List<GameRecord> records = new ArrayList<>();
        String userDir = RECORDS_DIR + username + "/";
        File dir = new File(userDir);
        
        if (!dir.exists()) {
            return records;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".record"));
        if (files != null) {
            for (File file : files) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    records.add((GameRecord) ois.readObject());
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("加载录像失败: " + e.getLocalizedMessage());
                }
            }
        }
        
        return records;
    }
}
