// games/gomoku/Gomoku.java
package com.chessplatform.games.gomoku;

import com.chessplatform.ai.*;
import com.chessplatform.ai.mcts.*;
import com.chessplatform.ai.random.*;
import com.chessplatform.ai.rules.*;
import com.chessplatform.core.AIType;
import com.chessplatform.core.Game;
import com.chessplatform.core.GameMode;
import com.chessplatform.core.GameType;
import com.chessplatform.core.ReplayMode;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.*;
import com.chessplatform.recorder.GameRecorder;
import java.io.*;
import java.util.*;

public class Gomoku implements Game, Serializable {
    private static final long serialVersionUID = 1L;
    
    private Board board;
    private Player blackPlayer;
    private Player whitePlayer;
    private Player currentPlayer;
    private boolean gameOver;
    private Player winner;
    private Stack<Move> moveHistory;
    private int consecutivePasses;
    
    // 新增：录像和回放相关字段
    private GameRecorder gameRecorder;
    private ReplayMode replayMode;
    private int replayStep;
    
    // 新增AI相关字段
    private GameMode gameMode;
    private Map<Player, AIType> playerAITypes;
    private Map<AIType, AI> aiInstances;
    
    public Gomoku(int boardSize) {
        this.board = new Board(boardSize);
        this.blackPlayer = new Player("黑方", PieceColor.BLACK);
        this.whitePlayer = new Player("白方", PieceColor.WHITE);
        this.currentPlayer = blackPlayer;
        this.gameOver = false;
        this.winner = null;
        this.moveHistory = new Stack<>();
        this.consecutivePasses = 0;
        
        // 初始化录像和回放
        this.gameRecorder = new GameRecorder();
        this.replayMode = ReplayMode.NORMAL;
        this.replayStep = 0;
        
        // 记录初始状态
        gameRecorder.recordInitialState(board, currentPlayer);
        gameRecorder.addAnnotation("五子棋游戏开始");
        gameRecorder.addAnnotation("棋盘大小: " + boardSize + "x" + boardSize);

        // 新增AI相关初始化
        this.gameMode = GameMode.PLAYER_VS_PLAYER;
        this.playerAITypes = new HashMap<>();
        this.playerAITypes.put(blackPlayer, AIType.NONE);
        this.playerAITypes.put(whitePlayer, AIType.NONE);
        
        // 初始化AI实例
        this.aiInstances = new HashMap<>();
        initializeAIInstances();
    }
    
    @Override
    public boolean makeMove(int row, int col) {
        if (isReplayMode()) {
            return false;
        }
        
        if (!isValidMove(row, col) || gameOver) {
            return false;
        }

        // 如果是AI走棋，忽略手动输入
        if (isAIMove() && !isReplayMode()) {
            System.out.println("当前是AI走棋，请等待AI思考...");
            Point aiPoint = getAIMove();
            row = aiPoint.getX();
            col = aiPoint.getY();
        }
        
        // 落子
        board.setPiece(row, col, new Piece(currentPlayer.getColor()));
        Move move = new Move(currentPlayer, row, col);
        moveHistory.push(move);
        
        // 记录到录像
        recordMove(row, col);
        
        // 检查胜负
        if (checkWin(row, col)) {
            gameOver = true;
            winner = currentPlayer;
            gameRecorder.recordGameEnd(this);
            gameRecorder.addAnnotation(winner.getName() + " 五子连珠获胜！");
            return true;
        }
        
        // 检查平局
        if (isBoardFull()) {
            gameOver = true;
            winner = null;
            gameRecorder.recordGameEnd(this);
            gameRecorder.addAnnotation("棋盘已满，平局！");
            return true;
        }
        
        // 切换玩家
        switchPlayer();
        return true;
    }
    
    @Override
    public boolean pass() {
        if (isReplayMode()) {
            return false;
        }
        
        if (gameOver) return false;
        
        // 五子棋不允许pass，但可以记录
        gameRecorder.addAnnotation(currentPlayer.getName() + " 尝试虚着，五子棋不允许此操作");
        return false;
    }
    
    @Override
    public boolean resign(Player player) {
        if (isReplayMode()) {
            return false;
        }
        
        if (gameOver) return false;
        
        gameOver = true;
        winner = (player == blackPlayer) ? whitePlayer : blackPlayer;
        player.resign();
        
        Move move = Move.createResignMove(player);
        moveHistory.push(move);
        
        // 记录到录像
        recordResign(player);
        gameRecorder.recordGameEnd(this);
        gameRecorder.addAnnotation(player.getName() + " 认输，" + winner.getName() + " 获胜");
        
        return true;
    }
    
