package rubidium.qol.features;

import rubidium.core.logging.RubidiumLogger;
import rubidium.qol.QoLFeature;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class JoinLeaveMessagesFeature extends QoLFeature {
    
    public record MessagesConfig(
        List<String> joinMessages,
        List<String> leaveMessages,
        List<String> firstJoinMessages,
        String quitOnTimeoutMessage,
        boolean showPlaytime,
        boolean randomizeMessages
    ) {
        public static MessagesConfig defaults() {
            return new MessagesConfig(
                List.of(
                    "&a+ &f{player} &7joined the server",
                    "&a+ &fWelcome back, &e{player}&f!",
                    "&a+ &f{player} &7has entered the game"
                ),
                List.of(
                    "&c- &f{player} &7left the server",
                    "&c- &f{player} &7has left the game",
                    "&c- &7Goodbye, &f{player}&7!"
                ),
                List.of(
                    "&6&l>>> &eWelcome &b{player} &eto the server for the first time! &6&l<<<"
                ),
                "&c- &f{player} &7timed out",
                true,
                true
            );
        }
    }
    
    private final Map<String, Instant> joinTimes = new ConcurrentHashMap<>();
    private final Set<String> knownPlayers = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();
    
    private MessagesConfig config;
    private Consumer<String> messageHandler;
    
    public JoinLeaveMessagesFeature(RubidiumLogger logger) {
        super("join-leave-messages", "Join/Leave Messages", 
            "Customizable player join and leave messages with first-join detection",
            logger);
        this.config = MessagesConfig.defaults();
    }
    
    public void setConfig(MessagesConfig config) {
        this.config = config;
    }
    
    public MessagesConfig getConfig() {
        return config;
    }
    
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    public void loadKnownPlayers(Set<String> players) {
        knownPlayers.addAll(players);
    }
    
    public Set<String> getKnownPlayers() {
        return Collections.unmodifiableSet(knownPlayers);
    }
    
    @Override
    protected void onEnable() {
        logger.debug("Join/leave messages enabled");
    }
    
    @Override
    protected void onDisable() {
        joinTimes.clear();
    }
    
    public String onPlayerJoin(String playerId) {
        if (!enabled) return null;
        
        joinTimes.put(playerId, Instant.now());
        
        String message;
        boolean isFirstJoin = !knownPlayers.contains(playerId);
        
        if (isFirstJoin) {
            knownPlayers.add(playerId);
            message = selectMessage(config.firstJoinMessages());
        } else {
            message = selectMessage(config.joinMessages());
        }
        
        message = formatMessage(message, playerId, null);
        
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
        
        return message;
    }
    
    public String onPlayerLeave(String playerId, boolean timeout) {
        if (!enabled) return null;
        
        Duration playtime = null;
        Instant joinTime = joinTimes.remove(playerId);
        if (joinTime != null && config.showPlaytime()) {
            playtime = Duration.between(joinTime, Instant.now());
        }
        
        String message;
        if (timeout) {
            message = config.quitOnTimeoutMessage();
        } else {
            message = selectMessage(config.leaveMessages());
        }
        
        message = formatMessage(message, playerId, playtime);
        
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
        
        return message;
    }
    
    private String selectMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "{player}";
        }
        
        if (config.randomizeMessages() && messages.size() > 1) {
            return messages.get(random.nextInt(messages.size()));
        }
        
        return messages.get(0);
    }
    
    private String formatMessage(String message, String playerId, Duration playtime) {
        String formatted = message.replace("{player}", playerId);
        
        if (playtime != null) {
            formatted = formatted.replace("{playtime}", formatDuration(playtime));
        }
        
        return formatted;
    }
    
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public Optional<Duration> getSessionDuration(String playerId) {
        Instant joinTime = joinTimes.get(playerId);
        if (joinTime != null) {
            return Optional.of(Duration.between(joinTime, Instant.now()));
        }
        return Optional.empty();
    }
}
