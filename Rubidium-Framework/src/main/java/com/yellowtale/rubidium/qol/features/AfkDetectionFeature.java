package com.yellowtale.rubidium.qol.features;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.rubidium.qol.QoLFeature;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AfkDetectionFeature extends QoLFeature {
    
    public record AfkConfig(
        Duration timeout,
        boolean kickAfkPlayers,
        Duration kickDelay,
        String afkMessage,
        String returnMessage,
        boolean broadcastAfk
    ) {
        public static AfkConfig defaults() {
            return new AfkConfig(
                Duration.ofMinutes(5),
                false,
                Duration.ofMinutes(30),
                "{player} is now AFK",
                "{player} is no longer AFK",
                true
            );
        }
    }
    
    public record PlayerActivity(
        String playerId,
        Instant lastActivity,
        boolean isAfk,
        Instant afkSince
    ) {}
    
    private final Map<String, PlayerActivity> playerActivities = new ConcurrentHashMap<>();
    private AfkConfig config;
    private Consumer<String> messageHandler;
    private Consumer<String> kickHandler;
    
    public AfkDetectionFeature(RubidiumLogger logger) {
        super("afk-detection", "AFK Detection", 
            "Automatically detects inactive players and optionally kicks them after extended AFK periods",
            logger);
        this.config = AfkConfig.defaults();
    }
    
    public void setConfig(AfkConfig config) {
        this.config = config;
    }
    
    public AfkConfig getConfig() {
        return config;
    }
    
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    public void setKickHandler(Consumer<String> handler) {
        this.kickHandler = handler;
    }
    
    @Override
    protected void onEnable() {
        logger.debug("AFK detection enabled with {}s timeout", config.timeout().toSeconds());
    }
    
    @Override
    protected void onDisable() {
        playerActivities.clear();
    }
    
    public void onPlayerJoin(String playerId) {
        if (!enabled) return;
        playerActivities.put(playerId, new PlayerActivity(playerId, Instant.now(), false, null));
    }
    
    public void onPlayerLeave(String playerId) {
        playerActivities.remove(playerId);
    }
    
    public void onPlayerActivity(String playerId) {
        if (!enabled) return;
        
        PlayerActivity current = playerActivities.get(playerId);
        if (current == null) {
            onPlayerJoin(playerId);
            return;
        }
        
        if (current.isAfk()) {
            playerActivities.put(playerId, new PlayerActivity(playerId, Instant.now(), false, null));
            
            if (config.broadcastAfk() && messageHandler != null) {
                String message = config.returnMessage().replace("{player}", playerId);
                messageHandler.accept(message);
            }
            logger.debug("Player {} is no longer AFK", playerId);
        } else {
            playerActivities.put(playerId, new PlayerActivity(playerId, Instant.now(), false, null));
        }
    }
    
    public boolean isPlayerAfk(String playerId) {
        PlayerActivity activity = playerActivities.get(playerId);
        return activity != null && activity.isAfk();
    }
    
    public Set<String> getAfkPlayers() {
        Set<String> afkPlayers = new HashSet<>();
        for (PlayerActivity activity : playerActivities.values()) {
            if (activity.isAfk()) {
                afkPlayers.add(activity.playerId());
            }
        }
        return afkPlayers;
    }
    
    public Optional<Duration> getAfkDuration(String playerId) {
        PlayerActivity activity = playerActivities.get(playerId);
        if (activity != null && activity.isAfk() && activity.afkSince() != null) {
            return Optional.of(Duration.between(activity.afkSince(), Instant.now()));
        }
        return Optional.empty();
    }
    
    @Override
    public void tick() {
        if (!enabled) return;
        
        Instant now = Instant.now();
        List<String> toKick = new ArrayList<>();
        
        for (Map.Entry<String, PlayerActivity> entry : playerActivities.entrySet()) {
            PlayerActivity activity = entry.getValue();
            Duration inactiveDuration = Duration.between(activity.lastActivity(), now);
            
            if (!activity.isAfk() && inactiveDuration.compareTo(config.timeout()) > 0) {
                PlayerActivity newActivity = new PlayerActivity(
                    activity.playerId(), 
                    activity.lastActivity(), 
                    true, 
                    now
                );
                playerActivities.put(entry.getKey(), newActivity);
                
                if (config.broadcastAfk() && messageHandler != null) {
                    String message = config.afkMessage().replace("{player}", activity.playerId());
                    messageHandler.accept(message);
                }
                logger.debug("Player {} is now AFK", activity.playerId());
            }
            
            if (activity.isAfk() && config.kickAfkPlayers() && activity.afkSince() != null) {
                Duration afkDuration = Duration.between(activity.afkSince(), now);
                if (afkDuration.compareTo(config.kickDelay()) > 0) {
                    toKick.add(activity.playerId());
                }
            }
        }
        
        for (String playerId : toKick) {
            playerActivities.remove(playerId);
            if (kickHandler != null) {
                kickHandler.accept(playerId);
            }
            logger.info("Kicked player {} for being AFK too long", playerId);
        }
    }
}
