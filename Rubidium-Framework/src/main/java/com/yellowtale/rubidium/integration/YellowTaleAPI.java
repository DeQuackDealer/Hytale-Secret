package com.yellowtale.rubidium.integration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface YellowTaleAPI {
    
    CosmeticsService getCosmetics();
    
    FriendsService getFriends();
    
    PremiumService getPremium();
    
    SessionService getSessions();
    
    AnalyticsService getAnalytics();
    
    MatchmakingService getMatchmaking();
    
    interface CosmeticsService {
        CompletableFuture<PlayerCosmetics> getPlayerCosmetics(UUID playerId);
        CompletableFuture<Boolean> validateCosmetic(UUID playerId, String cosmeticId);
        void onPlayerCosmeticChange(CosmeticChangeListener listener);
    }
    
    interface FriendsService {
        CompletableFuture<List<FriendInfo>> getFriends(UUID playerId);
        CompletableFuture<Boolean> areFriends(UUID player1, UUID player2);
        void onFriendJoin(FriendActivityListener listener);
        void onFriendLeave(FriendActivityListener listener);
    }
    
    interface PremiumService {
        CompletableFuture<Boolean> isPremium(UUID playerId);
        CompletableFuture<PremiumTier> getPremiumTier(UUID playerId);
        boolean hasFeature(UUID playerId, String featureId);
    }
    
    interface SessionService {
        Optional<UUID> getSessionId(UUID playerId);
        CompletableFuture<Boolean> transferSession(UUID playerId, String targetServer);
        void registerServer(String serverId, ServerInfo info);
        void updatePlayerCount(int count);
    }
    
    interface AnalyticsService {
        void trackEvent(String eventName, Object data);
        void trackPlayerAction(UUID playerId, String action, Object data);
        void trackPerformanceMetric(String metric, double value);
    }
    
    interface MatchmakingService {
        CompletableFuture<MatchResult> findMatch(UUID playerId, MatchCriteria criteria);
        void joinQueue(UUID playerId, String queueName);
        void leaveQueue(UUID playerId);
        void onMatchFound(MatchFoundListener listener);
    }
    
    class PlayerCosmetics {
        private String skin;
        private String cape;
        private String wings;
        private String aura;
        private String[] emotes;
        
        public String getSkin() { return skin; }
        public String getCape() { return cape; }
        public String getWings() { return wings; }
        public String getAura() { return aura; }
        public String[] getEmotes() { return emotes; }
    }
    
    class FriendInfo {
        private UUID id;
        private String username;
        private boolean online;
        private String currentServer;
        
        public UUID getId() { return id; }
        public String getUsername() { return username; }
        public boolean isOnline() { return online; }
        public String getCurrentServer() { return currentServer; }
    }
    
    enum PremiumTier {
        NONE,
        BASIC,
        PLUS,
        PRO
    }
    
    class ServerInfo {
        private String name;
        private String address;
        private int maxPlayers;
        private String gameMode;
        private String region;
        
        public ServerInfo(String name, String address, int maxPlayers, String gameMode, String region) {
            this.name = name;
            this.address = address;
            this.maxPlayers = maxPlayers;
            this.gameMode = gameMode;
            this.region = region;
        }
    }
    
    class MatchCriteria {
        private String gameMode;
        private int minPlayers;
        private int maxPlayers;
        private String region;
        private int skillLevel;
        
        public MatchCriteria gameMode(String mode) { this.gameMode = mode; return this; }
        public MatchCriteria players(int min, int max) { this.minPlayers = min; this.maxPlayers = max; return this; }
        public MatchCriteria region(String region) { this.region = region; return this; }
        public MatchCriteria skill(int level) { this.skillLevel = level; return this; }
    }
    
    class MatchResult {
        private boolean found;
        private String serverId;
        private String serverAddress;
        
        public boolean isFound() { return found; }
        public String getServerId() { return serverId; }
        public String getServerAddress() { return serverAddress; }
    }
    
    @FunctionalInterface
    interface CosmeticChangeListener {
        void onCosmeticChange(UUID playerId, String slot, String newCosmeticId);
    }
    
    @FunctionalInterface
    interface FriendActivityListener {
        void onFriendActivity(UUID playerId, UUID friendId, String activity);
    }
    
    @FunctionalInterface
    interface MatchFoundListener {
        void onMatchFound(UUID playerId, MatchResult result);
    }
}
