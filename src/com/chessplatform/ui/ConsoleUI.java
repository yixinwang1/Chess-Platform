// ui/ConsoleUI.java
package com.chessplatform.ui;

import com.chessplatform.command.*;
import com.chessplatform.core.*;
import com.chessplatform.games.GameFactory;
import com.chessplatform.games.gomoku.Gomoku;
import com.chessplatform.games.reversi.Reversi;
import com.chessplatform.memento.GameCaretaker;
import com.chessplatform.memento.GameMemento;
import com.chessplatform.model.*;
import com.chessplatform.platform.ChessPlatformWithReplay;
import com.chessplatform.recorder.GameRecorder;
import com.chessplatform.recorder.ReplayController;
import com.chessplatform.util.FileUtil;
import com.chessplatform.util.ValidationUtil;
import java.util.*;
import javax.swing.SwingUtilities;

public class ConsoleUI implements com.chessplatform.core.Observer {
    private Game currentGame;
    private GameCaretaker caretaker;
    private boolean showHelp;
    private Scanner scanner;
    private boolean running;
    private ChessPlatformWithReplay replayPlatform;  // 新增
    private boolean isReplayMode;
    // 新增字段
    private boolean waitingForAI;
    private Thread aiThread;
    
    public ConsoleUI() {
        this.caretaker = new GameCaretaker();
        this.showHelp = true;
        this.scanner = new Scanner(System.in);
        this.running = true;
        this.replayPlatform = new ChessPlatformWithReplay();
        this.isReplayMode = false;
        this.waitingForAI = false;
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
            case "ai":
                handleAICommand(parts);
                break;
            case "aimode":
                handleAIModeCommand(parts);
                break;
            case "aistep":
                handleAIStepCommand();
                break;
            case "aiauto":
                handleAIAutoCommand(parts);
                break;
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
    
    // 修改start方法，支持AI模式选择
    private void handleStartCommand(String[] parts) {
        if (!ValidationUtil.isValidStartCommand(parts)) {
            System.out.println("用法: start [gomoku|go|reversi] [size] [mode] [blackAI] [whiteAI]");
            System.out.println("mode: pvp(玩家对战), pva(人机对战), ava(AI对战)");
            System.out.println("AI级别: none, random, rule, mcts");
            System.out.println("示例: start gomoku 15 pva random none");
            return;
        }
        
        try {
            GameType gameType = GameType.fromString(parts[1]);
            int size = Integer.parseInt(parts[2]);
            
            // 解析游戏模式和AI设置
            GameMode gameMode = GameMode.PLAYER_VS_PLAYER;
            AIType blackAI = AIType.NONE;
            AIType whiteAI = AIType.NONE;
            
            if (parts.length > 3) {
                gameMode = parseGameMode(parts[3]);
                if (parts.length > 4) blackAI = AIType.fromString(parts[4]);
                if (parts.length > 5) whiteAI = AIType.fromString(parts[5]);
            }
            
            // 创建游戏
            currentGame = GameFactory.createGame(gameType, size);
            
            // 设置游戏模式
            if (currentGame instanceof Gomoku) {
                ((Gomoku) currentGame).setGameMode(gameMode, blackAI, whiteAI);
            }
            
            // 开始游戏
            caretaker.clear();
            System.out.println("开始新游戏: " + gameType.getChineseName() + 
                             " " + size + "x" + size);
            System.out.println("游戏模式: " + gameMode.getDescription());
            
            if (blackAI != AIType.NONE) {
                System.out.println("黑方AI: " + blackAI.getDescription());
            }
            if (whiteAI != AIType.NONE) {
                System.out.println("白方AI: " + whiteAI.getDescription());
            }
            
            update(currentGame);
            
            // 如果黑方是AI，自动开始思考
            if (currentGame.isAIMove()) {
                startAITurn();
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
    
    private GameMode parseGameMode(String mode) {
        switch (mode.toLowerCase()) {
            case "pvp": return GameMode.PLAYER_VS_PLAYER;
            case "pva": return GameMode.PLAYER_VS_AI;
            case "ava": return GameMode.AI_VS_AI;
            default: throw new IllegalArgumentException("无效的游戏模式: " + mode);
        }
    }
    
    // 新增：启动AI思考线程
    private void startAITurn() {
        if (currentGame == null || !currentGame.isAIMove() || waitingForAI) {
            return;
        }
        
        waitingForAI = true;
        
        aiThread = new Thread(() -> {
            try {
                System.out.println("\n🤖 " + currentGame.getCurrentPlayer().getName() + 
                                 " 正在思考...");
                
                // 模拟AI思考时间
                Thread.sleep(500);
                
                // 获取AI走棋
                if (currentGame instanceof Gomoku) {
                    Gomoku gomoku = (Gomoku) currentGame;
                    Point aiMove = gomoku.getAIMove();
                    
                    if (aiMove != null) {
                        // 在主线程执行走棋
                        SwingUtilities.invokeLater(() -> {
                            executeAIMove(aiMove.getX(), aiMove.getY());
                        });
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                waitingForAI = false;
            }
        });
        
        aiThread.setDaemon(true);
        aiThread.start();
    }
    
    private void executeAIMove(int row, int col) {
        if (currentGame == null) return;
        
        Command moveCommand = new MoveCommand(currentGame, caretaker, row, col);
        if (moveCommand.execute()) {
            System.out.println("🤖 AI落子于 (" + row + ", " + col + ")");
            update(currentGame);
            
            // 检查是否游戏结束
            if (currentGame.isGameOver()) {
                System.out.println("\n🎯 游戏结束!");
                if (currentGame.getWinner() != null) {
                    System.out.println("🏆 获胜者: " + currentGame.getWinner().getName());
                } else {
                    System.out.println("🤝 平局!");
                }
            } else {
                // 如果下一个玩家也是AI，继续思考
                if (currentGame.isAIMove()) {
                    try {
                        Thread.sleep(500); // 给用户一点观察时间
                        startAITurn();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
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
        displayGameStatus(game);
        
        // 如果是AI走棋，显示提示
        if (game.isAIMove() && !waitingForAI) {
            System.out.println("⏳ 等待AI思考...");
        }
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
                if (currentGame.isAIMove()) {
                    startAITurn();
                }
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
    
    private void handleAICommand(String[] parts) {
        if (parts.length < 3) {
            System.out.println("用法: ai [black|white] [none|random|rule|mcts]");
            return;
        }
        
        if (currentGame == null) {
            System.out.println("请先开始游戏");
            return;
        }
        
        try {
            String playerStr = parts[1];
            String aiTypeStr = parts[2];
            
            Player player = playerStr.equalsIgnoreCase("black") ? 
                currentGame.getCurrentPlayer() : // 这里需要根据具体游戏获取玩家
                currentGame.getCurrentPlayer();  // 简化处理
            
            AIType aiType = AIType.fromString(aiTypeStr);
            currentGame.setAITypeForPlayer(player, aiType);
            
            System.out.println(player.getName() + " AI类型设置为: " + aiType.getDescription());
            
        } catch (Exception e) {
            System.out.println("设置AI失败: " + e.getMessage());
        }
    }
    
    private void handleAIModeCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("用法: aimode [pvp|pva|ava]");
            System.out.println("  pvp - 玩家对战");
            System.out.println("  pva - 人机对战");
            System.out.println("  ava - AI对战");
            return;
        }
        
        if (currentGame == null) {
            System.out.println("请先开始游戏");
            return;
        }
        
        String mode = parts[1].toLowerCase();
        GameMode gameMode;
        
        switch (mode) {
            case "pvp":
                gameMode = GameMode.PLAYER_VS_PLAYER;
                System.out.println("已设置为玩家对战模式");
                break;
            case "pva":
                gameMode = GameMode.PLAYER_VS_AI;
                System.out.println("已设置为人机对战模式");
                break;
            case "ava":
                gameMode = GameMode.AI_VS_AI;
                System.out.println("已设置为AI对战模式");
                break;
            default:
                System.out.println("无效的游戏模式，请使用: pvp, pva, ava");
                return;
        }
        
        // 设置游戏模式（需要游戏类支持）
        try {
            // 使用反射调用游戏类的setGameMode方法
            java.lang.reflect.Method method = currentGame.getClass()
                .getMethod("setGameMode", GameMode.class, AIType.class, AIType.class);
            
            // 获取当前AI设置
            AIType blackAI = AIType.NONE;
            AIType whiteAI = AIType.NONE;
            
            if (gameMode == GameMode.PLAYER_VS_AI) {
                whiteAI = AIType.RANDOM; // 默认白方为随机AI
            } else if (gameMode == GameMode.AI_VS_AI) {
                blackAI = AIType.RANDOM;
                whiteAI = AIType.RANDOM;
            }
            
            method.invoke(currentGame, gameMode, blackAI, whiteAI);
            
            // 更新显示
            update(currentGame);
            
            // 如果是AI模式，开始AI思考
            if (currentGame.isAIMove()) {
                System.out.println("AI开始思考...");
                startAITurn();
            }
            
        } catch (Exception e) {
            System.out.println("设置游戏模式失败: " + e.getMessage());
            System.out.println("此游戏可能不支持AI功能");
        }
    }

    private void handleAIStepCommand() {
        if (currentGame == null) {
            System.out.println("请先开始游戏");
            return;
        }
        
        if (!currentGame.isAIMove()) {
            System.out.println("当前不是AI回合");
            return;
        }
        
        System.out.println("执行AI走棋...");
        
        // 执行AI走棋
        if (currentGame instanceof com.chessplatform.games.gomoku.Gomoku) {
            com.chessplatform.games.gomoku.Gomoku gomoku = 
                (com.chessplatform.games.gomoku.Gomoku) currentGame;
            
            com.chessplatform.model.Point aiMove = gomoku.getAIMove();
            if (aiMove != null) {
                System.out.println("AI选择落子于 (" + aiMove.getX() + ", " + aiMove.getY() + ")");
                
                // 执行落子命令
                Command moveCommand = new MoveCommand(currentGame, caretaker, aiMove.getX(), aiMove.getY());
                if (moveCommand.execute()) {
                    update(currentGame);
                    
                    // 检查游戏是否结束
                    if (currentGame.isGameOver()) {
                        System.out.println("\n游戏结束!");
                        if (currentGame.getWinner() != null) {
                            System.out.println("获胜者: " + currentGame.getWinner().getName());
                        } else {
                            System.out.println("平局!");
                        }
                    }
                }
            } else {
                System.out.println("AI选择放弃落子");
            }
        } else {
            System.out.println("此游戏类型暂不支持AI");
        }
    }

    private void handleAIAutoCommand(String[] parts) {
        if (currentGame == null) {
            System.out.println("请先开始游戏");
            return;
        }
        
        final int delay = 1000; // 默认1秒延迟
        
        System.out.println("开始AI自动对战，延迟 " + delay + "ms...");
        
        // 在新线程中执行自动对战
        new Thread(() -> {
            try {
                int stepCount = 0;
                int maxSteps = 100; // 防止无限循环
                
                while (!currentGame.isGameOver() && stepCount < maxSteps) {
                    if (currentGame.isAIMove()) {
                        handleAIStepCommand();
                        stepCount++;
                    } else {
                        // 等待玩家操作
                        break;
                    }
                    
                    Thread.sleep(delay);
                }
                
                if (stepCount >= maxSteps) {
                    System.out.println("达到最大步数限制，自动对战停止");
                } else if (currentGame.isGameOver()) {
                    System.out.println("自动对战结束，游戏已结束");
                } else {
                    System.out.println("自动对战暂停，等待玩家操作");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("自动对战被中断");
            }
        }).start();
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
            "║   start [game] [size] [mode] [blackAI] [whiteAI]       ║\n" +
            "║     game: gomoku, go, reversi                          ║\n" +
            "║     mode: pvp(玩家对战), pva(人机对战), ava(AI对战)   ║\n" +
            "║     AI: none, random, rule, mcts                       ║\n" +
            "║   示例: start gomoku 15 pva random none                ║\n" +
            "║   restart              - 重新开始当前游戏              ║\n" +
            "║   exit                 - 退出程序                      ║\n" +
            "║                                                        ║\n" +
            "║ AI对战控制:                                            ║\n" +
            "║   ai [player] [type]   - 设置玩家AI类型               ║\n" +
            "║     player: black, white                               ║\n" +
            "║     type: none, random, rule, mcts                     ║\n" +
            "║   示例: ai black rule    # 设置黑方为规则AI           ║\n" +
            "║   aimode [mode]        - 设置游戏模式                 ║\n" +
            "║     mode: pvp, pva, ava                               ║\n" +
            "║   aistep               - AI走下一步                   ║\n" +
            "║   aiauto [delay]       - AI自动对战                  ║\n" +
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
        showHelp = false;
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