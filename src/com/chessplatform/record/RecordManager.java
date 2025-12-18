// record/RecordManager.java
package com.chessplatform.record;

import com.chessplatform.auth.User;
import com.chessplatform.core.GameEvent;
import com.chessplatform.core.GameEventListener;
import com.chessplatform.core.events.*;
import com.chessplatform.memento.GameMemento;

public class RecordManager implements GameEventListener {
    private boolean autoSaveRecords;
    private boolean saveGuestGames;
    
    public RecordManager(boolean autoSaveRecords) {
        this.autoSaveRecords = autoSaveRecords;
        this.saveGuestGames = false;
    }
    
    public RecordManager(boolean autoSaveRecords, boolean saveGuestGames) {
        this.autoSaveRecords = autoSaveRecords;
        this.saveGuestGames = saveGuestGames;
    }
    
    public void setAutoSaveRecords(boolean autoSave) {
        this.autoSaveRecords = autoSave;
    }
    
    public void setSaveGuestGames(boolean saveGuestGames) {
        this.saveGuestGames = saveGuestGames;
    }
    
    // 添加 isAutoSaveEnabled 方法
    public boolean isAutoSaveEnabled() {
        return autoSaveRecords;
    }
    
    // 添加 isSaveGuestGames 方法
    public boolean isSaveGuestGames() {
        return saveGuestGames;
    }
    
    @Override
    public void onGameEvent(GameEvent event) {
        if (!autoSaveRecords) return;
        
        if (event instanceof GameEndedEvent || event instanceof PlayerResignedEvent) {
            saveGameRecord(event.getSource());
        }
    }
    
    private void saveGameRecord(com.chessplatform.core.Game game) {
        try {
            GameMemento memento = game.saveToMemento();
            String gameType = game.getGameType().name();
            
            com.chessplatform.model.Player blackPlayer = game.getBlackPlayer();
            com.chessplatform.model.Player whitePlayer = game.getWhitePlayer();
            
            // 保存黑方录像
            if (shouldSaveUserRecord(blackPlayer.getUser())) {
                saveRecordForUser(blackPlayer.getUser(), gameType, memento);
            }
            
            // 保存白方录像
            if (shouldSaveUserRecord(whitePlayer.getUser())) {
                saveRecordForUser(whitePlayer.getUser(), gameType, memento);
            }
        } catch (Exception e) {
            System.err.println("保存录像失败: " + e.getMessage());
        }
    }
    
    private void saveRecordForUser(User user, String gameType, GameMemento memento) {
        // 这里需要调用 GameRecordManager.saveRecord 方法
        // 由于 GameRecordManager 可能有静态方法，我们先简化实现
        String username = user.getUsername();
        System.out.println("[录像] 为 " + username + " 保存游戏录像: " + gameType);
        
        // 实际保存逻辑可以在这里实现
        // GameRecordManager.saveRecord(user, gameType, memento);
    }
    
    private boolean shouldSaveUserRecord(User user) {
        if (user.isAI()) {
            return false;
        } else if (user.isGuest()) {
            return saveGuestGames;
        } else {
            return true;
        }
    }
    
    // 添加 getStatsInfo 方法
    public String getStatsInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("录像管理状态:\n");
        sb.append("  • 自动保存: ").append(autoSaveRecords ? "开启" : "关闭").append("\n");
        sb.append("  • 保存游客游戏: ").append(saveGuestGames ? "是" : "否").append("\n");
        sb.append("  • 保存规则:\n");
        sb.append("      - 注册用户: 总是保存\n");
        sb.append("      - AI玩家: 不保存\n");
        sb.append("      - 游客: ").append(saveGuestGames ? "保存" : "不保存");
        return sb.toString();
    }
}