package com.chessplatform.platform;


import com.chessplatform.core.Game;
import com.chessplatform.record.ReplayController;
import com.chessplatform.ui.ConsoleUI;

public class ChessPlatform {
    private Game currentGame;
    private ReplayController replayController;
    private boolean isReplayMode;
    
    public ChessPlatform() {
        this.isReplayMode = false;
    }
    
    // 开始回放
    public void startReplay(Game game) {
        if (game == null || game.getGameRecorder() == null) {
            throw new IllegalArgumentException("无法开始回放：游戏或录像数据不存在");
        }
        
        this.currentGame = game;
        this.currentGame.setReplayMode(true);
        this.replayController = game.getGameRecorder().createReplayController();
        this.isReplayMode = true;
        
        // 重置到第一步
        replayController.goToStart();
        updateGameToReplayStep();
    }
    
    // 停止回放
    public void stopReplay() {
        if (isReplayMode && currentGame != null) {
            currentGame.setReplayMode(false);
            isReplayMode = false;
            replayController = null;
        }
    }
    
    // 回放控制
    public void replayNext() {
        if (isReplayMode && replayController != null) {
            if (replayController.nextStep()) {
                updateGameToReplayStep();
            }
        }
    }
    
    public void replayPrevious() {
        if (isReplayMode && replayController != null) {
            if (replayController.previousStep()) {
                updateGameToReplayStep();
            }
        }
    }
    
    public void replayGoTo(int step) {
        if (isReplayMode && replayController != null) {
            replayController.goToStep(step);
            updateGameToReplayStep();
        }
    }
    
    public void replayPlay() {
        if (isReplayMode && replayController != null) {
            replayController.play();
            // 可以启动一个线程来自动播放
            startAutoPlayback();
        }
    }
    
    public void replayPause() {
        if (isReplayMode && replayController != null) {
            replayController.pause();
        }
    }
    
    // 获取回放信息
    public String getReplayInfo() {
        if (isReplayMode && replayController != null) {
            return String.format("回放中: %s (第%d/%d步)", 
                replayController.getProgress(),
                replayController.getCurrentStep(),
                replayController.getTotalSteps());
        }
        return "非回放模式";
    }
    
    public boolean isReplayMode() {
        return isReplayMode;
    }
    
    public ReplayController getReplayController() {
        return replayController;
    }
    
    // 私有方法
    private void updateGameToReplayStep() {
        if (currentGame != null && replayController != null) {
            currentGame.setReplayStep(replayController.getCurrentStep());
        }
    }
    
    private void startAutoPlayback() {
        // 实现自动播放逻辑
        new Thread(() -> {
            while (replayController.isPlaying().get() && 
                   replayController.getCurrentStep() < replayController.getTotalSteps()) {
                try {
                    Thread.sleep(replayController.getPlaybackSpeed());
                    replayNext();
                    // 通知UI更新
                    // 这里需要观察者模式通知UI
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            replayController.pause();
        }).start();
    }

    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();
        ui.start();
    }
}