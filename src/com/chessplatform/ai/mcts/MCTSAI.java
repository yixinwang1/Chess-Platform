// 新增 ai/mcts/MCTSAI.java
package com.chessplatform.ai.mcts;

import com.chessplatform.ai.AbstractAI;
import com.chessplatform.core.*;
import com.chessplatform.model.*;
import java.util.*;

public class MCTSAI extends AbstractAI {
    private static final int DEFAULT_ITERATIONS = 1000;
    private int iterations;
    
    public MCTSAI() {
        super("MCTS AI", 3);
        this.iterations = DEFAULT_ITERATIONS;
    }
    
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
    
    @Override
    public Point think(Game game) {
        List<Point> validMoves = getValidMoves(game);
        if (validMoves == null || validMoves.isEmpty()) {
            return null;
        }
        
        // 如果只有一步可走，直接返回
        if (validMoves.size() == 1) {
            return validMoves.get(0);
        }
        
        // 创建根节点
        MCTSNode root = new MCTSNode(game.copy(), null, null);
        
        // MCTS迭代
        for (int i = 0; i < iterations; i++) {
            // 1. 选择
            MCTSNode node = select(root);
            
            // 2. 扩展
            if (!node.isTerminal()) {
                node = expand(node);
            }
            
            // 3. 模拟
            double result = simulate(node);
            
            // 4. 回传
            backpropagate(node, result);
        }
        
        // 选择最佳移动
        return getBestMove(root);
    }
    
    private MCTSNode select(MCTSNode node) {
        while (!node.hasUntriedMoves() && node.hasChildren()) {
            node = node.selectChild();
        }
        return node;
    }
    
    private MCTSNode expand(MCTSNode node) {
        List<Point> untriedMoves = node.getUntriedMoves();
        if (untriedMoves.isEmpty()) {
            return node;
        }
        
        Point move = untriedMoves.get(random.nextInt(untriedMoves.size()));
        Game nextState = simulateMove(node.getGameState(), move);
        return node.addChild(nextState, move);
    }
    
    private double simulate(MCTSNode node) {
        Game gameState = node.getGameState().copy();
        
        // 随机模拟到游戏结束
        while (!gameState.isGameOver()) {
            List<Point> moves = gameState.getValidMoves();
            if (moves.isEmpty()) {
                gameState.pass();
                continue;
            }
            
            Point randomMove = moves.get(random.nextInt(moves.size()));
            gameState.makeMove(randomMove.getX(), randomMove.getY());
        }
        
        // 返回模拟结果
        if (gameState.getWinner() == null) {
            return 0.5; // 平局
        }
        
        // 判断输赢（简化）
        return gameState.getWinner().getColor() == node.getPlayerColor() ? 1.0 : 0.0;
    }
    
    private void backpropagate(MCTSNode node, double result) {
        while (node != null) {
            node.update(result);
            node = node.getParent();
        }
    }
    
    private Point getBestMove(MCTSNode root) {
        return root.getBestChild().getMove();
    }
    
    private Game simulateMove(Game game, Point move) {
        Game copy = game.copy();
        copy.makeMove(move.getX(), move.getY());
        return copy;
    }
    
    // MCTS节点类完整实现
    private class MCTSNode {
        private Game gameState;
        private MCTSNode parent;
        private Point move;
        private List<MCTSNode> children;
        private List<Point> untriedMoves;
        private int visits;
        private double wins;
        private PieceColor playerColor; // 记录当前节点的玩家颜色
        
        public MCTSNode(Game gameState, MCTSNode parent, Point move) {
            this.gameState = gameState;
            this.parent = parent;
            this.move = move;
            this.children = new ArrayList<>();
            this.untriedMoves = new ArrayList<>(gameState.getValidMoves());
            this.visits = 0;
            this.wins = 0;
            this.playerColor = gameState.getCurrentPlayer().getColor();
        }
        
        /**
         * 选择子节点（使用UCB公式）
         */
        public MCTSNode selectChild() {
            MCTSNode selected = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            double explorationFactor = Math.sqrt(2.0); // UCB公式中的C值
            
            for (MCTSNode child : children) {
                if (child.visits == 0) {
                    return child; // 优先选择未探索的节点
                }
                
                // UCB公式：选择价值最高的节点
                double ucbValue = child.wins / child.visits + 
                    explorationFactor * Math.sqrt(Math.log(this.visits) / child.visits);
                
                if (ucbValue > bestValue) {
                    bestValue = ucbValue;
                    selected = child;
                }
            }
            
            return selected;
        }
        
