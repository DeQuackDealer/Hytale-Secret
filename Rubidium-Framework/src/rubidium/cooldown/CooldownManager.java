package rubidium.cooldown;

import rubidium.api.player.Player;
import rubidium.display.ActionBar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    
    private final Map<String, CooldownEntry> cooldowns;
    private final Map<UUID, Map<String, Long>> playerCooldowns;
    private ActionBar actionBar;
    
    public CooldownManager() {
        this.cooldowns = new ConcurrentHashMap<>();
        this.playerCooldowns = new ConcurrentHashMap<>();
    }
    
    public void setActionBar(ActionBar actionBar) {
        this.actionBar = actionBar;
    }
    
    public CooldownManager registerCooldown(String id, long durationMs) {
        return registerCooldown(id, durationMs, null, true);
    }
    
    public CooldownManager registerCooldown(String id, long durationMs, String displayName, boolean showFeedback) {
        cooldowns.put(id, new CooldownEntry(id, durationMs, displayName != null ? displayName : id, showFeedback));
        return this;
    }
    
    public boolean isOnCooldown(Player player, String cooldownId) {
        Map<String, Long> pcd = playerCooldowns.get(player.getUUID());
        if (pcd == null) return false;
        
        Long endTime = pcd.get(cooldownId);
        if (endTime == null) return false;
        
        return System.currentTimeMillis() < endTime;
    }
    
    public long getRemainingTime(Player player, String cooldownId) {
        Map<String, Long> pcd = playerCooldowns.get(player.getUUID());
        if (pcd == null) return 0;
        
        Long endTime = pcd.get(cooldownId);
        if (endTime == null) return 0;
        
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    public boolean tryUse(Player player, String cooldownId) {
        if (isOnCooldown(player, cooldownId)) {
            showCooldownFeedback(player, cooldownId);
            return false;
        }
        
        startCooldown(player, cooldownId);
        return true;
    }
    
    public void startCooldown(Player player, String cooldownId) {
        CooldownEntry entry = cooldowns.get(cooldownId);
        if (entry == null) return;
        
        playerCooldowns
            .computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
            .put(cooldownId, System.currentTimeMillis() + entry.durationMs());
    }
    
    public void startCooldown(Player player, String cooldownId, long customDurationMs) {
        playerCooldowns
            .computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
            .put(cooldownId, System.currentTimeMillis() + customDurationMs);
    }
    
    public void clearCooldown(Player player, String cooldownId) {
        Map<String, Long> pcd = playerCooldowns.get(player.getUUID());
        if (pcd != null) {
            pcd.remove(cooldownId);
        }
    }
    
    public void clearAllCooldowns(Player player) {
        playerCooldowns.remove(player.getUUID());
    }
    
    public void reduceCooldown(Player player, String cooldownId, long reduceByMs) {
        Map<String, Long> pcd = playerCooldowns.get(player.getUUID());
        if (pcd == null) return;
        
        Long endTime = pcd.get(cooldownId);
        if (endTime != null) {
            pcd.put(cooldownId, endTime - reduceByMs);
        }
    }
    
    private void showCooldownFeedback(Player player, String cooldownId) {
        CooldownEntry entry = cooldowns.get(cooldownId);
        if (entry == null || !entry.showFeedback() || actionBar == null) return;
        
        long remaining = getRemainingTime(player, cooldownId);
        String message = String.format("%s on cooldown: %.1fs", entry.displayName(), remaining / 1000.0);
        actionBar.sendPriority(player, message, 1000, ActionBar.Priority.HIGH);
    }
    
    public float getCooldownProgress(Player player, String cooldownId) {
        CooldownEntry entry = cooldowns.get(cooldownId);
        if (entry == null) return 1.0f;
        
        long remaining = getRemainingTime(player, cooldownId);
        if (remaining <= 0) return 1.0f;
        
        return 1.0f - (float) remaining / entry.durationMs();
    }
    
    public void tick() {
        long now = System.currentTimeMillis();
        playerCooldowns.values().forEach(pcd -> 
            pcd.entrySet().removeIf(e -> e.getValue() < now)
        );
        playerCooldowns.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
    
    public record CooldownEntry(String id, long durationMs, String displayName, boolean showFeedback) {}
}
