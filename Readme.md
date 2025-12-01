# 棋类对战平台 - Chess Battle Platform

## 📖 项目简介

一个基于Java面向对象设计的棋类对战平台，支持五子棋和围棋的双人对战功能。项目采用多种设计模式，实现了高扩展性、低耦合的架构设计。

## 🎮 功能特性

### 核心功能
- ✅ **双人对战**：支持五子棋和围棋的双人本地对战
- ✅ **完整规则**：实现五子棋连五获胜、围棋提子和胜负判断
- ✅ **游戏控制**：开始/重新开始游戏、悔棋、认输
- ✅ **存档管理**：游戏状态保存与加载
- ✅ **界面交互**：美观的控制台界面，实时棋盘显示

### 规则实现
- **五子棋**：连五获胜、棋盘满平局
- **围棋**：提子、虚着、劫争、数子法胜负判断
- **通用**：合法落子检查、禁入点判断

## 🏗️ 系统架构

### 设计模式应用
- **策略模式**：不同棋类游戏规则
- **命令模式**：用户操作封装
- **观察者模式**：界面与逻辑解耦
- **备忘录模式**：悔棋和存档功能
- **工厂模式**：游戏对象创建
- **单例模式**：平台实例管理

### 项目结构
```
src/
├── com/chessplatform/
│   ├── ChessPlatform.java          # 程序入口
│   ├── core/                       # 核心接口
│   ├── games/                      # 游戏实现
│   ├── model/                      # 数据模型
│   ├── command/                    # 命令模式
│   ├── memento/                    # 备忘录模式
│   ├── ui/                         # 用户界面
│   └── util/                       # 工具类
```

## 🚀 快速开始

### 编译运行
```bash
# 1. 克隆项目
git clone <repository-url>
cd chess-platform

# 2. 编译项目
javac -d ./bin ./src/com/chessplatform/**/*.java

# 3. 运行程序
java -cp ./bin com.chessplatform.ChessPlatform
```

## 🎯 使用说明

### 启动游戏
程序启动后，您将看到欢迎界面和命令帮助。

### 常用命令
```
# 游戏控制
start [gomoku|go] [size]    # 开始游戏（size: 8-19）
restart                     # 重新开始
exit                        # 退出程序

# 游戏操作
move [row] [col]            # 落子
pass                        # 虚着（仅围棋）
undo                        # 悔棋
resign                      # 认输

# 存档管理
save [filename]             # 保存游戏
load [filename]             # 加载游戏
list                        # 列出存档

# 系统命令
help                        # 显示帮助
hidehelp                    # 隐藏帮助
status                      # 显示游戏状态
```

### 示例游戏
```bash
# 开始一局15×15的五子棋
平台> start gomoku 15

# 黑方在中心落子
黑方> move 7 7

# 白方在旁边落子
白方> move 7 8

# 保存游戏
白方> save my_game

# 悔棋一步
黑方> undo
```