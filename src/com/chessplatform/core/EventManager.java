// core/EventManager.java
package com.chessplatform.core;

import java.util.*;

public class EventManager {
    private Map<Class<? extends GameEvent>, List<GameEventListener>> listeners;
    
    public EventManager() {
        listeners = new HashMap<>();
    }
    
    public void addListener(Class<? extends GameEvent> eventType, GameEventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }
    
    public void removeListener(Class<? extends GameEvent> eventType, GameEventListener listener) {
        List<GameEventListener> listenerList = listeners.get(eventType);
        if (listenerList != null) {
            listenerList.remove(listener);
        }
    }
    
    public void fireEvent(GameEvent event) {
        List<GameEventListener> listenerList = listeners.get(event.getClass());
        if (listenerList != null) {
            for (GameEventListener listener : new ArrayList<>(listenerList)) {
                listener.onGameEvent(event);
            }
        }
    }
    
    public void clear() {
        listeners.clear();
    }
}