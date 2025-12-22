// games/go/Go.java
package com.chessplatform.games.go;


import com.chessplatform.core.*;
import com.chessplatform.core.events.*;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.*;
import com.chessplatform.record.GameRecorder;
import java.io.Serializable;
import java.util.*;


public class Go extends Subject implements Game, Serializable {
    private static final long serialVersionUID = 1L;
    
    private Board board;
    private Player blackPlayer;
    private Player whitePlayer;
    private Player currentPlayer;
    private boolean gameOver;
    private Player winner;
    private Stack<Move> moveHistory;
    private int consecutivePasses;
    private Point lastKoPoint;
    
    // 录像和回放相关字段
    private GameRecorder gameRecorder;
    private ReplayMode replayMode;
    private int replayStep;
    
    // 添加AI相关字段
    private GameMode gameMode;
    private Map<Player, AIType> playerAITypes;
    
    public Go(int boardSize) {
        this.board = new Board(boardSize);
        this.blackPlayer = new Player("黑方", PieceColor.BLACK);
        this.whitePlayer = new Player("白方", PieceColor.WHITE);
        this.currentPlayer = blackPlayer;
        this.gameOver = false;
        this.winner = null;
        this.moveHistory = new Stack<>();
        this.consecutivePasses = 0;
        this.lastKoPoint = null;
        
        // 初始化录像和回放
        this.gameRecorder = new GameRecorder();
        this.replayMode = ReplayMode.NORMAL;
        this.replayStep = 0;
        
        // 记录初始状态
        gameRecorder.recordInitialState(board, currentPlayer);
        gameRecorder.addAnnotation("围棋游戏开始");
        gameRecorder.addAnnotation("棋盘大小: " + boardSize + "x" + boardSize);
        gameRecorder.addAnnotation("贴目: 6.5目");
        
        // AI初始化
        this.gameMode = GameMode.PLAYER_VS_PLAYER;
        this.playerAITypes = new HashMap<>();
        this.playerAITypes.put(blackPlayer, AIType.NONE);
        this.playerAITypes.put(whitePlayer, AIType.NONE);
    }
    
    @Override
    public boolean makeMove(int row, int col) {
        if (isReplayMode()) {
            return false;
        }
        
        if (!isValidMove(row, col) || gameOver) {
            return false;
        }
        
        // 检查劫争
        if (lastKoPoint != null && lastKoPoint.getX() == row && lastKoPoint.getY() == col) {
            gameRecorder.addAnnotation("劫争位置，不能立即回提");
            return false;
        }
        
        // 创建落子前快照（用于检查自尽）
        Board snapshot = board.copy();
        
        // 落子
        board.setPiece(row, col, new Piece(currentPlayer.getColor()));
        Move move = new Move(currentPlayer, row, col);
        moveHistory.push(move);
        // 创建移动事件
        fireGameEvent(new MoveMadeEvent(this, move));
        
        // 记录到录像
        recordMove(row, col);
        
        // 提子
        List<Point> capturedStones = captureStones(row, col);
        
        // 检查自尽
        if (!hasLiberties(row, col)) {
            // 撤销落子
            board = snapshot;
            moveHistory.pop();
            gameRecorder.addAnnotation("自尽位置，落子无效");
            return false;
        }
        
        // 设置劫点
        if (capturedStones.size() == 1) {
            lastKoPoint = capturedStones.get(0);
        } else {
            lastKoPoint = null;
        }
        
        consecutivePasses = 0;
        
        // 检查游戏是否结束
        checkGameEnd();
        
        // 切换玩家
        switchPlayer();
        
        // 记录提子信息
        if (!capturedStones.isEmpty()) {
            gameRecorder.addAnnotation("提子: " + capturedStones.size() + "颗");
        }
        
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
        
        // 围棋悔棋比较复杂，这里简化实现
        Move lastMove = moveHistory.pop();
        if (lastMove.isNormalMove()) {
            // 需要恢复被提的棋子，这里简化处理
            board.clearPosition(lastMove.getRow(), lastMove.getCol());
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
    
    private List<Point> captureStones(int row, int col) {
        List<Point> captured = new ArrayList<>();
        PieceColor currentColor = currentPlayer.getColor();
        PieceColor opponentColor = currentColor.getOpposite();
        
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (board.isValidPosition(newRow, newCol)) {
                Piece piece = board.getPiece(newRow, newCol);
                if (piece.getColor() == opponentColor) {
                    Set<Point> group = findGroup(newRow, newCol);
                    if (!hasLiberties(group)) {
                        captured.addAll(group);
                        for (Point p : group) {
                            board.clearPosition(p.getX(), p.getY());
                        }
                    }
                }
            }
        }
        
        return captured;
    }
    
    private Set<Point> findGroup(int startRow, int startCol) {
        Set<Point> group = new HashSet<>();
        Stack<Point> stack = new Stack<>();
        PieceColor color = board.getPiece(startRow, startCol).getColor();
        
        stack.push(new Point(startRow, startCol));
        
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            if (group.contains(p)) continue;
            
            group.add(p);
            
            int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : directions) {
                int newRow = p.getX() + dir[0];
                int newCol = p.getY() + dir[1];
                
                if (board.isValidPosition(newRow, newCol)) {
                    Point neighbor = new Point(newRow, newCol);
                    if (!group.contains(neighbor) && 
                        board.getPiece(newRow, newCol).getColor() == color) {
                        stack.push(neighbor);
                    }
                }
            }
        }
        
