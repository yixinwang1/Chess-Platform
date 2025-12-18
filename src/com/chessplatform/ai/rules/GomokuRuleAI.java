// 新增 ai/rules/GomokuRuleAI.java
package com.chessplatform.ai.rules;

import com.chessplatform.ai.AbstractAI;
import com.chessplatform.core.Game;
import com.chessplatform.model.*;
import java.util.*;

public class GomokuRuleAI extends AbstractAI {
    
    public GomokuRuleAI() {
        super("规则AI", 2);
    }
    
    @Override
    public Point think(Game game) {
        List<Point> validMoves = getValidMoves(game);
        if (validMoves == null || validMoves.isEmpty()) {
            return null;
        }
        
        // 评分选择最佳位置
        Map<Point, Integer> scores = new HashMap<>();
        PieceColor aiColor = game.getCurrentPlayer().getColor();
        
        for (Point move : validMoves) {
            int score = evaluateMove(game, move, aiColor);
            scores.put(move, score);
        }
        
        // 选择最高分的位置
        return selectBestMove(scores);
    }
    
    private int evaluateMove(Game game, Point move, PieceColor aiColor) {
        int score = 0;
        
        // 1. 中心区域加分
        score += evaluateCenterPosition(move, game.getBoard().getSize());
        
        // 2. 进攻得分（形成连子）
        score += evaluateOffensive(game, move, aiColor) * 2;
        
        // 3. 防守得分（阻止对手）
        score += evaluateDefensive(game, move, aiColor.getOpposite());
        
        // 4. 棋型评分
        score += evaluatePattern(game, move, aiColor);
        
        return score;
    }
    
    private int evaluateCenterPosition(Point move, int boardSize) {
        int center = boardSize / 2;
        int distance = Math.abs(move.getX() - center) + Math.abs(move.getY() - center);
        return Math.max(0, 10 - distance);
    }
    
    private int evaluateOffensive(Game game, Point move, PieceColor color) {
        return countPotentialLines(game, move, color);
    }
    
    private int evaluateDefensive(Game game, Point move, PieceColor opponentColor) {
        return countPotentialLines(game, move, opponentColor);
    }
    
    private int evaluatePattern(Game game, Point move, PieceColor color) {
        // 简化的棋型判断
        // 活四 > 冲四 > 活三 > 死四 > 活二
        // 这里实现简化版本
        return 0;
    }
    
    private int countPotentialLines(Game game, Point move, PieceColor color) {
        int count = 0;
        int[][] directions = {{1,0}, {0,1}, {1,1}, {1,-1}};
        
        for (int[] dir : directions) {
            if (checkDirectionPotential(game, move, color, dir[0], dir[1])) {
                count++;
            }
        }
        
        return count;
    }
    
    private boolean checkDirectionPotential(Game game, Point move, PieceColor color, 
                                          int dx, int dy) {
        // 检查一个方向是否有潜力
        int consecutive = 1; // 当前位置
        
        // 正向
        for (int i = 1; i <= 4; i++) {
            int x = move.getX() + dx * i;
            int y = move.getY() + dy * i;
            if (!isValidPosition(x, y, game.getBoard())) break;
            
            Piece piece = game.getBoard().getPiece(x, y);
            if (piece.getColor() == color) {
                consecutive++;
            } else if (piece.isEmpty()) {
                // 空位，可以继续
                break;
            } else {
                // 对手棋子，阻断
                break;
            }
        }
        
        // 反向
        for (int i = 1; i <= 4; i++) {
            int x = move.getX() - dx * i;
            int y = move.getY() - dy * i;
            if (!isValidPosition(x, y, game.getBoard())) break;
            
            Piece piece = game.getBoard().getPiece(x, y);
            if (piece.getColor() == color) {
                consecutive++;
            } else if (piece.isEmpty()) {
                break;
            } else {
                break;
            }
        }
        
        return consecutive >= 2; // 至少有2子相连
    }
    
    private boolean isValidPosition(int x, int y, Board board) {
        return x >= 0 && x < board.getSize() && y >= 0 && y < board.getSize();
    }
    
    private Point selectBestMove(Map<Point, Integer> scores) {
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
}