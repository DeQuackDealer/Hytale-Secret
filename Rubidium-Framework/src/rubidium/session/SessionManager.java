package rubidium.session;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    
    private final RubidiumLogger logger;
    private final Map<UUID, PlayerSession> sessions;
    
    public SessionManager(RubidiumLogger logger) {
        this.logger = logger;
        this.sessions = new ConcurrentHashMap<>();
    }
    
    public PlayerSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUUID(), 
            id -> new PlayerSession(id, player.getName()));
    }
    
    public Optional<PlayerSession> findSession(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }
    
    public void createSession(Player player) {
        PlayerSession session = new PlayerSession(player.getUUID(), player.getName());
        sessions.put(player.getUUID(), session);
        logger.debug("Created session for: " + player.getName());
    }
    
    public void destroySession(Player player) {
        PlayerSession session = sessions.remove(player.getUUID());
        if (session != null) {
            session.endSession();
            logger.debug("Destroyed session for: " + player.getName());
        }
    }
    
    public Collection<PlayerSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }
    
    public int getOnlineCount() {
        return sessions.size();
    }
    
    public static class PlayerSession {
        private final UUID playerId;
        private final String playerName;
        private final long loginTime;
        private long lastActivity;
        private final Map<String, Object> data;
        private SessionState state;
        
        public PlayerSession(UUID playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.loginTime = System.currentTimeMillis();
            this.lastActivity = loginTime;
            this.data = new ConcurrentHashMap<>();
            this.state = SessionState.ACTIVE;
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public long getLoginTime() { return loginTime; }
        public long getLastActivity() { return lastActivity; }
        public SessionState getState() { return state; }
        
        public long getSessionDuration() {
            return System.currentTimeMillis() - loginTime;
        }
        
        public long getIdleTime() {
            return System.currentTimeMillis() - lastActivity;
        }
        
        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
            if (state == SessionState.AFK) {
                state = SessionState.ACTIVE;
            }
        }
        
        public void setAfk() {
            this.state = SessionState.AFK;
        }
        
        public void endSession() {
            this.state = SessionState.ENDED;
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getData(String key) {
            return (T) data.get(key);
        }
        
        public <T> T getData(String key, T defaultValue) {
            T value = getData(key);
            return value != null ? value : defaultValue;
        }
        
        public void setData(String key, Object value) {
            data.put(key, value);
        }
        
        public void removeData(String key) {
            data.remove(key);
        }
        
        public boolean hasData(String key) {
            return data.containsKey(key);
        }
        
        public void clearData() {
            data.clear();
        }
        
        public void incrementCounter(String key) {
            int current = getData(key, 0);
            setData(key, current + 1);
        }
        
        public void addToList(String key, Object item) {
            List<Object> list = getData(key);
            if (list == null) {
                list = new ArrayList<>();
                setData(key, list);
            }
            list.add(item);
        }
    }
    
    public enum SessionState {
        ACTIVE,
        AFK,
        ENDED
    }
}
