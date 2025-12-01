// ui/ConsoleUI.java
package com.chessplatform.ui;

import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.core.Observer;
import com.chessplatform.games.GameFactory;
import com.chessplatform.model.Board;
import com.chessplatform.model.Piece;
import com.chessplatform.model.PieceColor;
import com.chessplatform.command.*;
import com.chessplatform.memento.GameCaretaker;
import com.chessplatform.util.FileUtil;
import com.chessplatform.util.ValidationUtil;

import java.util.Scanner;

public class ConsoleUI implements Observer {
    private Game currentGame;
    private GameCaretaker caretaker;
    private boolean showHelp;
    private Scanner scanner;
    private boolean running;
    
    public ConsoleUI() {
        this.caretaker = new GameCaretaker();
        this.showHelp = true;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }
    
    public void start() {
        displayWelcome();
        
        while (running) {
            try {
                if (showHelp) {
                    displayHelp();
                }
                
                displayPrompt();
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                processInput(input);
                
            } catch (Exception e) {
                System.out.println("错误: " + e.getMessage());
                System.out.println("请输入 'help' 查看帮助");
            }
        }
        
        scanner.close();
        System.out.println("感谢使用棋类对战平台!");
    }
    
    private void processInput(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "start":
                handleStartCommand(parts);
                break;
            case "move":
                handleMoveCommand(parts);
                break;
            case "pass":
                handlePassCommand();
                break;
            case "undo":
                handleUndoCommand();
                break;
            case "resign":
                handleResignCommand();
                break;
            case "save":
                handleSaveCommand(parts);
                break;
            case "load":
                handleLoadCommand(parts);
                break;
            case "restart":
                handleRestartCommand();
                break;
            case "help":
                showHelp = true;
                System.out.println("已显示帮助信息");
                break;
            case "hidehelp":
                showHelp = false;
                System.out.println("已隐藏帮助信息");
                break;
            case "status":
                displayGameStatus();
                break;
            case "list":
                listSaveFiles();
                break;
            case "exit":
                running = false;
                break;
            default:
                System.out.println("未知命令: " + command);
                System.out.println("请输入 'help' 查看可用命令");
        }
    }
    
    private void handleStartCommand(String[] parts) {
        if (!ValidationUtil.isValidStartCommand(parts)) {
            System.out.println("用法: start [gomoku|go] [size]");
            System.out.println("size: 棋盘大小(8-19)");
            return;
        }
        
        try {
            GameType gameType = GameType.fromString(parts[1]);
            int size = Integer.parseInt(parts[2]);
            
            currentGame = GameFactory.createGame(gameType, size);
            caretaker.clear();
            
            System.out.println("开始新游戏: " + gameType.getChineseName() + 
                             " " + size + "x" + size);
            
            // 注册观察者
            if (currentGame instanceof com.chessplatform.core.Subject) {
                ((com.chessplatform.core.Subject) currentGame).addObserver(this);
            }
            
            update(currentGame);
            
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
    
    private void handleMoveCommand(String[] parts) {
        if (currentGame == null) {
            System.out.println("请先使用 'start' 命令开始游戏");
            return;
        }
        
        if (!ValidationUtil.isValidMoveFormat(parts)) {
            System.out.println("用法: move [row] [col]");
            return;
        }
        
        try {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            
            MoveCommand moveCmd = new MoveCommand(currentGame, caretaker, row, col);
            if (moveCmd.execute()) {
                System.out.println("落子成功: (" + row + ", " + col + ")");
                update(currentGame);
            } else {
                System.out.println("落子失败，位置不合法");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("错误: 坐标必须是数字");
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
    
    private void handlePassCommand() {
        if (currentGame == null) {
            System.out.println("请先使用 'start' 命令开始游戏");
            return;
        }
        
        if (currentGame.getGameType() != GameType.GO) {
            System.out.println("只有围棋支持虚着(pass)");
            return;
        }
        
        if (currentGame.pass()) {
            System.out.println("虚着成功");
            update(currentGame);
        } else {
            System.out.println("虚着失败");
        }
    }
    
    private void handleUndoCommand() {
        if (currentGame == null) {
            System.out.println("请先使用 'start' 命令开始游戏");
            return;
        }
        
        UndoCommand undoCmd = new UndoCommand(currentGame, caretaker);
        if (undoCmd.execute()) {
            System.out.println("悔棋成功");
            update(currentGame);
        } else {
            System.out.println("无法悔棋");
        }
    }
    
    private void handleResignCommand() {
        if (currentGame == null) {
            System.out.println("请先使用 'start' 命令开始游戏");
            return;
        }
        
        if (currentGame.isGameOver()) {
            System.out.println("游戏已结束，无法认输");
            return;
        }
        
        System.out.print("确认认输吗? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (confirmation.equals("yes") || confirmation.equals("y")) {
            ResignCommand resignCmd = new ResignCommand(
                currentGame, currentGame.getCurrentPlayer());
            if (resignCmd.execute()) {
                System.out.println("认输成功");
                update(currentGame);
            }
        } else {
            System.out.println("认输已取消");
        }
    }
    
    private void handleSaveCommand(String[] parts) {
        if (currentGame == null) {
            System.out.println("请先使用 'start' 命令开始游戏");
            return;
        }
        
        if (parts.length < 2) {
            System.out.println("用法: save [filename]");
            return;
        }
        
        try {
            String filename = ValidationUtil.validateFilename(parts[1]);
            SaveCommand saveCmd = new SaveCommand(currentGame, filename);
            if (saveCmd.execute()) {
                System.out.println("游戏已保存到: " + filename);
            } else {
                System.out.println("保存失败");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
    
    private void handleLoadCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("用法: load [filename]");
            return;
        }
        
        try {
            String filename = ValidationUtil.validateFilename(parts[1]);
            if (!FileUtil.saveFileExists(filename)) {
                System.out.println("文件不存在: " + filename);
                return;
            }
            
            if (currentGame != null) {
                System.out.print("当前游戏进度将丢失，确认加载吗? (yes/no): ");
                String confirmation = scanner.nextLine().trim().toLowerCase();
                if (!confirmation.equals("yes") && !confirmation.equals("y")) {
                    System.out.println("加载已取消");
                    return;
                }
            }
            
            // 创建一个临时游戏对象用于加载
            Game tempGame = GameFactory.createGame(GameType.GOMOKU, 15);
            LoadCommand loadCmd = new LoadCommand(tempGame, filename);
            
            if (loadCmd.execute()) {
                currentGame = tempGame;
                caretaker.clear();
                
                System.out.println("游戏已加载: " + filename);
                update(currentGame);
            } else {
                System.out.println("加载失败");
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
    
    private void handleRestartCommand() {
        if (currentGame == null) {
            System.out.println("没有进行中的游戏");
            return;
        }
        
        System.out.print("确认重新开始当前游戏吗? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (confirmation.equals("yes") || confirmation.equals("y")) {
            GameType gameType = currentGame.getGameType();
            int size = currentGame.getBoard().getSize();
            
            currentGame = GameFactory.createGame(gameType, size);
            caretaker.clear();
            
            System.out.println("游戏已重新开始");
            update(currentGame);
        } else {
            System.out.println("重新开始已取消");
        }
    }
    
    private void displayGameStatus() {
        if (currentGame == null) {
            System.out.println("没有进行中的游戏");
            return;
        }
        
        System.out.println("\n=== 游戏状态 ===");
        System.out.println("游戏类型: " + currentGame.getGameType().getChineseName());
        System.out.println("棋盘大小: " + currentGame.getBoard().getSize() + "x" + 
                         currentGame.getBoard().getSize());
        System.out.println("总步数: " + currentGame.getMoveCount());
        System.out.println("当前玩家: " + currentGame.getCurrentPlayer());
        System.out.println("游戏状态: " + currentGame.getGameStatus());
        System.out.println("================\n");
    }
    
    private void listSaveFiles() {
        String[] files = FileUtil.listSaveFiles();
        if (files == null || files.length == 0) {
            System.out.println("没有找到存档文件");
            return;
        }
        
        System.out.println("\n=== 存档列表 ===");
        for (int i = 0; i < files.length; i++) {
            System.out.println((i + 1) + ". " + files[i]);
        }
        System.out.println("===============\n");
    }
    
    @Override
    public void update(Game game) {
        displayBoard(game);
        displayGameStatus();
    }
    
    private void displayBoard(Game game) {
        if (game == null) return;
        
        Board board = game.getBoard();
        int size = board.getSize();
        
        System.out.println("\n当前棋盘:");
        
        // 打印列标号
        System.out.print("   ");
        for (int j = 0; j < size; j++) {
            System.out.print(String.format("%2d ", j));
        }
        System.out.println();
        
        // 打印分隔线
        System.out.print("  +");
        for (int j = 0; j < size; j++) {
            System.out.print("---");
        }
        System.out.println("+");
        
        // 打印棋盘内容
        for (int i = 0; i < size; i++) {
            System.out.print(String.format("%2d| ", i));
            for (int j = 0; j < size; j++) {
                Piece piece = board.getPiece(i, j);
                System.out.print(piece + "  ");
            }
            System.out.println("|" + i);
        }
        
        // 打印分隔线
        System.out.print("  +");
        for (int j = 0; j < size; j++) {
            System.out.print("---");
        }
        System.out.println("+");
        
        // 打印列标号
        System.out.print("   ");
        for (int j = 0; j < size; j++) {
            System.out.print(String.format("%2d ", j));
        }
        System.out.println("\n");
    }
    
    private void displayPrompt() {
        if (currentGame == null) {
            System.out.print("平台> ");
        } else {
            System.out.print(currentGame.getCurrentPlayer().getName() + "> ");
        }
    }
    
    private void displayHelp() {
        System.out.println("\n" + 
            "╔════════════════════════════════════════════════════════╗\n" +
            "║                    可用命令列表                        ║\n" +
            "╠════════════════════════════════════════════════════════╣\n" +
            "║ 游戏控制:                                              ║\n" +
            "║   start [gomoku|go] [size] - 开始新游戏(size: 8-19)    ║\n" +
            "║   restart              - 重新开始当前游戏              ║\n" +
            "║   exit                 - 退出程序                      ║\n" +
            "║                                                        ║\n" +
            "║ 游戏操作:                                              ║\n" +
            "║   move [row] [col]     - 在指定位置落子                ║\n" +
            "║   pass                 - 虚着(仅围棋)                  ║\n" +
            "║   undo                 - 悔棋                          ║\n" +
            "║   resign               - 认输                          ║\n" +
            "║                                                        ║\n" +
            "║ 存档管理:                                              ║\n" +
            "║   save [filename]      - 保存游戏                      ║\n" +
            "║   load [filename]      - 加载游戏                      ║\n" +
            "║   list                 - 列出所有存档                  ║\n" +
            "║                                                        ║\n" +
            "║ 系统命令:                                              ║\n" +
            "║   help                 - 显示帮助                      ║\n" +
            "║   hidehelp             - 隐藏帮助                      ║\n" +
            "║   status               - 显示游戏状态                  ║\n" +
            "╚════════════════════════════════════════════════════════╝\n");
    }
    
    private void displayWelcome() {
        System.out.println("\n" +
            "╔════════════════════════════════════════════════════════╗\n" +
            "║                欢迎使用棋类对战平台                    ║\n" +
            "║                    版本 1.0.0                          ║\n" +
            "║        支持五子棋(Gomoku)和围棋(Go)对战               ║\n" +
            "╚════════════════════════════════════════════════════════╝\n");
    }
}