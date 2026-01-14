package rubidium.voice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceGroup {
    
    private final String id;
    private String name;
    private UUID owner;
    private String password;
    private boolean isPublic;
    private boolean persistent;
    
    private final Set<UUID> members;
    private final Set<UUID> invited;
    private final Map<UUID, MemberSettings> memberSettings;
    
    private int maxMembers;
    private long createdAt;
    private long lastActivity;
    
    public VoiceGroup(String id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.password = null;
        this.isPublic = false;
        this.persistent = false;
        this.members = ConcurrentHashMap.newKeySet();
        this.invited = ConcurrentHashMap.newKeySet();
        this.memberSettings = new ConcurrentHashMap<>();
        this.maxMembers = 16;
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = System.currentTimeMillis();
        
        members.add(owner);
        memberSettings.put(owner, new MemberSettings());
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    
    public boolean hasPassword() { return password != null && !password.isEmpty(); }
    public void setPassword(String password) { this.password = password; }
    public boolean checkPassword(String attempt) {
        if (!hasPassword()) return true;
        return password.equals(attempt);
    }
    
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
    
    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
    
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public int getMemberCount() { return members.size(); }
    
    public boolean isFull() { return members.size() >= maxMembers; }
    
    public boolean addMember(UUID playerId) {
        if (isFull()) return false;
        
        if (members.add(playerId)) {
            memberSettings.put(playerId, new MemberSettings());
            invited.remove(playerId);
            lastActivity = System.currentTimeMillis();
            return true;
        }
        return false;
    }
    
    public boolean addMember(UUID playerId, String attemptedPassword) {
        if (!checkPassword(attemptedPassword)) return false;
        return addMember(playerId);
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
        memberSettings.remove(playerId);
        lastActivity = System.currentTimeMillis();
        
        if (playerId.equals(owner) && !members.isEmpty()) {
            owner = members.iterator().next();
        }
    }
    
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    public boolean isOwner(UUID playerId) {
        return owner.equals(playerId);
    }
    
    public void invite(UUID playerId) {
        invited.add(playerId);
    }
    
    public void uninvite(UUID playerId) {
        invited.remove(playerId);
    }
    
    public boolean isInvited(UUID playerId) {
        return invited.contains(playerId);
    }
    
    public boolean canJoin(UUID playerId) {
        if (isFull()) return false;
        if (isPublic) return true;
        if (isInvited(playerId)) return true;
        return !hasPassword();
    }
    
    public MemberSettings getMemberSettings(UUID playerId) {
        return memberSettings.get(playerId);
    }
    
    public long getCreatedAt() { return createdAt; }
    public long getLastActivity() { return lastActivity; }
    public void updateActivity() { lastActivity = System.currentTimeMillis(); }
    
    public boolean isEmpty() {
        return members.isEmpty();
    }
    
    public static class MemberSettings {
        private float volume = 1.0f;
        private boolean muted = false;
        private boolean leader = false;
        
        public float getVolume() { return volume; }
        public void setVolume(float volume) { this.volume = Math.max(0, Math.min(2.0f, volume)); }
        public boolean isMuted() { return muted; }
        public void setMuted(boolean muted) { this.muted = muted; }
        public boolean isLeader() { return leader; }
        public void setLeader(boolean leader) { this.leader = leader; }
    }
}
