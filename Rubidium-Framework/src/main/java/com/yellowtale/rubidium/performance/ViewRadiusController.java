package com.yellowtale.rubidium.performance;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class ViewRadiusController {
    
    private final AtomicInteger globalViewRadius;
    private final int minimumRadius;
    private final int maximumRadius;
    
    private final Map<UUID, Integer> playerOverrides;
    private BiConsumer<UUID, Integer> radiusChangeListener;
    
    public ViewRadiusController(int defaultRadius, int minimumRadius, int maximumRadius) {
        this.globalViewRadius = new AtomicInteger(defaultRadius);
        this.minimumRadius = minimumRadius;
        this.maximumRadius = maximumRadius;
        this.playerOverrides = new ConcurrentHashMap<>();
    }
    
    public int getGlobalViewRadius() {
        return globalViewRadius.get();
    }
    
    public void setGlobalViewRadius(int radius) {
        int clamped = clamp(radius);
        int previous = globalViewRadius.getAndSet(clamped);
        
        if (previous != clamped && radiusChangeListener != null) {
            radiusChangeListener.accept(null, clamped);
        }
    }
    
    public int getViewRadius(UUID playerId) {
        Integer override = playerOverrides.get(playerId);
        if (override != null) {
            return Math.min(override, globalViewRadius.get());
        }
        return globalViewRadius.get();
    }
    
    public void setPlayerOverride(UUID playerId, int radius) {
        int clamped = clamp(radius);
        playerOverrides.put(playerId, clamped);
        
        if (radiusChangeListener != null) {
            radiusChangeListener.accept(playerId, getViewRadius(playerId));
        }
    }
    
    public void clearPlayerOverride(UUID playerId) {
        playerOverrides.remove(playerId);
        
        if (radiusChangeListener != null) {
            radiusChangeListener.accept(playerId, getViewRadius(playerId));
        }
    }
    
    public boolean hasPlayerOverride(UUID playerId) {
        return playerOverrides.containsKey(playerId);
    }
    
    public void decrease(int amount) {
        int current = globalViewRadius.get();
        int newRadius = Math.max(minimumRadius, current - amount);
        setGlobalViewRadius(newRadius);
    }
    
    public void increase(int amount) {
        int current = globalViewRadius.get();
        int newRadius = Math.min(maximumRadius, current + amount);
        setGlobalViewRadius(newRadius);
    }
    
    public void setToMinimum() {
        setGlobalViewRadius(minimumRadius);
    }
    
    public void setToMaximum() {
        setGlobalViewRadius(maximumRadius);
    }
    
    public boolean isAtMinimum() {
        return globalViewRadius.get() <= minimumRadius;
    }
    
    public boolean isAtMaximum() {
        return globalViewRadius.get() >= maximumRadius;
    }
    
    public int getMinimumRadius() {
        return minimumRadius;
    }
    
    public int getMaximumRadius() {
        return maximumRadius;
    }
    
    public void onRadiusChange(BiConsumer<UUID, Integer> listener) {
        this.radiusChangeListener = listener;
    }
    
    private int clamp(int radius) {
        return Math.max(minimumRadius, Math.min(maximumRadius, radius));
    }
}