        /**
         * 扩展节点：从未尝试的走法中随机选择一个创建子节点
         */
        public MCTSNode expand() {
            if (untriedMoves.isEmpty()) {
                return this; // 没有可扩展的走法
            }
            
            // 随机选择一个未尝试的走法
            int randomIndex = random.nextInt(untriedMoves.size());
            Point moveToTry = untriedMoves.remove(randomIndex);
            
            // 创建新状态
            Game nextState = simulateMove(gameState, moveToTry);
            
            // 创建子节点
            MCTSNode child = new MCTSNode(nextState, this, moveToTry);
            children.add(child);
            
            return child;
        }

        private  MCTSNode addChild(Game gameState, Point move) {
            // 创建子节点
            MCTSNode child = new MCTSNode(gameState, this, move);
            children.add(child);
            return child;
        }
        
        /**
         * 模拟（随机对局直到结束）
         */
        public double simulate() {
            Game simulationState = gameState.copy();
            int maxSimulationDepth = 100; // 防止无限循环
            
            for (int depth = 0; depth < maxSimulationDepth; depth++) {
                if (simulationState.isGameOver()) {
                    break;
                }
                
                List<Point> validMoves = simulationState.getValidMoves();
                if (validMoves.isEmpty()) {
                    simulationState.pass();
                    continue;
                }
                
                // 随机选择走法
                Point randomMove = validMoves.get(random.nextInt(validMoves.size()));
                simulationState.makeMove(randomMove.getX(), randomMove.getY());
            }
            
            // 计算胜率
            return evaluateResult(simulationState);
        }
        
        /**
         * 回传结果到根节点
         */
        public void backpropagate(double result) {
            MCTSNode node = this;
            while (node != null) {
                node.visits++;
                node.wins += result;
                
                // 对于对手的节点，胜率要取反
                if (node.parent != null && 
                    node.parent.playerColor != node.playerColor) {
                    result = 1.0 - result;
                }
                
                node = node.parent;
            }
        }
        
        // ========== 辅助方法 ==========
        
        /**
         * 评估游戏结果
         * @return 1.0: 当前玩家胜利, 0.0: 对手胜利, 0.5: 平局
         */
        private double evaluateResult(Game gameState) {
            if (!gameState.isGameOver()) {
                // 游戏未结束，使用启发式评估
                return evaluateHeuristic(gameState);
            }
            
            if (gameState.getWinner() == null) {
                return 0.5; // 平局
            }
            
            // 判断胜负
            return (gameState.getWinner().getColor() == this.playerColor) ? 1.0 : 0.0;
        }
        
        /**
         * 启发式评估（当游戏未结束时使用）
         */
        private double evaluateHeuristic(Game gameState) {
            // 简化的五子棋评估函数
            if (gameState.getGameType() != GameType.GOMOKU) {
                return 0.5; // 对于非五子棋游戏，返回中性值
            }
            
            try {
                // 计算双方潜在威胁
                double blackScore = evaluatePlayerThreat(gameState, PieceColor.BLACK);
                double whiteScore = evaluatePlayerThreat(gameState, PieceColor.WHITE);
                
                if (blackScore == 0 && whiteScore == 0) {
                    return 0.5;
                }
                
                // 根据当前玩家颜色返回相对优势
                if (playerColor == PieceColor.BLACK) {
                    return blackScore / (blackScore + whiteScore);
                } else {
                    return whiteScore / (blackScore + whiteScore);
                }
            } catch (Exception e) {
                return 0.5;
            }
        }
        
        /**
         * 评估玩家威胁（五子棋专用）
         */
        private double evaluatePlayerThreat(Game gameState, PieceColor color) {
            double score = 0;
            Board board = gameState.getBoard();
            int size = board.getSize();
            
            // 检查四个方向
            int[][] directions = {{1,0}, {0,1}, {1,1}, {1,-1}};
            
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (!board.getPiece(i, j).isEmpty()) {
                        continue;
                    }
                    
                    // 检查这个空位对指定颜色的价值
                    for (int[] dir : directions) {
                        int potential = countPotential(board, i, j, color, dir[0], dir[1]);
                        score += Math.pow(2, potential); // 连子越多，指数级加分
                    }
                }
            }
            
