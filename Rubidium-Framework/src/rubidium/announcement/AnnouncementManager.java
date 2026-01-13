package rubidium.announcement;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;
import rubidium.display.ActionBar;
import rubidium.display.TitleDisplay;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AnnouncementManager {
    
    private final RubidiumLogger logger;
    private final List<ScheduledAnnouncement> scheduledAnnouncements;
    private final List<String> rotatingMessages;
    private int currentRotationIndex;
    private long rotationInterval;
    private long lastRotation;
    private ActionBar actionBar;
    private TitleDisplay titleDisplay;
    
    public AnnouncementManager(RubidiumLogger logger) {
        this.logger = logger;
        this.scheduledAnnouncements = new CopyOnWriteArrayList<>();
        this.rotatingMessages = new CopyOnWriteArrayList<>();
        this.currentRotationIndex = 0;
        this.rotationInterval = 300000;
        this.lastRotation = 0;
    }
    
    public void setDisplays(ActionBar actionBar, TitleDisplay titleDisplay) {
        this.actionBar = actionBar;
        this.titleDisplay = titleDisplay;
    }
    
    public void broadcast(Collection<Player> players, String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
        logger.info("Broadcast: " + message);
    }
    
    public void broadcastTitle(Collection<Player> players, String title, String subtitle) {
        if (titleDisplay == null) return;
        for (Player player : players) {
            titleDisplay.showTitle(player, title, subtitle);
        }
    }
    
    public void broadcastActionBar(Collection<Player> players, String message, long durationMs) {
        if (actionBar == null) return;
        for (Player player : players) {
            actionBar.send(player, message, durationMs);
        }
    }
    
    public void addRotatingMessage(String message) {
        rotatingMessages.add(message);
    }
    
    public void removeRotatingMessage(String message) {
        rotatingMessages.remove(message);
    }
    
    public void clearRotatingMessages() {
        rotatingMessages.clear();
        currentRotationIndex = 0;
    }
    
    public void setRotationInterval(long intervalMs) {
        this.rotationInterval = intervalMs;
    }
    
    public void scheduleAnnouncement(String message, long delayMs, AnnouncementType type) {
        scheduledAnnouncements.add(new ScheduledAnnouncement(
            message, System.currentTimeMillis() + delayMs, type, false, 0
        ));
    }
    
    public void scheduleRepeatingAnnouncement(String message, long intervalMs, AnnouncementType type) {
        scheduledAnnouncements.add(new ScheduledAnnouncement(
            message, System.currentTimeMillis() + intervalMs, type, true, intervalMs
        ));
    }
    
    public void cancelScheduledAnnouncements() {
        scheduledAnnouncements.clear();
    }
    
    public void tick(Collection<Player> players) {
        long now = System.currentTimeMillis();
        
        if (!rotatingMessages.isEmpty() && now - lastRotation >= rotationInterval) {
            String message = rotatingMessages.get(currentRotationIndex);
            broadcast(players, message);
            currentRotationIndex = (currentRotationIndex + 1) % rotatingMessages.size();
            lastRotation = now;
        }
        
        Iterator<ScheduledAnnouncement> it = scheduledAnnouncements.iterator();
        List<ScheduledAnnouncement> toAdd = new ArrayList<>();
        
        while (it.hasNext()) {
            ScheduledAnnouncement ann = it.next();
            if (now >= ann.triggerTime()) {
                sendAnnouncement(players, ann);
                
                if (ann.repeating()) {
                    toAdd.add(new ScheduledAnnouncement(
                        ann.message(), now + ann.repeatInterval(), ann.type(), true, ann.repeatInterval()
                    ));
                }
                it.remove();
            }
        }
        
        scheduledAnnouncements.addAll(toAdd);
    }
    
    private void sendAnnouncement(Collection<Player> players, ScheduledAnnouncement ann) {
        switch (ann.type()) {
            case CHAT -> broadcast(players, ann.message());
            case TITLE -> broadcastTitle(players, ann.message(), null);
            case ACTIONBAR -> broadcastActionBar(players, ann.message(), 3000);
        }
    }
    
    public enum AnnouncementType { CHAT, TITLE, ACTIONBAR }
    
    public record ScheduledAnnouncement(
        String message,
        long triggerTime,
        AnnouncementType type,
        boolean repeating,
        long repeatInterval
    ) {}
}
