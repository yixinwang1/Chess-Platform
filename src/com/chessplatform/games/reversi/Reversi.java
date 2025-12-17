// games/reversi/Reversi.java
package com.chessplatform.games.reversi;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.core.ReplayMode;
import com.chessplatform.model.*;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.recorder.GameRecorder;

import java.io.Serializable;
import java.util.*;

public class Reversi implements Game, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final int BOARD_SIZE = 8;
    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1},           {0, 1},
        {1, -1},  {1, 0},  {1, 1}
    };
    
    private Board board;
    private Player blackPlayer;
    private Player whitePlayer;
    private Player currentPlayer;
    private boolean gameOver;
    private Player winner;
    private Stack<Move> moveHistory;
    private int consecutivePasses;
    private List<Point> lastFlippedStones;
    
    // 录像和回放相关字段
    private GameRecorder gameRecorder;
    private ReplayMode replayMode;
    private int replayStep;
    
    public Reversi() {
        this.board = new Board(BOARD_SIZE);
        this.blackPlayer = new Player("黑方", PieceColor.BLACK);
        this.whitePlayer = new Player("白方", PieceColor.WHITE);
        this.currentPlayer = blackPlayer;
        this.gameOver = false;
        this.winner = null;
        this.moveHistory = new Stack<>();
        this.consecutivePasses = 0;
        this.lastFlippedStones = new ArrayList<>();
        
        // 初始化录像和回放
        this.gameRecorder = new GameRecorder();
        this.replayMode = ReplayMode.NORMAL;
        this.replayStep = 0;
        
        initializeBoard();
        
        // 记录初始状态
        gameRecorder.recordInitialState(board, currentPlayer);
        gameRecorder.addAnnotation("黑白棋游戏开始");
        gameRecorder.addAnnotation("棋盘大小: 8x8");
        gameRecorder.addAnnotation("初始布局: 中心4子对角摆放");
    }
    
    private void initializeBoard() {
        board.clear();
        
        int center = board.getSize() / 2 - 1;
        board.setPiece(center, center, new Piece(PieceColor.WHITE));
        board.setPiece(center, center + 1, new Piece(PieceColor.BLACK));
        board.setPiece(center + 1, center, new Piece(PieceColor.BLACK));
        board.setPiece(center + 1, center + 1, new Piece(PieceColor.WHITE));
    }
    
    @Override
    public boolean makeMove(int row, int col) {
        if (isReplayMode()) {
            return false;
        }
        
        if (!isValidMove(row, col) || gameOver) {
            return false;
        }
        
        // 执行落子
        board.setPiece(row, col, new Piece(currentPlayer.getColor()));
        Move move = new Move(currentPlayer, row, col);
        moveHistory.push(move);
        
        // 记录到录像
        recordMove(row, col);
        
        // 翻转对方棋子
        lastFlippedStones.clear();
        flipOpponentStones(row, col);
        
        consecutivePasses = 0;
        
        // 记录翻转信息
        if (!lastFlippedStones.isEmpty()) {
            gameRecorder.addAnnotation("翻转了 " + lastFlippedStones.size() + " 颗棋子");
        }
        
        // 检查游戏是否结束
        checkGameEnd();
        
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
        
        consecutivePasses++;
        Move move = Move.createPassMove(currentPlayer);
        moveHistory.push(move);
        
        // 记录到录像
        recordPass();
        gameRecorder.addAnnotation(currentPlayer.getName() + " 虚着");
        
        if (consecutivePasses >= 2) {
            gameOver = true;
            calculateWinner();
            gameRecorder.recordGameEnd(this);
        } else {
            switchPlayer();
        }
        return true;
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
            // 移除落子
            board.clearPosition(lastMove.getRow(), lastMove.getCol());
            
            // 恢复被翻转的棋子（简化实现）
            // 实际需要更复杂的逻辑来恢复状态
            
            switchPlayer();
            
            gameRecorder.addAnnotation(currentPlayer.getName() + " 悔棋一步");
            return true;
        } else if (lastMove.isPass()) {
            consecutivePasses--;
            switchPlayer();
            gameRecorder.addAnnotation("撤销虚着");
            return true;
        }
        return false;
    }
    
    private void flipOpponentStones(int row, int col) {
        PieceColor currentColor = currentPlayer.getColor();
        PieceColor opponentColor = currentColor.getOpposite();
        
        for (int[] dir : DIRECTIONS) {
            List<Point> toFlip = new ArrayList<>();
            
            int r = row + dir[0];
            int c = col + dir[1];
            
            while (board.isValidPosition(r, c)) {
                Piece piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    break;
                }
                
                if (piece.getColor() == opponentColor) {
                    toFlip.add(new Point(r, c));
                } else if (piece.getColor() == currentColor) {
                    if (!toFlip.isEmpty()) {
                        for (Point p : toFlip) {
                            board.setPiece(p.getX(), p.getY(), new Piece(currentColor));
                            lastFlippedStones.add(p);
                        }
                    }
                    break;
                }
                
                r += dir[0];
                c += dir[1];
            }
        }
    }
    
    private void checkGameEnd() {
        // 检查当前玩家是否有合法落子
        if (!hasValidMoves()) {
            // 当前玩家无合法落子，自动pass
            pass();
        }
        
        // 检查双方是否都无合法落子
        boolean blackHasMove = playerHasMove(blackPlayer);
        boolean whiteHasMove = playerHasMove(whitePlayer);
        
        if (!blackHasMove && !whiteHasMove) {
            gameOver = true;
            calculateWinner();
            gameRecorder.recordGameEnd(this);
        }
    }
    
    private boolean hasValidMoves() {
        int size = board.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (isValidMove(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean playerHasMove(Player player) {
        Player savedPlayer = currentPlayer;
        currentPlayer = player;
        
        boolean hasMove = hasValidMoves();
        
        currentPlayer = savedPlayer;
        return hasMove;
    }
    
    private void calculateWinner() {
        int blackCount = 0;
        int whiteCount = 0;
        
        int size = board.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Piece piece = board.getPiece(i, j);
                if (piece.getColor() == PieceColor.BLACK) {
                    blackCount++;
                } else if (piece.getColor() == PieceColor.WHITE) {
                    whiteCount++;
                }
            }
        }
        
        if (blackCount > whiteCount) {
            winner = blackPlayer;
            gameRecorder.addAnnotation("黑方胜: " + blackCount + " vs " + whiteCount);
        } else if (whiteCount > blackCount) {
            winner = whitePlayer;
            gameRecorder.addAnnotation("白方胜: " + whiteCount + " vs " + blackCount);
        } else {
            winner = null;
            gameRecorder.addAnnotation("平局: " + blackCount + " vs " + whiteCount);
        }
    }
    
    private void switchPlayer() {
        currentPlayer = (currentPlayer == blackPlayer) ? whitePlayer : blackPlayer;
    }
    
    @Override
    public boolean isValidMove(int row, int col) {
        if (gameOver || isReplayMode()) return false;
        if (!board.isValidPosition(row, col)) return false;
        if (!board.isPositionEmpty(row, col)) return false;
        
        PieceColor currentColor = currentPlayer.getColor();
        PieceColor opponentColor = currentColor.getOpposite();
        
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0];
            int c = col + dir[1];
            
            boolean foundOpponent = false;
            
            while (board.isValidPosition(r, c)) {
                Piece piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    break;
                }
                
                if (piece.getColor() == opponentColor) {
                    foundOpponent = true;
                } else if (piece.getColor() == currentColor) {
                    if (foundOpponent) {
                        return true;
                    }
                    break;
                }
                
                r += dir[0];
                c += dir[1];
            }
        }
        
        return false;
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
        return GameType.REVERSI;
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
        
        Map<PieceColor, Integer> counts = getPieceCount();
        return "当前玩家: " + currentPlayer.getName() + 
               " (黑子:" + counts.get(PieceColor.BLACK) + 
               " 白子:" + counts.get(PieceColor.WHITE) + 
               "，总步数: " + moveHistory.size() + ")";
    }
    
    @Override
    public int getMoveCount() {
        return moveHistory.size();
    }
    
    // 获取棋子统计
    public Map<PieceColor, Integer> getPieceCount() {
        Map<PieceColor, Integer> counts = new HashMap<>();
        counts.put(PieceColor.BLACK, 0);
        counts.put(PieceColor.WHITE, 0);
        
        int size = board.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Piece piece = board.getPiece(i, j);
                if (!piece.isEmpty()) {
                    counts.put(piece.getColor(), counts.get(piece.getColor()) + 1);
                }
            }
        }
        
        return counts;
    }
    
    // 获取合法落子位置
    public List<Point> getValidMoves() {
        List<Point> validMoves = new ArrayList<>();
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
            return reconstructBoard(step);
        }
        return null;
    }
    
    private void updateBoardToReplayStep() {
        // 重建棋盘到指定步数
        board = reconstructBoard(replayStep);
        
        // 更新其他状态
        moveHistory.clear();
        gameOver = false;
        winner = null;
        currentPlayer = blackPlayer;
        consecutivePasses = 0;
        lastFlippedStones.clear();
        
        List<Move> moves = gameRecorder.getMoveHistory();
        for (int i = 0; i < replayStep; i++) {
            Move move = moves.get(i);
            if (move.isNormalMove()) {
                board.setPiece(move.getRow(), move.getCol(), 
                              new Piece(move.getPlayer().getColor()));
                flipOpponentStonesForReplay(move.getRow(), move.getCol(), 
                                          move.getPlayer().getColor());
                moveHistory.push(move);
                switchPlayer();
            } else if (move.isPass()) {
                consecutivePasses++;
                if (consecutivePasses >= 2) {
                    gameOver = true;
                    calculateWinner();
                    break;
                }
                switchPlayer();
            }
        }
    }
    
    private void flipOpponentStonesForReplay(int row, int col, PieceColor color) {
        PieceColor opponentColor = color.getOpposite();
        
        for (int[] dir : DIRECTIONS) {
            List<Point> toFlip = new ArrayList<>();
            
            int r = row + dir[0];
            int c = col + dir[1];
            
            while (board.isValidPosition(r, c)) {
                Piece piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    break;
                }
                
                if (piece.getColor() == opponentColor) {
                    toFlip.add(new Point(r, c));
                } else if (piece.getColor() == color) {
                    if (!toFlip.isEmpty()) {
                        for (Point p : toFlip) {
                            board.setPiece(p.getX(), p.getY(), new Piece(color));
                        }
                    }
                    break;
                }
                
                r += dir[0];
                c += dir[1];
            }
        }
    }
    
    private Board reconstructBoard(int targetStep) {
        Board reconstructed = new Board(BOARD_SIZE);
        
        // 设置初始布局
        int center = BOARD_SIZE / 2 - 1;
        reconstructed.setPiece(center, center, new Piece(PieceColor.WHITE));
        reconstructed.setPiece(center, center + 1, new Piece(PieceColor.BLACK));
        reconstructed.setPiece(center + 1, center, new Piece(PieceColor.BLACK));
        reconstructed.setPiece(center + 1, center + 1, new Piece(PieceColor.WHITE));
        
        // 执行前targetStep步
        List<Move> moves = gameRecorder.getMoveHistory();
        Player replayPlayer = blackPlayer;
        
        for (int i = 0; i < targetStep && i < moves.size(); i++) {
            Move move = moves.get(i);
            if (move.isNormalMove()) {
                reconstructed.setPiece(move.getRow(), move.getCol(), 
                                      new Piece(move.getPlayer().getColor()));
                
                // 翻转棋子
                flipForReconstruction(reconstructed, move.getRow(), move.getCol(), 
                                    move.getPlayer().getColor());
                
                replayPlayer = (replayPlayer == blackPlayer) ? whitePlayer : blackPlayer;
            }
        }
        
        return reconstructed;
    }
    
    private void flipForReconstruction(Board board, int row, int col, PieceColor color) {
        PieceColor opponentColor = color.getOpposite();
        
        for (int[] dir : DIRECTIONS) {
            List<Point> toFlip = new ArrayList<>();
            
            int r = row + dir[0];
            int c = col + dir[1];
            
            while (board.isValidPosition(r, c)) {
                Piece piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    break;
                }
                
                if (piece.getColor() == opponentColor) {
                    toFlip.add(new Point(r, c));
                } else if (piece.getColor() == color) {
                    if (!toFlip.isEmpty()) {
                        for (Point p : toFlip) {
                            board.setPiece(p.getX(), p.getY(), new Piece(color));
                        }
                    }
                    break;
                }
                
                r += dir[0];
                c += dir[1];
            }
        }
    }
    
    @Override
    public GameMemento saveToMemento() {
        return new GameMemento(this);
    }
    
    @Override
    public void restoreFromMemento(GameMemento memento) {
        Reversi savedState = (Reversi) memento.getSavedState();
        this.board = savedState.board.copy();
        this.currentPlayer = savedState.currentPlayer;
        this.gameOver = savedState.gameOver;
        this.winner = savedState.winner;
        this.moveHistory = new Stack<>();
        this.moveHistory.addAll(savedState.moveHistory);
        this.consecutivePasses = savedState.consecutivePasses;
        this.lastFlippedStones = new ArrayList<>(savedState.lastFlippedStones);
        
        // 恢复录像
        if (memento.getGameRecorder() != null) {
            this.gameRecorder = memento.getGameRecorder();
        }
    }
}