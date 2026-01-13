package rubidium.party;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Party {
    private final UUID id;
    private String name;
    private final long createdAt;
    
    private UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> moderators;
    private final Set<UUID> bannedPlayers;
    
    private PartySettings settings;
    private PartyPrivacy privacy;
    
    private Object voiceChannel;
    private Object chatChannel;
    private final Set<UUID> sharedWaypoints;
    
    private final Map<UUID, PartyMemberStats> memberStats;
    
    public Party(UUID id, String name, UUID leader) {
        this.id = id;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.leader = leader;
        this.members = ConcurrentHashMap.newKeySet();
        this.moderators = ConcurrentHashMap.newKeySet();
        this.bannedPlayers = ConcurrentHashMap.newKeySet();
        this.settings = PartySettings.defaults();
        this.privacy = PartyPrivacy.INVITE_ONLY;
        this.sharedWaypoints = ConcurrentHashMap.newKeySet();
        this.memberStats = new ConcurrentHashMap<>();
        
        this.members.add(leader);
        this.memberStats.put(leader, new PartyMemberStats(leader));
    }
    
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getCreatedAt() { return createdAt; }
    
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { 
        this.leader = leader;
        this.moderators.add(leader);
    }
    
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public Set<UUID> getModerators() { return Collections.unmodifiableSet(moderators); }
    public Set<UUID> getBannedPlayers() { return Collections.unmodifiableSet(bannedPlayers); }
    
    public PartySettings getSettings() { return settings; }
    public void setSettings(PartySettings settings) { this.settings = settings; }
    public PartyPrivacy getPrivacy() { return privacy; }
    public void setPrivacy(PartyPrivacy privacy) { this.privacy = privacy; }
    
    public Object getVoiceChannel() { return voiceChannel; }
    public void setVoiceChannel(Object voiceChannel) { this.voiceChannel = voiceChannel; }
    public Object getChatChannel() { return chatChannel; }
    public void setChatChannel(Object chatChannel) { this.chatChannel = chatChannel; }
    public Set<UUID> getSharedWaypoints() { return sharedWaypoints; }
    
    public void addMember(UUID playerId) {
        members.add(playerId);
        memberStats.put(playerId, new PartyMemberStats(playerId));
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
        moderators.remove(playerId);
        memberStats.remove(playerId);
    }
    
    public void promoteModerator(UUID playerId) {
        if (members.contains(playerId)) {
            moderators.add(playerId);
        }
    }
    
    public void demoteModerator(UUID playerId) {
        if (!playerId.equals(leader)) {
            moderators.remove(playerId);
        }
    }
    
    public void ban(UUID playerId) {
        bannedPlayers.add(playerId);
    }
    
    public void unban(UUID playerId) {
        bannedPlayers.remove(playerId);
    }
    
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    public boolean isModerator(UUID playerId) {
        return moderators.contains(playerId) || leader.equals(playerId);
    }
    
    public boolean isLeader(UUID playerId) {
        return leader.equals(playerId);
    }
    
    public boolean isBanned(UUID playerId) {
        return bannedPlayers.contains(playerId);
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    public boolean isFull() {
        return members.size() >= settings.maxMembers();
    }
    
    public PartyMemberStats getMemberStats(UUID playerId) {
        return memberStats.get(playerId);
    }
    
    public void updateMemberStats(UUID playerId, String playerName, double health, double maxHealth, double distance) {
        PartyMemberStats stats = memberStats.get(playerId);
        if (stats != null) {
            stats.setName(playerName);
            stats.setHealth(health);
            stats.setMaxHealth(maxHealth);
            stats.setDistance(distance);
            stats.setLastUpdate(System.currentTimeMillis());
        }
    }
    
    public enum PartyPrivacy {
        OPEN,
        INVITE_ONLY,
        FRIENDS_ONLY,
        PRIVATE
    }
    
    public static class PartyMemberStats {
        private final UUID playerId;
        private String name;
        private double health;
        private double maxHealth;
        private double distance;
        private long lastUpdate;
        private long joinedAt;
        
        public PartyMemberStats(UUID playerId) {
            this.playerId = playerId;
            this.name = "";
            this.health = 20;
            this.maxHealth = 20;
            this.distance = 0;
            this.lastUpdate = System.currentTimeMillis();
            this.joinedAt = System.currentTimeMillis();
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getHealth() { return health; }
        public void setHealth(double health) { this.health = health; }
        public double getMaxHealth() { return maxHealth; }
        public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public long getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(long lastUpdate) { this.lastUpdate = lastUpdate; }
        public long getJoinedAt() { return joinedAt; }
        
        public double getHealthPercentage() {
            return maxHealth > 0 ? (health / maxHealth) : 0;
        }
    }
}
