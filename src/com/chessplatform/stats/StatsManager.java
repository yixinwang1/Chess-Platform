// stats/StatsManager.java
package com.chessplatform.stats;

import com.chessplatform.auth.UserManager;
import com.chessplatform.core.GameEvent;
import com.chessplatform.core.GameEventListener;
import com.chessplatform.core.events.*;
import com.chessplatform.model.Player;

import java.util.HashMap;
import java.util.Map;

public class StatsManager implements GameEventListener {
    private UserManager userManager;
    private boolean enabled;
    private Map<String, Integer> eventCounts;
    
    public StatsManager(UserManager userManager) {
        this.userManager = userManager;
        this.enabled = true;
        this.eventCounts = new HashMap<>();
        initializeEventCounts();
    }
    
    private void initializeEventCounts() {
        eventCounts.put("GameEndedEvent", 0);
        eventCounts.put("PlayerResignedEvent", 0);
        eventCounts.put("GameStartedEvent", 0);
        eventCounts.put("MoveMadeEvent", 0);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void onGameEvent(GameEvent event) {
        if (!enabled) return;
        
        String eventName = event.getClass().getSimpleName();
        eventCounts.put(eventName, eventCounts.getOrDefault(eventName, 0) + 1);
        
        if (event instanceof GameEndedEvent) {
            handleGameEnded((GameEndedEvent) event);
        } else if (event instanceof PlayerResignedEvent) {
            handlePlayerResigned((PlayerResignedEvent) event);
        }
    }
    
    private void handleGameEnded(GameEndedEvent event) {
        Player winner = event.getWinner();
        boolean isDraw = event.isDraw();
        
        if (isDraw) {
            updateDrawStats(event.getSource().getBlackPlayer(), 
                          event.getSource().getWhitePlayer());
            logStatUpdate("平局", event.getSource());
        } else if (winner != null) {
            Player loser = (winner == event.getSource().getBlackPlayer()) ?
                          event.getSource().getWhitePlayer() : event.getSource().getBlackPlayer();
            updateWinLossStats(winner, loser);
            logStatUpdate(winner.getName() + " 获胜", event.getSource());
        }
    }
    
    private void handlePlayerResigned(PlayerResignedEvent event) {
        updateWinLossStats(event.getWinner(), event.getResignedPlayer());
        logStatUpdate(event.getResignedPlayer().getName() + " 认输", event.getSource());
    }
    
    private void updateWinLossStats(Player winner, Player loser) {
        if (winner.getUser().isRegistered()) {
            userManager.updateUserStats(winner.getUser(), true, false);
        }
        if (loser.getUser().isRegistered()) {
            userManager.updateUserStats(loser.getUser(), false, false);
        }
    }
    
    private void updateDrawStats(Player player1, Player player2) {
        if (player1.getUser().isRegistered()) {
            userManager.updateUserStats(player1.getUser(), false, true);
        }
        if (player2.getUser().isRegistered()) {
            userManager.updateUserStats(player2.getUser(), false, true);
        }
    }
    
    private void logStatUpdate(String action, com.chessplatform.core.Game game) {
        if (enabled) {
            System.out.println("[战绩系统] " + action + " - 战绩已更新");
        }
    }
    
    // 添加 getStatsSummary 方法
    public String getStatsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("战绩统计系统状态:\n");
        sb.append("  • 状态: ").append(enabled ? "运行中" : "已关闭").append("\n");
        sb.append("  • 事件统计:\n");
        
        int totalEvents = eventCounts.values().stream().mapToInt(Integer::intValue).sum();
        sb.append("      - 总处理事件: ").append(totalEvents).append("\n");
        
        for (Map.Entry<String, Integer> entry : eventCounts.entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(String.format("      - %s: %d次\n", 
                    entry.getKey(), entry.getValue()));
            }
        }
        
        return sb.toString();
    }
    
    // 添加 resetStatistics 方法
    public void resetStatistics() {
        initializeEventCounts();
    }
    
    // 添加 getEventCounts 方法
    public Map<String, Integer> getEventCounts() {
        return new HashMap<>(eventCounts);
    }
}