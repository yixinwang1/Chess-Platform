// core/Subject.java
package com.chessplatform.core;

import java.util.ArrayList;
import java.util.List;

public abstract class Subject {
    private List<Observer> observers = new ArrayList<>();
    
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
}