// games/reversi/Reversi.java
package com.chessplatform.games.reversi;

import com.chessplatform.core.*;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.*;
import com.chessplatform.record.GameRecorder;
import java.io.Serializable;
import java.util.*;

public class Reversi extends Subject implements Game, Serializable {
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

    // 添加AI相关字段
    private GameMode gameMode;
    private Map<Player, AIType> playerAITypes;
    
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

        // AI初始化
        this.gameMode = GameMode.PLAYER_VS_PLAYER;
        this.playerAITypes = new HashMap<>();
        this.playerAITypes.put(blackPlayer, AIType.NONE);
        this.playerAITypes.put(whitePlayer, AIType.NONE);
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

    // 1. 获取玩家颜色
    @Override
    public PieceColor getPlayerColor(Player player) {
        if (player == blackPlayer) return PieceColor.BLACK;
        if (player == whitePlayer) return PieceColor.WHITE;
        return PieceColor.EMPTY;
    }

    // 2. 判断是否为AI走棋
    @Override
    public boolean isAIMove() {
        if (currentPlayer == null) return false;
        AIType aiType = playerAITypes.get(currentPlayer);
        return aiType != null && aiType != AIType.NONE;
    }

    // 3. 设置玩家AI类型
    @Override
    public void setAITypeForPlayer(Player player, AIType aiType) {
        playerAITypes.put(player, aiType);
        if (gameRecorder != null) {
            gameRecorder.addAnnotation(player.getName() + " AI类型设置为: " + 
                aiType.getDescription());
        }
    }

    // 4. 获取玩家AI类型
    @Override
    public AIType getAITypeForPlayer(Player player) {
        return playerAITypes.getOrDefault(player, AIType.NONE);
    }

    // 5. 游戏复制（简化版）
    @Override
    public Game copy() {
        // 创建新实例
        Reversi copy = new Reversi();
        
        // 复制棋盘
        copy.board = this.board.copy();
        
        // 复制玩家状态
        copy.currentPlayer = (this.currentPlayer == this.blackPlayer) ? 
                        copy.blackPlayer : copy.whitePlayer;
        copy.gameOver = this.gameOver;
        
        if (this.winner == this.blackPlayer) {
            copy.winner = copy.blackPlayer;
        } else if (this.winner == this.whitePlayer) {
            copy.winner = copy.whitePlayer;
        } else {
            copy.winner = null;
        }
        
        // 复制走棋历史
        copy.moveHistory.clear();
        copy.moveHistory.addAll(this.moveHistory);
        
        copy.consecutivePasses = this.consecutivePasses;
        
        // 复制最近翻转的棋子
        copy.lastFlippedStones = new ArrayList<>(this.lastFlippedStones);
        
        // 复制AI设置
        copy.gameMode = this.gameMode;
        copy.playerAITypes = new HashMap<>();
        copy.playerAITypes.put(copy.blackPlayer, 
            this.playerAITypes.getOrDefault(this.blackPlayer, AIType.NONE));
        copy.playerAITypes.put(copy.whitePlayer, 
            this.playerAITypes.getOrDefault(this.whitePlayer, AIType.NONE));
        
        // 复制录像记录器状态（重要）
        if (this.gameRecorder != null) {
            copy.gameRecorder = this.gameRecorder;
        }
        
        return copy;
    }

    // 6. AI走棋方法
    public Point getAIMove() {
        if (!isAIMove()) {
            return null;
        }
        
        AIType aiType = playerAITypes.get(currentPlayer);
        if (aiType == AIType.NONE) {
            return null;
        }
        
        // 获取所有合法落子位置
        List<Point> validMoves = getValidMoves();
        
        if (validMoves.isEmpty()) {
            // 没有合法落子位置
            return null;
        }
        
        Random random = new Random();
        
        switch (aiType) {
            case RANDOM:
                // 随机AI：随机选择一个合法位置
                return validMoves.get(random.nextInt(validMoves.size()));
                
            case RULE:
                // 规则AI：基于黑白棋策略
                return getRuleBasedMove(validMoves);
                
            case MCTS:
            case ADVANCED:
                // 高级AI：使用评估函数
                return getAdvancedMove(validMoves);
                
            default:
                // 默认随机
                return validMoves.get(random.nextInt(validMoves.size()));
        }
    }

