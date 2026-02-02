package rubidium.api.title;

import rubidium.core.HytaleRuntimeBridge;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class TitleAPI {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Title");
    private static TitleAPI instance;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<UUID, ScheduledFuture<?>> activeTitles = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeActionBars = new ConcurrentHashMap<>();
    
    private TitleAPI() {}
    
    public static TitleAPI get() {
        if (instance == null) {
            instance = new TitleAPI();
        }
        return instance;
    }
    
    public void sendTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        cancelActiveTitle(playerId);
        
        TitlePacket packet = new TitlePacket(title, subtitle, fadeIn, stay, fadeOut);
        
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().sendTitle(playerId, packet);
        } else {
            LOGGER.info("[Rubidium] Title to " + playerId + ": " + title + " | " + subtitle);
        }
        
        int totalTicks = fadeIn + stay + fadeOut;
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            activeTitles.remove(playerId);
        }, totalTicks * 50L, TimeUnit.MILLISECONDS);
        
        activeTitles.put(playerId, future);
    }
    
    public void sendTitle(UUID playerId, String title, String subtitle) {
        sendTitle(playerId, title, subtitle, 10, 70, 20);
    }
    
    public void sendTitle(UUID playerId, String title) {
        sendTitle(playerId, title, "", 10, 70, 20);
    }
    
    public void sendSubtitle(UUID playerId, String subtitle) {
        sendTitle(playerId, "", subtitle, 10, 70, 20);
    }
    
    public void clearTitle(UUID playerId) {
        cancelActiveTitle(playerId);
        
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().clearTitle(playerId);
        }
    }
    
    public void sendActionBar(UUID playerId, String message) {
        activeActionBars.put(playerId, message);
        
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().sendActionBar(playerId, message);
        } else {
            LOGGER.info("[Rubidium] ActionBar to " + playerId + ": " + message);
        }
    }
    
    public void sendActionBar(UUID playerId, String message, int durationTicks) {
        sendActionBar(playerId, message);
        
        scheduler.schedule(() -> {
            if (message.equals(activeActionBars.get(playerId))) {
                activeActionBars.remove(playerId);
                clearActionBar(playerId);
            }
        }, durationTicks * 50L, TimeUnit.MILLISECONDS);
    }
    
    public void clearActionBar(UUID playerId) {
        activeActionBars.remove(playerId);
        
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().clearActionBar(playerId);
        }
    }
    
    public void sendBossBar(UUID playerId, String id, String text, float progress, BossBarColor color, BossBarStyle style) {
        BossBarPacket packet = new BossBarPacket(id, text, progress, color, style);
        
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().sendBossBar(playerId, packet);
        } else {
            LOGGER.info("[Rubidium] BossBar to " + playerId + ": " + text + " (" + (int)(progress * 100) + "%)");
        }
    }
    
    public void updateBossBar(UUID playerId, String id, String text, float progress) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().updateBossBar(playerId, id, text, progress);
        }
    }
    
    public void removeBossBar(UUID playerId, String id) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().removeBossBar(playerId, id);
        }
    }
    
    public void broadcastTitle(Collection<UUID> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (UUID playerId : players) {
            sendTitle(playerId, title, subtitle, fadeIn, stay, fadeOut);
        }
    }
    
    public void broadcastActionBar(Collection<UUID> players, String message) {
        for (UUID playerId : players) {
            sendActionBar(playerId, message);
        }
    }
    
    private void cancelActiveTitle(UUID playerId) {
        ScheduledFuture<?> existing = activeTitles.remove(playerId);
        if (existing != null) {
            existing.cancel(false);
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    public enum BossBarColor {
        PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    }
    
    public enum BossBarStyle {
        SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
    }
    
    public static class TitlePacket {
        public final String title;
        public final String subtitle;
        public final int fadeIn;
        public final int stay;
        public final int fadeOut;
        
        public TitlePacket(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            this.title = title;
            this.subtitle = subtitle;
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }
    }
    
    public static class BossBarPacket {
        public final String id;
        public final String text;
        public final float progress;
        public final BossBarColor color;
        public final BossBarStyle style;
        
        public BossBarPacket(String id, String text, float progress, BossBarColor color, BossBarStyle style) {
            this.id = id;
            this.text = text;
            this.progress = progress;
            this.color = color;
            this.style = style;
        }
    }
}
