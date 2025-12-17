package com.chessplatform.recorder;

import com.chessplatform.model.Board;
import com.chessplatform.model.Move;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReplayController {
    private GameRecorder recorder;
    private int currentStep;
    private AtomicBoolean isPlaying;  // 改为AtomicBoolean
    private int playbackSpeed;
    private Thread playbackThread;
    
    public ReplayController(GameRecorder recorder) {
        this.recorder = recorder;
        this.currentStep = 0;
        this.isPlaying = new AtomicBoolean(false);
        this.playbackSpeed = 1000;
        this.playbackThread = null;
    }
    
    public void play() {
        if (isPlaying.get()) {
            return;  // 已经在播放
        }
        
        isPlaying.set(true);
        
        // 创建播放线程
        playbackThread = new Thread(() -> {
            while (isPlaying.get() && currentStep < recorder.getTotalMoves()) {
                try {
                    Thread.sleep(playbackSpeed);
                    if (isPlaying.get()) {
                        currentStep++;
                        
                        // 通知UI更新（需要回调机制）
                        if (playbackListener != null) {
                            playbackListener.onStepChanged(currentStep);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            isPlaying.set(false);
        });
        
        playbackThread.setDaemon(true);  // 设置为守护线程
        playbackThread.start();
    }
    
    public void pause() {
        isPlaying.set(false);
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
    }
    
    public void stop() {
        pause();
        currentStep = 0;
    }
    
    public boolean nextStep() {
        if (currentStep < recorder.getTotalMoves()) {
            currentStep++;
            return true;
        }
        return false;
    }
    
    public boolean previousStep() {
        if (currentStep > 0) {
            currentStep--;
            return true;
        }
        return false;
    }
    
    public void goToStep(int step) {
        if (step >= 0 && step <= recorder.getTotalMoves()) {
            currentStep = step;
        }
    }
    
    public void goToStart() {
        currentStep = 0;
    }
    
    public void goToEnd() {
        currentStep = recorder.getTotalMoves();
    }
    
    // 获取当前状态
    public Move getCurrentMove() {
        if (currentStep > 0 && currentStep <= recorder.getTotalMoves()) {
            return recorder.getMove(currentStep - 1);
        }
        return null;
    }
    
    public Board getCurrentBoard() {
        // 如果有保存的快照，使用快照
        Board snapshot = recorder.getBoardAtMove(currentStep);
        if (snapshot != null) {
            return snapshot;
        }
        
        // 否则需要重建棋盘（通过回放走法）
        return reconstructBoard(currentStep);
    }
    
    public int getCurrentStep() {
        return currentStep;
    }
    
    public int getTotalSteps() {
        return recorder.getTotalMoves();
    }
    
    public AtomicBoolean isPlaying() {
        return isPlaying;
    }
    
    public int getPlaybackSpeed() {
        return playbackSpeed;
    }
    
    public void setPlaybackSpeed(int milliseconds) {
        this.playbackSpeed = Math.max(100, Math.min(10000, milliseconds));
    }
    
    public String getProgress() {
        return String.format("%d / %d", currentStep, recorder.getTotalMoves());
    }
    
    public float getProgressPercentage() {
        if (recorder.getTotalMoves() == 0) return 0;
        return (float) currentStep / recorder.getTotalMoves();
    }
    
    // 私有方法：通过回放走法重建棋盘
    private Board reconstructBoard(int targetStep) {
        // 这里需要游戏的具体实现来重建棋盘
        // 实际实现可能需要游戏类的配合
        return null; // 简化实现
    }
    
    // 添加回调接口
    public interface PlaybackListener {
        void onStepChanged(int currentStep);
    }
    
    private PlaybackListener playbackListener;
    
    public void setPlaybackListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }
}