    // 8. 规则AI（黑白棋专用策略）
    private Point getRuleBasedMove(List<Point> validMoves) {
        if (validMoves.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        
        // 黑白棋策略优先级：
        // 1. 角落位置（最高价值）
        // 2. 边缘位置（高价值）
        // 3. 翻转数量多的位置
        // 4. 避免给对手角落机会的位置
        
        Map<Point, Integer> scores = new HashMap<>();
        
        for (Point move : validMoves) {
            int score = evaluateMoveForReversi(move, currentPlayer.getColor());
            scores.put(move, score);
        }
        
        // 选择最高分的位置
        int maxScore = Integer.MIN_VALUE;
        List<Point> bestMoves = new ArrayList<>();
        
        for (Map.Entry<Point, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestMoves.clear();
                bestMoves.add(entry.getKey());
            } else if (entry.getValue() == maxScore) {
                bestMoves.add(entry.getKey());
            }
        }
        
        // 如果多个位置分数相同，随机选择一个
        return bestMoves.get(random.nextInt(bestMoves.size()));
    }

    // 9. 高级AI（使用更复杂的评估函数）
    private Point getAdvancedMove(List<Point> validMoves) {
        if (validMoves.isEmpty()) {
            return null;
        }
        
        // 如果合法落子较少，使用更精确的评估
        if (validMoves.size() <= 5) {
            // 可以向前看几步
            return getLookAheadMove(validMoves, 2); // 向前看2步
        }
        
        // 否则使用规则AI
        return getRuleBasedMove(validMoves);
    }

    // 10. 向前看搜索（简化版）
    private Point getLookAheadMove(List<Point> validMoves, int depth) {
        if (validMoves.isEmpty() || depth <= 0) {
            return null;
        }
        
        Map<Point, Integer> scores = new HashMap<>();
        PieceColor aiColor = currentPlayer.getColor();
        
        for (Point move : validMoves) {
            // 模拟这一步
            Reversi simulated = (Reversi) this.copy();
            simulated.makeMove(move.getX(), move.getY());
            
            // 评估局面
            int score = evaluateBoardPosition(simulated, aiColor);
            
            // 如果还有深度，可以考虑对手的最佳回应
            if (depth > 1) {
                List<Point> opponentMoves = simulated.getValidMoves();
                if (!opponentMoves.isEmpty()) {
                    // 假设对手会选择对我们最不利的走法
                    int opponentBestScore = Integer.MIN_VALUE;
                    for (Point oppMove : opponentMoves) {
                        Reversi oppSimulated = (Reversi) simulated.copy();
                        oppSimulated.makeMove(oppMove.getX(), oppMove.getY());
                        int oppScore = evaluateBoardPosition(oppSimulated, aiColor);
                        if (oppScore > opponentBestScore) {
                            opponentBestScore = oppScore;
                        }
                    }
                    // 从我们的得分中减去对手的最佳得分
                    score -= opponentBestScore / 2;
                }
            }
            
            scores.put(move, score);
        }
        
        // 选择最高分
        int maxScore = Integer.MIN_VALUE;
        Point bestMove = null;
        
        for (Map.Entry<Point, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestMove = entry.getKey();
            }
        }
        