        return group;
    }
    
    private boolean hasLiberties(int row, int col) {
        Set<Point> group = findGroup(row, col);
        return hasLiberties(group);
    }
    
    private boolean hasLiberties(Set<Point> group) {
        for (Point p : group) {
            int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : directions) {
                int newRow = p.getX() + dir[0];
                int newCol = p.getY() + dir[1];
                
                if (board.isValidPosition(newRow, newCol)) {
                    if (board.getPiece(newRow, newCol).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void checkGameEnd() {
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
        int blackScore = 0;
        int whiteScore = 0;
        double komi = 6.5; // 贴目
        
        // 简化版的数子法
        for (int i = 0; i < board.getSize(); i++) {
            for (int j = 0; j < board.getSize(); j++) {
                Piece piece = board.getPiece(i, j);
                if (piece.getColor() == PieceColor.BLACK) {
                    blackScore++;
                } else if (piece.getColor() == PieceColor.WHITE) {
                    whiteScore++;
                } else {
                    // 空点判断（简化）
                    if (isTerritoryFor(i, j, PieceColor.BLACK)) {
                        blackScore++;
                    } else if (isTerritoryFor(i, j, PieceColor.WHITE)) {
                        whiteScore++;
                    }
                }
            }
        }
        
        // 白方加贴目
        whiteScore += komi;
        
        if (blackScore > whiteScore) {
            winner = blackPlayer;
            gameRecorder.addAnnotation("黑方胜: " + blackScore + " vs " + whiteScore + 
                                     " (含贴目" + komi + ")");
        } else if (whiteScore > blackScore) {
            winner = whitePlayer;
            gameRecorder.addAnnotation("白方胜: " + whiteScore + " vs " + blackScore + 
                                     " (含贴目" + komi + ")");
        } else {
            winner = null;
            gameRecorder.addAnnotation("平局: " + blackScore + " vs " + whiteScore);
        }
    }
    
    private boolean isTerritoryFor(int row, int col, PieceColor color) {
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (board.isValidPosition(newRow, newCol)) {
                Piece piece = board.getPiece(newRow, newCol);
                if (piece.getColor() == color.getOpposite()) {
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
        if (!board.isPositionEmpty(row, col)) return false;
        
        // 检查劫争
        if (lastKoPoint != null && lastKoPoint.getX() == row && lastKoPoint.getY() == col) {
            return false;
        }
        
        // 检查自尽
        Board testBoard = board.copy();
        testBoard.setPiece(row, col, new Piece(currentPlayer.getColor()));
        
        // 模拟提子
        List<Point> captured = new ArrayList<>();
        PieceColor currentColor = currentPlayer.getColor();
        PieceColor opponentColor = currentColor.getOpposite();
        
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (testBoard.isValidPosition(newRow, newCol)) {
                Piece piece = testBoard.getPiece(newRow, newCol);
                if (piece.getColor() == opponentColor) {
                    Set<Point> group = findGroupOnBoard(testBoard, newRow, newCol);
                    if (!hasLibertiesOnBoard(testBoard, group)) {
                        captured.addAll(group);
                    }
                }
            }
        }
        
        // 提子后检查新棋子是否有气
        for (Point p : captured) {
            testBoard.clearPosition(p.getX(), p.getY());
        }
        
        Set<Point> newGroup = findGroupOnBoard(testBoard, row, col);
        return hasLibertiesOnBoard(testBoard, newGroup);
    }
    
    private Set<Point> findGroupOnBoard(Board testBoard, int startRow, int startCol) {
        Set<Point> group = new HashSet<>();
        Stack<Point> stack = new Stack<>();
        PieceColor color = testBoard.getPiece(startRow, startCol).getColor();
        
        stack.push(new Point(startRow, startCol));
        
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            if (group.contains(p)) continue;
            
            group.add(p);
            
            int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : directions) {
                int newRow = p.getX() + dir[0];
                int newCol = p.getY() + dir[1];
                
                if (testBoard.isValidPosition(newRow, newCol)) {
                    Point neighbor = new Point(newRow, newCol);
                    if (!group.contains(neighbor) && 
                        testBoard.getPiece(newRow, newCol).getColor() == color) {
                        stack.push(neighbor);
                    }
                }
            }
        }
        
        return group;
    }
    
    private boolean hasLibertiesOnBoard(Board testBoard, Set<Point> group) {
        for (Point p : group) {
            int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : directions) {
                int newRow = p.getX() + dir[0];
                int newCol = p.getY() + dir[1];
                
                if (testBoard.isValidPosition(newRow, newCol)) {
                    if (testBoard.getPiece(newRow, newCol).isEmpty()) {
                        return true;
                    }
                }
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
        return GameType.GO;
    }
    
    @Override
    public String getGameStatus() {
        if (gameOver) {
            if (winner != null) {
                return "游戏结束! 获胜者: " + winner.getColor() + "(" + winner.getName() + ")";
            } else {
                return "游戏结束! 平局";
            }
        }
        return "当前玩家: " + currentPlayer.getName() + 
               " (连续虚着: " + consecutivePasses + "/2，总步数: " + moveHistory.size() + ")";
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
        lastKoPoint = null;
        
        List<Move> moves = gameRecorder.getMoveHistory();
        for (int i = 0; i < replayStep; i++) {
            Move move = moves.get(i);
            if (move.isNormalMove()) {
                board.setPiece(move.getRow(), move.getCol(), 
                              new Piece(move.getPlayer().getColor()));
                moveHistory.push(move);
                
                // 提子（简化）
                captureStones(move.getRow(), move.getCol());
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
    
    private Board reconstructBoard(int targetStep) {
        Board reconstructed = new Board(board.getSize());
        List<Move> moves = gameRecorder.getMoveHistory();
        
        // 这里需要完整的回放逻辑，简化实现
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
        Go savedState = (Go) memento.getSavedState();
        this.board = savedState.board.copy();
        this.currentPlayer = savedState.currentPlayer;
        this.gameOver = savedState.gameOver;
        this.winner = savedState.winner;
        this.moveHistory = new Stack<>();
        this.moveHistory.addAll(savedState.moveHistory);
        this.consecutivePasses = savedState.consecutivePasses;
        this.lastKoPoint = savedState.lastKoPoint;
        
        // 恢复录像
        if (memento.getGameRecorder() != null) {
            this.gameRecorder = memento.getGameRecorder();
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

    @Override
    public List<Point> getValidMoves() {
        List<Point> validMoves = new ArrayList<>();
        int size = board.getSize();
        
        // 检查每个空位置
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (isValidMove(i, j)) {
                    validMoves.add(new Point(i, j));
                }
            }
        }
        
        // 如果没有任何合法落子位置，返回空列表
        // 注意：围棋允许虚着(pass)，所以空列表是合法的
        return validMoves;
    }

    // 在指定棋盘上提子（辅助方法）
    private List<Point> captureStonesOnBoard(Board board, int row, int col, PieceColor color) {
        List<Point> captured = new ArrayList<>();
        PieceColor opponentColor = color.getOpposite();
        
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (board.isValidPosition(newRow, newCol)) {
                Piece piece = board.getPiece(newRow, newCol);
                if (piece.getColor() == opponentColor) {
                    Set<Point> group = findGroupOnBoard(board, newRow, newCol);
                    if (!hasLibertiesOnBoard(board, group)) {
                        captured.addAll(group);
                        // 移除无气棋子
                        for (Point p : group) {
                            board.clearPosition(p.getX(), p.getY());
                        }
                    }
                }
            }
        }
        
        return captured;
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
        Go copy = new Go(board.getSize());
        
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
        
        // 复制劫点
        if (this.lastKoPoint != null) {
            copy.lastKoPoint = new Point(this.lastKoPoint.getX(), this.lastKoPoint.getY());
        }
        
        // 复制AI设置
        copy.gameMode = this.gameMode;
        copy.playerAITypes = new HashMap<>();
        copy.playerAITypes.put(copy.blackPlayer, 
            this.playerAITypes.getOrDefault(this.blackPlayer, AIType.NONE));
        copy.playerAITypes.put(copy.whitePlayer, 
            this.playerAITypes.getOrDefault(this.whitePlayer, AIType.NONE));
        
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
            // 没有合法落子位置，AI可以选择虚着
            return null;
        }
        
        Random random = new Random();
        
        switch (aiType) {
            case RANDOM:
                // 随机AI：随机选择一个合法位置
                return validMoves.get(random.nextInt(validMoves.size()));
                
            case RULE:
                // 简化规则AI：优先选择有战略价值的位置
                return getRuleBasedMove(validMoves);
                
            case MCTS:
            case ADVANCED:
                // 高级AI：使用简化评估
                return getAdvancedMove(validMoves);
                
            default:
                // 默认随机
                return validMoves.get(random.nextInt(validMoves.size()));
        }
    }

    // 7. 规则AI（围棋专用）
    private Point getRuleBasedMove(List<Point> validMoves) {
        if (validMoves.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        int boardSize = board.getSize();
        
        // 围棋策略：优先占角，其次占边，最后占中
        List<Point> cornerMoves = new ArrayList<>();
        List<Point> edgeMoves = new ArrayList<>();
        List<Point> centerMoves = new ArrayList<>();
        
        for (Point move : validMoves) {
            int x = move.getX();
            int y = move.getY();
            
            // 判断位置类型
            boolean isCorner = (x <= 1 || x >= boardSize-2) && 
                            (y <= 1 || y >= boardSize-2);
            boolean isEdge = (x <= 1 || x >= boardSize-2 || 
                            y <= 1 || y >= boardSize-2) && !isCorner;
            
            if (isCorner) {
                cornerMoves.add(move);
            } else if (isEdge) {
                edgeMoves.add(move);
            } else {
                centerMoves.add(move);
            }
        }
        
        // 按优先级选择
        if (!cornerMoves.isEmpty()) {
            return cornerMoves.get(random.nextInt(cornerMoves.size()));
        } else if (!edgeMoves.isEmpty()) {
            return edgeMoves.get(random.nextInt(edgeMoves.size()));
        } else {
            return centerMoves.get(random.nextInt(centerMoves.size()));
        }
    }

    // 8. 高级AI（简化评估）
    private Point getAdvancedMove(List<Point> validMoves) {
        if (validMoves.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        
        // 简单的评估函数
        Map<Point, Integer> scores = new HashMap<>();
        PieceColor aiColor = currentPlayer.getColor();
        
        for (Point move : validMoves) {
            int score = evaluateMoveForGo(move, aiColor);
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

    // 9. 围棋落子评估函数
    private int evaluateMoveForGo(Point move, PieceColor aiColor) {
        int score = 0;
        int x = move.getX();
        int y = move.getY();
        int boardSize = board.getSize();
        
        // 1. 位置价值：角 > 边 > 中
        boolean isCorner = (x <= 1 || x >= boardSize-2) && 
                        (y <= 1 || y >= boardSize-2);
        boolean isEdge = (x <= 1 || x >= boardSize-2 || 
                        y <= 1 || y >= boardSize-2) && !isCorner;
        
        if (isCorner) {
            score += 30;
        } else if (isEdge) {
            score += 15;
        } else {
            score += 5;
        }
        
        // 2. 连接已有棋子的能力
        score += evaluateConnection(move, aiColor) * 10;
        
        // 3. 提子潜力
        score += evaluateCapturePotential(move, aiColor) * 20;
        
        // 4. 避免被提子
        score -= evaluateSelfCaptureRisk(move, aiColor) * 15;
        
        return score;
    }

    // 10. 评估连接能力
    private int evaluateConnection(Point move, PieceColor color) {
        int connections = 0;
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        
        for (int[] dir : directions) {
            int newX = move.getX() + dir[0];
            int newY = move.getY() + dir[1];
            
            if (board.isValidPosition(newX, newY)) {
                Piece piece = board.getPiece(newX, newY);
                if (piece.getColor() == color) {
                    connections++;
                }
            }
        }
        
        return connections;
    }

    // 11. 评估提子潜力
    private int evaluateCapturePotential(Point move, PieceColor color) {
        // 创建临时棋盘模拟
        Board testBoard = board.copy();
        testBoard.setPiece(move.getX(), move.getY(), new Piece(color));
        
        int capturedStones = 0;
        PieceColor opponentColor = color.getOpposite();
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        
        for (int[] dir : directions) {
            int newX = move.getX() + dir[0];
            int newY = move.getY() + dir[1];
            
            if (testBoard.isValidPosition(newX, newY)) {
                Piece piece = testBoard.getPiece(newX, newY);
                if (piece.getColor() == opponentColor) {
                    Set<Point> group = findGroupOnBoard(testBoard, newX, newY);
                    if (!hasLibertiesOnBoard(testBoard, group)) {
                        capturedStones += group.size();
                    }
                }
            }
        }
        
        return capturedStones;
    }

    // 12. 评估自尽风险
    private int evaluateSelfCaptureRisk(Point move, PieceColor color) {
        // 创建临时棋盘检查
        Board testBoard = board.copy();
        testBoard.setPiece(move.getX(), move.getY(), new Piece(color));
        
        // 提子后检查
        captureStonesOnBoard(testBoard, move.getX(), move.getY(), color);
        
        // 检查新棋子是否有气
        Set<Point> newGroup = findGroupOnBoard(testBoard, move.getX(), move.getY());
        boolean hasLiberties = hasLibertiesOnBoard(testBoard, newGroup);
        
        return hasLiberties ? 0 : 1; // 有自尽风险返回1
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