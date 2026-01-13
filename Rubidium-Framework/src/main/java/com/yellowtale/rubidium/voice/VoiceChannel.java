package com.yellowtale.rubidium.voice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceChannel {
    private final String id;
    private String name;
    private ChannelType type;
    private UUID owner;
    
    private final Set<UUID> members;
    private final Set<UUID> moderators;
    private final Set<UUID> banned;
    
    private final ChannelSettings settings;
    
    public VoiceChannel(String id, String name, ChannelType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.owner = null;
        this.members = ConcurrentHashMap.newKeySet();
        this.moderators = ConcurrentHashMap.newKeySet();
        this.banned = ConcurrentHashMap.newKeySet();
        this.settings = new ChannelSettings();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ChannelType getType() { return type; }
    public void setType(ChannelType type) { this.type = type; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public Set<UUID> getModerators() { return Collections.unmodifiableSet(moderators); }
    public Set<UUID> getBanned() { return Collections.unmodifiableSet(banned); }
    public ChannelSettings getSettings() { return settings; }
    
    public void addMember(UUID playerId) {
        if (!banned.contains(playerId)) {
            members.add(playerId);
        }
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
        moderators.remove(playerId);
    }
    
    public void addModerator(UUID playerId) {
        if (members.contains(playerId)) {
            moderators.add(playerId);
        }
    }
    
    public void removeModerator(UUID playerId) {
        moderators.remove(playerId);
    }
    
    public void ban(UUID playerId) {
        banned.add(playerId);
        removeMember(playerId);
    }
    
    public void unban(UUID playerId) {
        banned.remove(playerId);
    }
    
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    public boolean isModerator(UUID playerId) {
        return moderators.contains(playerId) || playerId.equals(owner);
    }
    
    public boolean isBanned(UUID playerId) {
        return banned.contains(playerId);
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    public enum ChannelType {
        PROXIMITY,
        GLOBAL,
        PARTY,
        TEAM,
        PRIVATE,
        ADMIN
    }
    
    public static class ChannelSettings {
        private boolean persistent;
        private boolean requiresPassword;
        private String password;
        private int maxMembers;
        private boolean allowSpectators;
        private boolean pushToTalk;
        
        public ChannelSettings() {
            this.persistent = false;
            this.requiresPassword = false;
            this.password = null;
            this.maxMembers = 32;
            this.allowSpectators = true;
            this.pushToTalk = false;
        }
        
        public boolean isPersistent() { return persistent; }
        public void setPersistent(boolean persistent) { this.persistent = persistent; }
        public boolean isRequiresPassword() { return requiresPassword; }
        public void setRequiresPassword(boolean requiresPassword) { this.requiresPassword = requiresPassword; }
        public String getPassword() { return password; }
        public void setPassword(String password) { 
            this.password = password; 
            this.requiresPassword = password != null && !password.isEmpty();
        }
        public int getMaxMembers() { return maxMembers; }
        public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
        public boolean isAllowSpectators() { return allowSpectators; }
        public void setAllowSpectators(boolean allowSpectators) { this.allowSpectators = allowSpectators; }
        public boolean isPushToTalk() { return pushToTalk; }
        public void setPushToTalk(boolean pushToTalk) { this.pushToTalk = pushToTalk; }
        
        public boolean checkPassword(String attempt) {
            if (!requiresPassword || password == null) return true;
            return password.equals(attempt);
        }
    }
}
