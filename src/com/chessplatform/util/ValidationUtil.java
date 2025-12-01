// util/ValidationUtil.java
package com.chessplatform.util;

public class ValidationUtil {
    
    public static boolean isValidBoardSize(int size) {
        return size >= 8 && size <= 19;
    }
    
    public static boolean isValidCoordinate(int coordinate, int boardSize) {
        return coordinate >= 0 && coordinate < boardSize;
    }
    
    public static boolean isValidMoveFormat(String[] parts) {
        if (parts.length != 3) return false;
        try {
            Integer.parseInt(parts[1]);
            Integer.parseInt(parts[2]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidStartCommand(String[] parts) {
        if (parts.length != 3) return false;
        try {
            String gameType = parts[1].toUpperCase();
            int size = Integer.parseInt(parts[2]);
            
            if (!gameType.equals("GOMOKU") && !gameType.equals("GO")) {
                return false;
            }
            return isValidBoardSize(size);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static String validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String cleanName = filename.trim();
        if (!cleanName.endsWith(".save")) {
            cleanName += ".save";
        }
        
        // 移除非法字符
        cleanName = cleanName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        return cleanName;
    }
}