        return bestMove;
    }

    // 11. 黑白棋落子评估函数
    private int evaluateMoveForReversi(Point move, PieceColor aiColor) {
        int score = 0;
        int x = move.getX();
        int y = move.getY();
        int boardSize = board.getSize();
        
        // 1. 位置价值表（黑白棋标准权重）
        // 角落最高，边缘其次，中心最差
        
        // 角落位置（最高价值 +100）
        if ((x == 0 && y == 0) || (x == 0 && y == boardSize-1) ||
            (x == boardSize-1 && y == 0) || (x == boardSize-1 && y == boardSize-1)) {
            score += 100;
        }
        // 危险位置（靠近角落但可能让对手得角，负价值）
        else if ((x == 1 && y == 0) || (x == 0 && y == 1) || 
                (x == 1 && y == 1) || // 靠近角落的C位
                (x == 1 && y == boardSize-1) || (x == 0 && y == boardSize-2) ||
                (x == 1 && y == boardSize-2) ||
                (x == boardSize-1 && y == 1) || (x == boardSize-2 && y == 0) ||
                (x == boardSize-2 && y == 1) ||
                (x == boardSize-1 && y == boardSize-2) || (x == boardSize-2 && y == boardSize-1) ||
                (x == boardSize-2 && y == boardSize-2)) {
            score -= 50;
        }
        // 边缘位置（高价值 +20）
        else if (x == 0 || x == boardSize-1 || y == 0 || y == boardSize-1) {
            score += 20;
        }
        // 内部边缘（中等价值 +5）
        else if (x == 1 || x == boardSize-2 || y == 1 || y == boardSize-2) {
            score += 5;
        }
        // 中心位置（最低价值 +1）
        else {
            score += 1;
        }
        
        // 2. 翻转数量（越多越好）
        int flipCount = countPotentialFlips(move.getX(), move.getY(), aiColor);
        score += flipCount * 3; // 每个翻转的棋子值3分
        
        // 3. 行动力（让对手选择少的位置更好）
        int opponentMobility = estimateOpponentMobilityAfterMove(move, aiColor);
        score -= opponentMobility * 2; // 对手的行动力越少越好
        
        // 4. 稳定性（角落是否安全）
        if (isMoveSecuringCorner(move, aiColor)) {
            score += 30;
        }
        
        // 5. 棋盘控制（早期控制边缘，晚期控制数量）
        Map<PieceColor, Integer> pieceCounts = getPieceCount();
        int totalPieces = pieceCounts.get(PieceColor.BLACK) + pieceCounts.get(PieceColor.WHITE);
        
        if (totalPieces < 20) { // 早期游戏
            // 优先角落和边缘
            score += evaluateEarlyGamePosition(move, aiColor);
        } else if (totalPieces < 50) { // 中期游戏
            // 平衡发展和控制
            score += evaluateMidGamePosition(move, aiColor);
        } else { // 晚期游戏
            // 最大化翻转数量
            score += flipCount * 5;
        }
        
        return score;
    }

    // 12. 评估棋盘整体局面
    private int evaluateBoardPosition(Reversi game, PieceColor aiColor) {
        int score = 0;
        
        // 1. 棋子数量差
        Map<PieceColor, Integer> counts = game.getPieceCount();
        int aiCount = counts.get(aiColor);
        int oppCount = counts.get(aiColor.getOpposite());
        score += (aiCount - oppCount) * 10;
        
        // 2. 角落控制
        score += evaluateCornerControl(game, aiColor) * 50;
        
        // 3. 行动力（当前玩家的合法落子数）
        List<Point> aiMoves = game.getValidMoves();
        score += aiMoves.size() * 5;
        
        // 4. 边缘控制
        score += evaluateEdgeControl(game, aiColor) * 10;
        
        // 5. 稳定性（不会被翻转的棋子）
        score += evaluateStability(game, aiColor) * 20;
        
        return score;
    }

    // 13. 计算可能翻转的棋子数量
    private int countPotentialFlips(int row, int col, PieceColor color) {
        int totalFlips = 0;
        PieceColor opponentColor = color.getOpposite();
        
        for (int[] dir : DIRECTIONS) {
            int flipsInDirection = 0;
            int r = row + dir[0];
            int c = col + dir[1];
            
            // 查找可以翻转的棋子
            while (board.isValidPosition(r, c)) {
                Piece piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    break; // 遇到空位，不能翻转
                }
                
                if (piece.getColor() == opponentColor) {
                    flipsInDirection++;
                } else if (piece.getColor() == color) {
                    // 遇到己方棋子，可以翻转中间的对手棋子
                    totalFlips += flipsInDirection;
                    break;
                }
                
                r += dir[0];
                c += dir[1];
            }
        }
        
        return totalFlips;
    }

    // 14. 估计对手的行动力
    private int estimateOpponentMobilityAfterMove(Point move, PieceColor aiColor) {
        // 创建临时棋盘模拟
        Reversi simulated = (Reversi) this.copy();
        simulated.makeMove(move.getX(), move.getY());
        
        // 计算对手的合法落子数
        List<Point> opponentMoves = simulated.getValidMoves();
        return opponentMoves.size();
    }

    // 15. 检查是否保护了角落
    private boolean isMoveSecuringCorner(Point move, PieceColor aiColor) {
        int boardSize = board.getSize();
        
        // 检查四个角落
        int[][] corners = {{0,0}, {0,boardSize-1}, {boardSize-1,0}, {boardSize-1,boardSize-1}};
        
        for (int[] corner : corners) {
            Piece cornerPiece = board.getPiece(corner[0], corner[1]);
            if (cornerPiece.getColor() == aiColor) {
                // 如果角落已经是自己的，检查这个走法是否加强了角落的防守
                if (isAdjacentToCorner(move, corner[0], corner[1])) {
                    // 占据角落旁边的位置可以加强防守
                    return true;
                }
            }
        }
        
        return false;
    }

    // 16. 检查是否与角落相邻
    private boolean isAdjacentToCorner(Point move, int cornerX, int cornerY) {
        return Math.abs(move.getX() - cornerX) <= 1 && 
            Math.abs(move.getY() - cornerY) <= 1;
    }

    // 17. 评估角落控制
    private int evaluateCornerControl(Reversi game, PieceColor aiColor) {
        int score = 0;
        int boardSize = game.getBoard().getSize();
        Board board = game.getBoard();
        
        int[][] corners = {{0,0}, {0,boardSize-1}, {boardSize-1,0}, {boardSize-1,boardSize-1}};
        
        for (int[] corner : corners) {
            Piece piece = board.getPiece(corner[0], corner[1]);
            if (piece.getColor() == aiColor) {
                score += 1; // 占据一个角落
            } else if (piece.getColor() == aiColor.getOpposite()) {
                score -= 1; // 对手占据一个角落
            }
            // 空角落不计分
        }
        
        return score;
    }

    // 18. 评估边缘控制
    private int evaluateEdgeControl(Reversi game, PieceColor aiColor) {
        int score = 0;
        int boardSize = game.getBoard().getSize();
        Board board = game.getBoard();
        
        // 检查四条边（不包括角落）
        for (int i = 1; i < boardSize-1; i++) {
            // 上边
            if (board.getPiece(0, i).getColor() == aiColor) score++;
            // 下边
            if (board.getPiece(boardSize-1, i).getColor() == aiColor) score++;
            // 左边
            if (board.getPiece(i, 0).getColor() == aiColor) score++;
            // 右边
            if (board.getPiece(i, boardSize-1).getColor() == aiColor) score++;
        }
        
        return score;
    }

    // 19. 评估稳定性（简化版）
    private int evaluateStability(Reversi game, PieceColor aiColor) {
        // 简化实现：计算角落和边缘的稳定棋子
        int stablePieces = 0;
        int boardSize = game.getBoard().getSize();
        Board board = game.getBoard();
        
        // 角落总是稳定的
        int[][] corners = {{0,0}, {0,boardSize-1}, {boardSize-1,0}, {boardSize-1,boardSize-1}};
        for (int[] corner : corners) {
            if (board.getPiece(corner[0], corner[1]).getColor() == aiColor) {
                stablePieces++;
            }
        }
        
        return stablePieces;
    }

    // 20. 早期游戏评估
    private int evaluateEarlyGamePosition(Point move, PieceColor aiColor) {
        int score = 0;
        int boardSize = board.getSize();
        
        // 早期游戏：避免中心，优先边缘
        if (move.getX() > 1 && move.getX() < boardSize-2 && 
            move.getY() > 1 && move.getY() < boardSize-2) {
            score -= 10; // 早期避免中心
        }
        
        return score;
    }

    // 21. 中期游戏评估
    private int evaluateMidGamePosition(Point move, PieceColor aiColor) {
        int score = 0;
        
        // 中期游戏：平衡发展和控制
        int flipCount = countPotentialFlips(move.getX(), move.getY(), aiColor);
        score += flipCount * 2;
        
        return score;
    }

    // 设置游戏模式方法
    public void setGameMode(GameMode mode, AIType blackAI, AIType whiteAI) {
        this.gameMode = mode;
        this.playerAITypes.put(blackPlayer, blackAI);
        this.playerAITypes.put(whitePlayer, whiteAI);
        
        if (gameRecorder != null) {
            gameRecorder.addAnnotation("游戏模式: " + mode.getDescription());
        }
    }
    
    @Override
    public void setPlayers(Player blackPlayer, Player whitePlayer) {
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.currentPlayer = blackPlayer;
    }
    
    @Override
    public Player getBlackPlayer() {
        return blackPlayer;
    }
    
    @Override
    public Player getWhitePlayer() {
        return whitePlayer;
    }
}