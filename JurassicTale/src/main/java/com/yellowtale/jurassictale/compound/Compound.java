package com.yellowtale.jurassictale.compound;

import com.yellowtale.jurassictale.compound.power.PowerCore;
import com.yellowtale.jurassictale.compound.power.PowerGrid;
import com.yellowtale.jurassictale.compound.power.PowerDevice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Compound {
    
    private final UUID id;
    private final UUID ownerId;
    private String name;
    
    private final PowerCore powerCore;
    private final PowerGrid powerGrid;
    
    private final Map<UUID, CompoundMember> members;
    private final Map<UUID, NPC> npcs;
    private final Map<UUID, PowerDevice> devices;
    
    private final Set<UUID> authorizedPlayers;
    
    private long createdAt;
    private boolean lockdownActive;
    private int sabotageRisk;
    
    public Compound(UUID id, UUID ownerId, String name, PowerCore powerCore, PowerGrid powerGrid) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.powerCore = powerCore;
        this.powerGrid = powerGrid;
        this.members = new ConcurrentHashMap<>();
        this.npcs = new ConcurrentHashMap<>();
        this.devices = new ConcurrentHashMap<>();
        this.authorizedPlayers = ConcurrentHashMap.newKeySet();
        this.createdAt = System.currentTimeMillis();
        this.lockdownActive = false;
        this.sabotageRisk = 0;
        
        authorizedPlayers.add(ownerId);
    }
    
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public PowerCore getPowerCore() { return powerCore; }
    public PowerGrid getPowerGrid() { return powerGrid; }
    
    public void addMember(UUID playerId, CompoundRole role) {
        members.put(playerId, new CompoundMember(playerId, role));
        authorizedPlayers.add(playerId);
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
        if (!playerId.equals(ownerId)) {
            authorizedPlayers.remove(playerId);
        }
    }
    
    public Optional<CompoundMember> getMember(UUID playerId) {
        return Optional.ofNullable(members.get(playerId));
    }
    
    public boolean isAuthorized(UUID playerId) {
        return authorizedPlayers.contains(playerId);
    }
    
    public void hireNPC(NPC npc) {
        npcs.put(npc.getId(), npc);
        sabotageRisk += npc.getType().getSabotageRisk();
    }
    
    public void fireNPC(UUID npcId) {
        NPC npc = npcs.remove(npcId);
        if (npc != null) {
            sabotageRisk -= npc.getType().getSabotageRisk();
        }
    }
    
    public long getNPCPayroll() {
        return npcs.values().stream()
            .mapToLong(npc -> npc.getType().getSalaryPerHour())
            .sum();
    }
    
    public void addDevice(PowerDevice device) {
        devices.put(device.getId(), device);
        powerGrid.addDevice(device);
    }
    
    public void removeDevice(UUID deviceId) {
        devices.remove(deviceId);
        powerGrid.removeDevice(deviceId);
    }
    
    public void tick() {
        powerGrid.tick();
    }
    
    public boolean isLockdownActive() { return lockdownActive; }
    public void setLockdownActive(boolean active) { this.lockdownActive = active; }
    
    public int getSabotageRisk() { return sabotageRisk; }
    public long getCreatedAt() { return createdAt; }
    
    public int getMemberCount() { return members.size(); }
    public int getNPCCount() { return npcs.size(); }
    public int getDeviceCount() { return devices.size(); }
    
    public record CompoundMember(UUID playerId, CompoundRole role) {}
    
    public enum CompoundRole {
        OWNER, ADMIN, MEMBER, VISITOR
    }
    
    public record NPC(UUID id, NPCType type, String name) {
        public enum NPCType {
            SECURITY_GUARD("Security Guard", 100, 5),
            TECHNICIAN("Technician", 150, 3),
            MEDIC("Medic", 120, 2),
            SCIENTIST("Scientist", 200, 4),
            MANAGER("Manager", 250, 6);
            
            private final String displayName;
            private final long salaryPerHour;
            private final int sabotageRisk;
            
            NPCType(String displayName, long salaryPerHour, int sabotageRisk) {
                this.displayName = displayName;
                this.salaryPerHour = salaryPerHour;
                this.sabotageRisk = sabotageRisk;
            }
            
            public String getDisplayName() { return displayName; }
            public long getSalaryPerHour() { return salaryPerHour; }
            public int getSabotageRisk() { return sabotageRisk; }
        }
    }
}