    @Override
    public boolean undo() {
        if (moveHistory.isEmpty() || gameOver || isReplayMode()) {
            return false;
        }
        
        Move lastMove = moveHistory.pop();
        if (lastMove.isNormalMove()) {
            board.clearPosition(lastMove.getRow(), lastMove.getCol());
            switchPlayer();
            
            // 记录悔棋
            gameRecorder.addAnnotation(currentPlayer.getName() + " 悔棋一步");
            return true;
        } else if (lastMove.isResign()) {
            gameOver = false;
            winner = null;
            lastMove.getPlayer().resign();
            gameRecorder.addAnnotation("撤销认输，游戏继续");
            return true;
        }
        return false;
    }
    
    private boolean checkWin(int row, int col) {
        PieceColor color = board.getPiece(row, col).getColor();
        
        int[][] directions = {
            {1, 0},   // 水平
            {0, 1},   // 垂直
            {1, 1},   // 对角线
            {1, -1}   // 反对角线
        };
        
        for (int[] dir : directions) {
            int count = 1;
            
            // 正向检查
            for (int i = 1; i < 5; i++) {
                int newRow = row + dir[0] * i;
                int newCol = col + dir[1] * i;
                if (!board.isValidPosition(newRow, newCol) || 
                    board.getPiece(newRow, newCol).getColor() != color) {
                    break;
                }
                count++;
            }
            
            // 反向检查
            for (int i = 1; i < 5; i++) {
                int newRow = row - dir[0] * i;
                int newCol = col - dir[1] * i;
                if (!board.isValidPosition(newRow, newCol) || 
                    board.getPiece(newRow, newCol).getColor() != color) {
                    break;
                }
                count++;
            }
            
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isBoardFull() {
        for (int i = 0; i < board.getSize(); i++) {
            for (int j = 0; j < board.getSize(); j++) {
                if (board.getPiece(i, j).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void switchPlayer() {
        currentPlayer = (currentPlayer == blackPlayer) ? whitePlayer : blackPlayer;
    }
    
    @Override
    public boolean isValidMove(int row, int col) {
        if (gameOver || isReplayMode()) return false;
        if (!board.isValidPosition(row, col)) return false;
        return board.isPositionEmpty(row, col);
    }
    
    @Override
    public boolean isGameOver() {
        return gameOver;
    }
    
    @Override
    public Player getWinner() {
        return winner;
    }
    
    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    
    @Override
    public Board getBoard() {
        return board;
    }
    
    @Override
    public GameType getGameType() {
        return GameType.GOMOKU;
    }
    
    @Override
    public String getGameStatus() {
        if (gameOver) {
            if (winner != null) {
                return "游戏结束! 获胜者: " + winner.getName();
            } else {
                return "游戏结束! 平局";
            }
        }
        return "当前玩家: " + currentPlayer.getName() + " (总步数: " + moveHistory.size() + ")";
    }
    
    @Override
    public int getMoveCount() {
        return moveHistory.size();
    }
    
    @Override
    public GameRecorder getGameRecorder() {
        return gameRecorder;
    }
    
    @Override
    public void setGameRecorder(GameRecorder recorder) {
        this.gameRecorder = recorder;
    }
    
    @Override
    public void recordMove(int row, int col) {
        if (gameRecorder != null && !isReplayMode()) {
            Move move = new Move(currentPlayer, row, col);
            gameRecorder.recordMove(move, board);
        }
    }
    
    @Override
    public void recordPass() {
        if (gameRecorder != null && !isReplayMode()) {
            Move move = Move.createPassMove(currentPlayer);
            gameRecorder.recordMove(move, board);
        }
    }
    
    @Override
    public void recordResign(Player player) {
        if (gameRecorder != null && !isReplayMode()) {
            Move move = Move.createResignMove(player);
            gameRecorder.recordMove(move, board);
        }
    }
    
    @Override
    public boolean isReplayMode() {
        return replayMode == ReplayMode.REPLAY;
    }
    
    @Override
    public void setReplayMode(boolean replayMode) {
        this.replayMode = replayMode ? ReplayMode.REPLAY : ReplayMode.NORMAL;
        if (replayMode) {
            this.replayStep = 0;
        }
    }
    
    @Override
    public void setReplayStep(int step) {
        if (isReplayMode()) {
            this.replayStep = Math.max(0, Math.min(step, gameRecorder.getTotalMoves()));
            updateBoardToReplayStep();
        }
    }
    
    @Override
    public Board getBoardAtStep(int step) {
        if (step >= 0 && step <= gameRecorder.getTotalMoves()) {
            // 重建棋盘到指定步数
            return reconstructBoard(step);
        }
        return null;
    }
    
    private void updateBoardToReplayStep() {
        // 根据replayStep重建棋盘
        board = reconstructBoard(replayStep);
        
        // 更新其他状态
        moveHistory.clear();
        gameOver = false;
        winner = null;
        currentPlayer = blackPlayer;
        
        List<Move> moves = gameRecorder.getMoveHistory();
        for (int i = 0; i < replayStep; i++) {
            Move move = moves.get(i);
            if (move.isNormalMove()) {
                board.setPiece(move.getRow(), move.getCol(), 
                              new Piece(move.getPlayer().getColor()));
                moveHistory.push(move);
                
                // 检查是否结束
                if (checkWin(move.getRow(), move.getCol())) {
                    gameOver = true;
                    winner = move.getPlayer();
                    break;
                }
                
                switchPlayer();
            }
        }
    }
    
    private Board reconstructBoard(int targetStep) {
        Board reconstructed = new Board(board.getSize());
        List<Move> moves = gameRecorder.getMoveHistory();
        
        for (int i = 0; i < targetStep && i < moves.size(); i++) {
            Move move = moves.get(i);
            if (move.isNormalMove()) {
                reconstructed.setPiece(move.getRow(), move.getCol(), 
                                      new Piece(move.getPlayer().getColor()));
            }
        }
        
        return reconstructed;
    }
    
    @Override
    public GameMemento saveToMemento() {
        return new GameMemento(this);
    }
    
    @Override
    public void restoreFromMemento(GameMemento memento) {
        Gomoku savedState = (Gomoku) memento.getSavedState();
        this.board = savedState.board.copy();
        this.currentPlayer = savedState.currentPlayer;
        this.gameOver = savedState.gameOver;
        this.winner = savedState.winner;
        this.moveHistory = new Stack<>();
        this.moveHistory.addAll(savedState.moveHistory);
        this.consecutivePasses = savedState.consecutivePasses;
        
        // 恢复录像
        if (memento.getGameRecorder() != null) {
            this.gameRecorder = memento.getGameRecorder();
        }
    }
    
    // 获取合法落子位置（用于界面提示）
    public java.util.List<Point> getValidMoves() {
        java.util.List<Point> validMoves = new java.util.ArrayList<>();
        int size = board.getSize();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (isValidMove(i, j)) {
                    validMoves.add(new Point(i, j));
                }
            }
        }
        
        return validMoves;
    }

    private void initializeAIInstances() {
        aiInstances.put(AIType.RANDOM, new RandomAI());
        aiInstances.put(AIType.RULE, new GomokuRuleAI());
        // 可以延迟加载高级AI
    }
    
    // 新增：设置游戏模式
    public void setGameMode(GameMode mode, AIType blackAI, AIType whiteAI) {
        this.gameMode = mode;
        this.playerAITypes.put(blackPlayer, blackAI);
        this.playerAITypes.put(whitePlayer, whiteAI);
        
        // 记录到录像
        if (gameRecorder != null) {
            gameRecorder.addAnnotation("设置游戏模式: " + mode.getDescription());
            if (blackAI != AIType.NONE) {
                gameRecorder.addAnnotation("黑方AI: " + blackAI.getDescription());
            }
            if (whiteAI != AIType.NONE) {
                gameRecorder.addAnnotation("白方AI: " + whiteAI.getDescription());
            }
        }
    }
    
    @Override
    public boolean isAIMove() {
        AIType aiType = playerAITypes.get(currentPlayer);
        return aiType != AIType.NONE;
    }   
    
    @Override
    public void setAITypeForPlayer(Player player, AIType aiType) {
        playerAITypes.put(player, aiType);
    
        // 记录到录像
        if (gameRecorder != null) {
            gameRecorder.addAnnotation(player.getName() + " AI类型设置为: " + 
                (aiType == null ? "无" : aiType.getDescription()));
        }
    }
    
    @Override
    public AIType getAITypeForPlayer(Player player) {
        return playerAITypes.get(player);
    }
    
    @Override
    public Game copy() {
        // 实现深拷贝
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Gomoku) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("复制游戏状态失败", e);
        }
    }
    
    // 新增：AI自动走棋方法
    public Point getAIMove() {
        AIType aiType = playerAITypes.get(currentPlayer);
        if (aiType == AIType.NONE) {
            return null;
        }
        
        AI ai = aiInstances.get(aiType);
        if (ai == null) {
            // 延迟加载AI
            ai = createAI(aiType);
            aiInstances.put(aiType, ai);
        }
        
        // 设置思考时间限制
        ai.setTimeLimit(2000); // 2秒
        
        // AI思考
        return ai.think(this);
    }
    
    private AI createAI(AIType aiType) {
        switch (aiType) {
            case RANDOM:
                return new RandomAI();
            case RULE:
                return new GomokuRuleAI();
            case MCTS:
                return new MCTSAI();
            default:
                throw new IllegalArgumentException("不支持的AI类型: " + aiType);
        }
    }

    @Override
    public PieceColor getPlayerColor(Player player) {
        if (player == blackPlayer) {
            return PieceColor.BLACK;
        } else if (player == whitePlayer) {
            return PieceColor.WHITE;
        }
        return PieceColor.EMPTY;
    }
}