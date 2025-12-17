// ui/ConsoleUI.java
package com.chessplatform.ui;

import com.chessplatform.command.*;
import com.chessplatform.core.Game;
import com.chessplatform.core.GameType;
import com.chessplatform.core.Observer;
import com.chessplatform.games.GameFactory;
import com.chessplatform.games.reversi.Reversi;
import com.chessplatform.memento.GameCaretaker;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.Board;
import com.chessplatform.model.Move;
import com.chessplatform.model.Piece;
import com.chessplatform.model.Point;
import com.chessplatform.platform.ChessPlatformWithReplay;
import com.chessplatform.recorder.GameRecorder;
import com.chessplatform.recorder.ReplayController;
import com.chessplatform.util.FileUtil;
import com.chessplatform.util.ValidationUtil;
import java.util.*;

public class ConsoleUI implements Observer {
    private Game currentGame;
    private GameCaretaker caretaker;
    private boolean showHelp;
    private Scanner scanner;
    private boolean running;
    private ChessPlatformWithReplay replayPlatform;  // 新增
    private boolean isReplayMode;
    
    public ConsoleUI() {
        this.caretaker = new GameCaretaker();
        this.showHelp = true;
        this.scanner = new Scanner(System.in);
        this.running = true;
        this.replayPlatform = new ChessPlatformWithReplay();
        this.isReplayMode = false;
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
        
        if (isReplayMode) {
            // 回放模式下的特殊命令
            processReplayCommand(command, parts);
            return;
        }
        
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
            case "replay":        // 新增
                handleReplayCommand(parts);
                break;
            case "showhistory":   // 新增
                handleShowHistoryCommand();
                break;
            default:
                System.out.println("未知命令: " + command);
                System.out.println("请输入 'help' 查看可用命令");
        }
    }
    
    private void handleStartCommand(String[] parts) {
        if (!ValidationUtil.isValidStartCommand(parts)) {
            System.out.println("用法: start [gomoku|go|reversi] [size]");
            System.out.println("size: 棋盘大小(8-19)，黑白棋固定8×8");
            return;
        }
        
        try {
            GameType gameType = GameType.fromString(parts[1]);
            int size = Integer.parseInt(parts[2]);
            
            // 黑白棋特殊处理
            if (gameType == GameType.REVERSI) {
                size = 8;  // 强制设置为8×8
                System.out.println("黑白棋固定使用8×8棋盘");
            }
            
            currentGame = GameFactory.createGame(gameType, size);
            caretaker.clear();
            
            System.out.println("开始新游戏: " + gameType.getChineseName() + 
                            " " + size + "x" + size);
            
            // 如果是黑白棋，显示初始提示
            if (gameType == GameType.REVERSI) {
                displayReversiHint();
            }
            
            update(currentGame);
            
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }

    // 添加黑白棋提示方法
    private void displayReversiHint() {
        System.out.println("\n=== 黑白棋规则提示 ===");
        System.out.println("1. 必须下在可以夹住对方棋子的位置");
        System.out.println("2. 被夹住的棋子会翻转为己方颜色");
        System.out.println("3. 当双方都无法落子时游戏结束");
        System.out.println("4. 棋子多的一方获胜");
        System.out.println("==================\n");
    }

    // 修改棋盘显示，为黑白棋添加特殊标记
    @Override
    public void update(Game game) {
        displayBoard(game);
        
        if (isReplayMode) {
            displayReplayInfo();
        } else {
            displayGameStatus(game);
        }
        
        displayGameStatus();
    }

    // 显示黑白棋的合法落子位置
    private void displayValidMoves(Game game) {
        if (game instanceof Reversi) {
            Reversi reversi = (Reversi) game;
            List<Point> validMoves = reversi.getValidMoves();
            
            if (!validMoves.isEmpty()) {
                System.out.print("合法落子位置: ");
                for (int i = 0; i < Math.min(validMoves.size(), 10); i++) {
                    Point p = validMoves.get(i);
                    System.out.print("(" + p.getX() + "," + p.getY() + ") ");
                }
                if (validMoves.size() > 10) {
                    System.out.print("... 等" + validMoves.size() + "个位置");
                }
                System.out.println();
            }
        }
    }

    // 修改棋盘显示，为合法落子位置添加标记
    private void displayBoard(Game game) {
        if (game == null) return;
        
        Board board = game.getBoard();
        int size = board.getSize();
        
        // 判断是否为黑白棋
        boolean isReversi = game.getGameType() == GameType.REVERSI;
        Set<String> validMovePositions = new HashSet<>();
        
        if (isReversi && game instanceof Reversi) {
            Reversi reversi = (Reversi) game;
            for (Point p : reversi.getValidMoves()) {
                validMovePositions.add(p.getX() + "," + p.getY());
            }
        }
        
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
                
                if (isReversi && validMovePositions.contains(i + "," + j) && piece.isEmpty()) {
                    // 标记合法落子位置
                    System.out.print("*  ");
                } else {
                    System.out.print(piece + "  ");
                }
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
        
        // 如果是回放模式，显示回放标记
        if (isReplayMode) {
            System.out.println("【回放模式】");
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

    private void displayGameStatus(Game game) {
        if (game == null) {
            System.out.println("没有进行中的游戏");
            return;
        }
        
        if (isReplayMode) {
            System.out.println("回放中... 输入 'help' 查看回放命令");
        } else {
            // 原有状态显示代码...
            System.out.println("游戏状态: " + game.getGameStatus());
            
            // 如果游戏结束，显示是否保存录像
            if (game.isGameOver() && game.getGameRecorder() != null) {
                System.out.println("本局游戏已录像，可使用 'showhistory' 查看");
            }
        }
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
    
    private void displayPrompt() {
        if (isReplayMode) {
            System.out.print("回放> ");
        } else if (currentGame == null) {
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
            "║   start [gomoku|go|reversi] [size] - 开始新游戏        ║\n" +
            "║   restart              - 重新开始当前游戏              ║\n" +
            "║   exit                 - 退出程序                      ║\n" +
            "║                                                        ║\n" +
            "║ 游戏操作:                                              ║\n" +
            "║   move [row] [col]     - 在指定位置落子                ║\n" +
            "║   pass                 - 虚着(围棋/黑白棋)             ║\n" +
            "║   undo                 - 悔棋                          ║\n" +
            "║   resign               - 认输                          ║\n" +
            "║                                                        ║\n" +
            "║ 录像与存档管理:                                        ║\n" +
            "║   save [filename]      - 保存游戏(包含录像)            ║\n" +
            "║   load [filename]      - 加载游戏(包含录像)            ║\n" +
            "║   list                 - 列出所有存档                  ║\n" +
            "║   showhistory          - 显示当前游戏的历史记录        ║\n" +
            "║   replay [filename]    - 回放指定存档                  ║\n" +
            "║                                                        ║\n" +
            "║ 回放模式命令(进入回放模式后可用):                      ║\n" +
            "║   next                 - 播放下一步                    ║\n" +
            "║   prev                 - 回到上一步                    ║\n" +
            "║   goto [n]             - 跳转到第n步                   ║\n" +
            "║   info                 - 显示回放信息                  ║\n" +
            "║   stop                 - 停止回放                      ║\n" +
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
            "║                    版本 2.0.0                          ║\n" +
            "║        支持五子棋、围棋、黑白棋对战                   ║\n" +
            "╚════════════════════════════════════════════════════════╝\n");
    }
    
    // 新增：处理回放命令
    private void handleReplayCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("用法: replay [filename]");
            return;
        }
        
        try {
            String filename = ValidationUtil.validateFilename(parts[1]);
            if (!FileUtil.saveFileExists(filename)) {
                System.out.println("文件不存在: " + filename);
                return;
            }
            
            // 加载游戏
            GameMemento loadedState = FileUtil.loadGame(filename);
            Game game = loadedState.getSavedState();
            
            // 应用录像数据
            if (loadedState.getGameRecorder() != null) {
                loadedState.applyRecorderToGame(game);
            }
            
            // 开始回放
            replayPlatform.startReplay(game);
            isReplayMode = true;
            currentGame = game;
            
            System.out.println("进入回放模式");
            System.out.println("对局信息: " + game.getGameRecorder().getGameTitle());
            System.out.println("总步数: " + game.getGameRecorder().getTotalMoves());
            System.out.println("时长: " + game.getGameRecorder().getDurationInSeconds() + "秒");
            
            displayReplayHelp();
            update(currentGame);
            
        } catch (Exception e) {
            System.out.println("回放失败: " + e.getMessage());
        }
    }
    
    // 新增：回放模式下的命令处理
    private void processReplayCommand(String command, String[] parts) {
        switch (command) {
            case "next":
                replayPlatform.replayNext();
                update(currentGame);
                break;
                
            case "prev":
            case "previous":
                replayPlatform.replayPrevious();
                update(currentGame);
                break;
                
            case "goto":
                if (parts.length >= 2) {
                    try {
                        int step = Integer.parseInt(parts[1]);
                        replayPlatform.replayGoTo(step);
                        update(currentGame);
                    } catch (NumberFormatException e) {
                        System.out.println("无效的步数: " + parts[1]);
                    }
                }
                break;
                
            case "play":
                replayPlatform.replayPlay();
                System.out.println("开始自动播放");
                break;
                
            case "pause":
                replayPlatform.replayPause();
                System.out.println("暂停播放");
                break;
                
            case "stop":
                replayPlatform.stopReplay();
                isReplayMode = false;
                currentGame = null;
                System.out.println("退出回放模式");
                break;
                
            case "info":
                displayReplayInfo();
                break;
                
            case "speed":
                if (parts.length >= 2) {
                    try {
                        int speed = Integer.parseInt(parts[1]);
                        ReplayController controller = replayPlatform.getReplayController();
                        if (controller != null) {
                            controller.setPlaybackSpeed(speed);
                            System.out.println("播放速度设置为: " + speed + "ms/步");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("无效的速度值");
                    }
                }
                break;
                
            case "help":
                displayReplayHelp();
                break;
                
            case "exit":
                replayPlatform.stopReplay();
                isReplayMode = false;
                System.out.println("退出回放模式");
                break;
                
            default:
                System.out.println("回放模式下未知命令，输入 'help' 查看回放命令");
        }
    }
    
    // 新增：显示历史记录
    private void handleShowHistoryCommand() {
        if (currentGame == null || currentGame.getGameRecorder() == null) {
            System.out.println("当前没有游戏或录像数据");
            return;
        }
        
        GameRecorder recorder = currentGame.getGameRecorder();
        System.out.println("\n=== 对局历史记录 ===");
        System.out.println("总步数: " + recorder.getTotalMoves());
        System.out.println("开始时间: " + recorder.getStartTime());
        if (recorder.getEndTime() != null) {
            System.out.println("结束时间: " + recorder.getEndTime());
        }
        System.out.println("时长: " + recorder.getDurationInSeconds() + "秒");
        
        // 显示前10步
        List<Move> moves = recorder.getMoveHistory();
        int showCount = Math.min(10, moves.size());
        System.out.println("\n前" + showCount + "步:");
        for (int i = 0; i < showCount; i++) {
            System.out.println("  " + moves.get(i));
        }
        
        if (moves.size() > 10) {
            System.out.println("  ... 还有" + (moves.size() - 10) + "步");
        }
        
        // 显示注解
        List<String> annotations = recorder.getAnnotations();
        if (!annotations.isEmpty()) {
            System.out.println("\n注解:");
            for (String annotation : annotations) {
                System.out.println("  " + annotation);
            }
        }
        
        System.out.println("==================\n");
    }
    
    // 新增：显示回放信息
    private void displayReplayInfo() {
        if (replayPlatform.isReplayMode()) {
            System.out.println(replayPlatform.getReplayInfo());
            
            ReplayController controller = replayPlatform.getReplayController();
            if (controller != null) {
                Move currentMove = controller.getCurrentMove();
                if (currentMove != null) {
                    System.out.println("当前: " + currentMove);
                }
                
                System.out.println("播放速度: " + controller.getPlaybackSpeed() + "ms/步");
                System.out.println("进度: " + String.format("%.1f%%", 
                    controller.getProgressPercentage() * 100));
            }
        }
    }
    
    // 新增：显示回放帮助
    private void displayReplayHelp() {
        System.out.println("\n=== 回放模式命令 ===");
        System.out.println("next        - 下一步");
        System.out.println("prev        - 上一步");
        System.out.println("goto [n]    - 跳转到第n步");
        System.out.println("play        - 开始自动播放");
        System.out.println("pause       - 暂停播放");
        System.out.println("stop        - 停止回放");
        System.out.println("speed [ms]  - 设置播放速度(毫秒/步)");
        System.out.println("info        - 显示回放信息");
        System.out.println("help        - 显示帮助");
        System.out.println("exit        - 退出回放模式");
        System.out.println("==================\n");
    }
}