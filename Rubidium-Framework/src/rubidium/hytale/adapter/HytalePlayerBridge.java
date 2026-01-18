package rubidium.hytale.adapter;

import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class HytalePlayerBridge {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Bridge");
    private static HytalePlayerBridge instance;
    
    private final Map<UUID, PlayerRef> playerRefs = new ConcurrentHashMap<>();
    private final Map<UUID, PageManager> pageManagers = new ConcurrentHashMap<>();
    private final Map<UUID, HudManager> hudManagers = new ConcurrentHashMap<>();
    
    public static HytalePlayerBridge get() {
        if (instance == null) {
            instance = new HytalePlayerBridge();
        }
        return instance;
    }
    
    public void registerPlayer(UUID playerId, PlayerRef playerRef, PageManager pageManager, HudManager hudManager) {
        LOGGER.info("[Rubidium] Registering player bridge: " + playerId);
        if (playerRef != null) playerRefs.put(playerId, playerRef);
        if (pageManager != null) pageManagers.put(playerId, pageManager);
        if (hudManager != null) hudManagers.put(playerId, hudManager);
    }
    
    public void unregisterPlayer(UUID playerId) {
        LOGGER.info("[Rubidium] Unregistering player bridge: " + playerId);
        playerRefs.remove(playerId);
        pageManagers.remove(playerId);
        hudManagers.remove(playerId);
    }
    
    public Optional<PlayerRef> getPlayerRef(UUID playerId) {
        return Optional.ofNullable(playerRefs.get(playerId));
    }
    
    public Optional<PageManager> getPageManager(UUID playerId) {
        return Optional.ofNullable(pageManagers.get(playerId));
    }
    
    public Optional<HudManager> getHudManager(UUID playerId) {
        return Optional.ofNullable(hudManagers.get(playerId));
    }
    
    public boolean isRegistered(UUID playerId) {
        return playerRefs.containsKey(playerId);
    }
}
