// core/Subject.java
package com.chessplatform.core;
import java.util.ArrayList;
import java.util.List;

public abstract class Subject {
    private List<Observer> observers = new ArrayList<>();
    private EventManager eventManager = new EventManager();
    
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
    
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }
    
    protected void notifyObservers(Game game) {
        for (Observer observer : observers) {
            observer.update(game);
        }
    }
    
    // 新增事件管理方法
    public void addEventListener(Class<? extends GameEvent> eventType, GameEventListener listener) {
        eventManager.addListener(eventType, listener);
    }
    
    public void removeEventListener(Class<? extends GameEvent> eventType, GameEventListener listener) {
        eventManager.removeListener(eventType, listener);
    }
    
    protected void fireGameEvent(GameEvent event) {
        eventManager.fireEvent(event);
        notifyObservers((Game) this); // 同时通知观察者
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
}