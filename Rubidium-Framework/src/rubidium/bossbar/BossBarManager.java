package rubidium.bossbar;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {
    
    private final RubidiumLogger logger;
    private final Map<String, BossBar> bossBars;
    
    public BossBarManager(RubidiumLogger logger) {
        this.logger = logger;
        this.bossBars = new ConcurrentHashMap<>();
    }
    
    public BossBar createBossBar(String id, String title) {
        BossBar bar = new BossBar(id, title);
        bossBars.put(id, bar);
        logger.debug("Created boss bar: " + id);
        return bar;
    }
    
    public BossBar createBossBar(String id, String title, float progress, BossBar.Color color, BossBar.Style style) {
        BossBar bar = new BossBar(id, title, progress, color, style);
        bossBars.put(id, bar);
        logger.debug("Created boss bar: " + id);
        return bar;
    }
    
    public Optional<BossBar> getBossBar(String id) {
        return Optional.ofNullable(bossBars.get(id));
    }
    
    public void removeBossBar(String id) {
        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            logger.debug("Removed boss bar: " + id);
        }
    }
    
    public void showToPlayer(Player player, String bossBarId) {
        BossBar bar = bossBars.get(bossBarId);
        if (bar != null) {
            bar.addViewer(player);
        }
    }
    
    public void hideFromPlayer(Player player, String bossBarId) {
        BossBar bar = bossBars.get(bossBarId);
        if (bar != null) {
            bar.removeViewer(player);
        }
    }
    
    public void hideAllFromPlayer(Player player) {
        for (BossBar bar : bossBars.values()) {
            bar.removeViewer(player);
        }
    }
    
    public Collection<BossBar> getAllBossBars() {
        return Collections.unmodifiableCollection(bossBars.values());
    }
    
    public void onPlayerQuit(Player player) {
        for (BossBar bar : bossBars.values()) {
            bar.getViewers().remove(player.getUUID());
        }
    }
}
