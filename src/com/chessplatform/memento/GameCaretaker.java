// memento/GameCaretaker.java
package com.chessplatform.memento;

import java.util.Stack;

public class GameCaretaker {
    private Stack<GameMemento> mementos = new Stack<>();
    private static final int MAX_UNDO_STEPS = 10;
    
    public void saveMemento(GameMemento memento) {
        if (mementos.size() >= MAX_UNDO_STEPS) {
            mementos.remove(0); // 移除最旧的记录
        }
        mementos.push(memento);
    }
    
    public GameMemento getLastMemento() {
        if (mementos.isEmpty()) {
            return null;
        }
        return mementos.pop();
    }
    
    public boolean canUndo() {
        return !mementos.isEmpty();
    }
    
    public void clear() {
        mementos.clear();
    }
}