            return score;
        }
        
        /**
         * 计算一个位置在一个方向上的潜力
         */
        private int countPotential(Board board, int row, int col, PieceColor color, int dx, int dy) {
            int count = 0;
            boolean blocked = false;
            
            // 正反两个方向检查
            for (int direction = -1; direction <= 1; direction += 2) {
                for (int step = 1; step <= 4; step++) {
                    int checkRow = row + dx * step * direction;
                    int checkCol = col + dy * step * direction;
                    
                    if (!board.isValidPosition(checkRow, checkCol)) {
                        blocked = true;
                        break;
                    }
                    
                    Piece piece = board.getPiece(checkRow, checkCol);
                    if (piece.getColor() == color) {
                        count++;
                    } else if (piece.isEmpty()) {
                        break;
                    } else {
                        blocked = true;
                        break;
                    }
                }
            }
            
            return blocked ? count : count * 2; // 未被阻挡的连子价值更高
        }
        
        /**
         * 模拟一步走法（不改变原始状态）
         */
        private Game simulateMove(Game originalGame, Point move) {
            Game copy = originalGame.copy();
            
            try {
                // 检查是否有效走法
                List<Point> validMoves = copy.getValidMoves();
                boolean isValid = false;
                for (Point validMove : validMoves) {
                    if (validMove.getX() == move.getX() && validMove.getY() == move.getY()) {
                        isValid = true;
                        break;
                    }
                }
                
                if (isValid) {
                    copy.makeMove(move.getX(), move.getY());
                }
            } catch (Exception e) {
                // 如果走法失败，返回原始状态
                System.err.println("模拟走法失败: " + e.getMessage());
            }
            
            return copy;
        }
        
        // ========== 查询方法 ==========
        
        public boolean isTerminal() {
            return gameState.isGameOver() || untriedMoves.isEmpty();
        }
        
        public boolean hasUntriedMoves() {
            return !untriedMoves.isEmpty();
        }
        
        public boolean hasChildren() {
            return !children.isEmpty();
        }
        
        public List<Point> getUntriedMoves() {
            return new ArrayList<>(untriedMoves);
        }
        
        public Game getGameState() {
            return gameState;
        }
        
        public Point getMove() {
            return move;
        }
        
        public MCTSNode getParent() {
            return parent;
        }
        
        public int getVisits() {
            return visits;
        }
        
        public double getWins() {
            return wins;
        }
        
        public double getWinRate() {
            return visits > 0 ? wins / visits : 0;
        }
        
        public PieceColor getPlayerColor() {
            return playerColor;
        }
        
        /**
         * 获取最佳子节点（用于最终选择）
         */
        public MCTSNode getBestChild() {
            if (children.isEmpty()) {
                return null;
            }
            
            // 选择访问次数最多的节点（更可靠）
            MCTSNode bestChild = null;
            int maxVisits = -1;
            
            for (MCTSNode child : children) {
                if (child.visits > maxVisits) {
                    maxVisits = child.visits;
                    bestChild = child;
                }
            }
            
            return bestChild;
        }
        
        /**
         * 获取最佳走法（基于胜率）
         */
        public Point getBestMove() {
            MCTSNode bestChild = getBestChild();
            return bestChild != null ? bestChild.getMove() : null;
        }
        
        /**
         * 获取所有子节点的统计信息
         */
        public Map<Point, NodeStats> getChildStats() {
            Map<Point, NodeStats> stats = new HashMap<>();
            
            for (MCTSNode child : children) {
                stats.put(child.getMove(), new NodeStats(
                    child.getWinRate(),
                    child.getVisits(),
                    child.getWins()
                ));
            }
            
            return stats;
        }
        
        /**
         * 更新节点统计
         */
        public void update(double result) {
            this.visits++;
            this.wins += result;
        }
        
        /**
         * 节点统计信息内部类
         */
        private class NodeStats {
            final double winRate;
            final int visits;
            final double wins;
            
            NodeStats(double winRate, int visits, double wins) {
                this.winRate = winRate;
                this.visits = visits;
                this.wins = wins;
            }
            
            @Override
            public String toString() {
                return String.format("胜率: %.2f%%, 访问: %d, 胜场: %.1f", 
                    winRate * 100, visits, wins);
            }
        }
        
        @Override
        public String toString() {
            return String.format("MCTSNode{move=%s, visits=%d, wins=%.1f, winRate=%.2f}", 
                move, visits, wins, getWinRate());
        }
    }
}