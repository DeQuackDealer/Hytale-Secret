package rubidium.display;

import rubidium.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBar {
    
    private final Map<UUID, ActionBarEntry> activeMessages;
    private final Map<UUID, Queue<ActionBarEntry>> messageQueue;
    
    public ActionBar() {
        this.activeMessages = new ConcurrentHashMap<>();
        this.messageQueue = new ConcurrentHashMap<>();
    }
    
    public void send(Player player, String message) {
        send(player, message, 2000);
    }
    
    public void send(Player player, String message, long durationMs) {
        ActionBarEntry entry = new ActionBarEntry(message, System.currentTimeMillis(), durationMs, Priority.NORMAL);
        setActive(player, entry);
    }
    
    public void sendPriority(Player player, String message, long durationMs, Priority priority) {
        ActionBarEntry entry = new ActionBarEntry(message, System.currentTimeMillis(), durationMs, priority);
        
        ActionBarEntry current = activeMessages.get(player.getUUID());
        if (current == null || priority.ordinal() >= current.priority().ordinal()) {
            setActive(player, entry);
        } else {
            messageQueue.computeIfAbsent(player.getUUID(), k -> new LinkedList<>()).offer(entry);
        }
    }
    
    private void setActive(Player player, ActionBarEntry entry) {
        activeMessages.put(player.getUUID(), entry);
        player.sendPacket(new ActionBarPacket(entry.message()));
    }
    
    public void tick(Collection<Player> players) {
        long now = System.currentTimeMillis();
        
        Iterator<Map.Entry<UUID, ActionBarEntry>> it = activeMessages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActionBarEntry> entry = it.next();
            ActionBarEntry msg = entry.getValue();
            
            if (now - msg.startTime() > msg.durationMs()) {
                it.remove();
                
                Queue<ActionBarEntry> queue = messageQueue.get(entry.getKey());
                if (queue != null && !queue.isEmpty()) {
                    ActionBarEntry next = queue.poll();
                    players.stream()
                        .filter(p -> p.getUUID().equals(entry.getKey()))
                        .findFirst()
                        .ifPresent(p -> setActive(p, next));
                }
            }
        }
    }
    
    public void clear(Player player) {
        activeMessages.remove(player.getUUID());
        messageQueue.remove(player.getUUID());
        player.sendPacket(new ActionBarPacket(""));
    }
    
    public enum Priority { LOW, NORMAL, HIGH, CRITICAL }
    
    public record ActionBarEntry(String message, long startTime, long durationMs, Priority priority) {}
    public record ActionBarPacket(String message) {}
}
