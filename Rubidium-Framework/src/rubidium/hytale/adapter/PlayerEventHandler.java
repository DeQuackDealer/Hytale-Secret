package rubidium.hytale.adapter;

import com.hypixel.hytale.server.api.player.Player;
import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer;
import com.hypixel.hytale.server.core.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;

import rubidium.api.server.Server;
import rubidium.api.ui.UIInitializer;
import rubidium.core.HytaleRuntimeBridge;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class PlayerEventHandler {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Events");
    private static PlayerEventHandler instance;
    
    private final Map<UUID, HytalePlayerImpl> playerMap = new ConcurrentHashMap<>();
    private boolean registered = false;
    private EventRegistry pluginEventRegistry;
    
    private PlayerEventHandler() {}
    
    public static PlayerEventHandler get() {
        if (instance == null) {
            instance = new PlayerEventHandler();
        }
        return instance;
    }
    
    public void registerEvents() {
        registerEvents(null);
    }
    
    public void registerEvents(EventRegistry pluginRegistry) {
        if (registered) {
            LOGGER.warning("[Rubidium] Event handlers already registered");
            return;
        }
        
        LOGGER.info("[Rubidium] Registering Hytale event handlers...");
        
        EventRegistry registry = pluginRegistry != null ? pluginRegistry : EventRegistry.get();
        this.pluginEventRegistry = registry;
        
        registry.register(AddPlayerToWorldEvent.class, this::handlePlayerJoin);
        registry.register(DrainPlayerFromWorldEvent.class, this::handlePlayerQuit);
        
        registered = true;
        LOGGER.info("[Rubidium] Event handlers registered via " + 
            (pluginRegistry != null ? "plugin registry" : "static registry"));
    }
    
    public void handlePlayerJoin(AddPlayerToWorldEvent event) {
        ServerPlayer serverPlayer = event.getPlayer();
        if (serverPlayer == null) {
            LOGGER.warning("[Rubidium] Received join event with null player");
            return;
        }
        
        UUID playerId = serverPlayer.getUuid();
        String playerName = serverPlayer.getUsername();
        
        LOGGER.info("[Rubidium] Player joining: " + playerName + " (" + playerId + ")");
        
        HytalePlayerImpl player = HytalePlayerImpl.wrap(serverPlayer);
        if (player == null) {
            LOGGER.warning("[Rubidium] Failed to wrap player: " + playerName);
            return;
        }
        
        playerMap.put(playerId, player);
        
        HytalePlayerBridge.get().registerPlayer(
            playerId,
            null,
            serverPlayer.getPageManager(),
            serverPlayer.getHudManager()
        );
        
        Object playerRef = extractPlayerRef(serverPlayer);
        Object entityStore = extractEntityStore(event);
        HytaleRuntimeBridge.get().registerPlayerRef(playerId, playerRef, entityStore);
        
        if (playerRef != null && entityStore != null) {
            LOGGER.info("[Rubidium] Registered player with real Hytale ECS refs: " + playerName);
        } else {
            LOGGER.info("[Rubidium] Registered player with fallback refs: " + playerName);
        }
        
        RubidiumPlayerImpl rubidiumPlayer = new RubidiumPlayerImpl(player);
        Server.registerPlayer(rubidiumPlayer);
        
        try {
            UIInitializer.get().onPlayerJoin(player);
            LOGGER.info("[Rubidium] UI initialized for player: " + playerName);
        } catch (Exception e) {
            LOGGER.severe("[Rubidium] Failed to initialize UI for player " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Object extractPlayerRef(ServerPlayer serverPlayer) {
        try {
            Method getEntityRef = serverPlayer.getClass().getMethod("getEntityRef");
            return getEntityRef.invoke(serverPlayer);
        } catch (Exception e1) {
            try {
                Method getRef = serverPlayer.getClass().getMethod("getRef");
                return getRef.invoke(serverPlayer);
            } catch (Exception e2) {
                try {
                    Method getEntity = serverPlayer.getClass().getMethod("getEntity");
                    Object entity = getEntity.invoke(serverPlayer);
                    if (entity != null) {
                        Method asRef = entity.getClass().getMethod("asRef");
                        return asRef.invoke(entity);
                    }
                } catch (Exception e3) {
                    LOGGER.fine("[Rubidium] Could not extract player ref: " + e3.getMessage());
                }
            }
        }
        return null;
    }
    
    private Object extractEntityStore(AddPlayerToWorldEvent event) {
        try {
            Method getWorld = event.getClass().getMethod("getWorld");
            Object world = getWorld.invoke(event);
            if (world != null) {
                Method getEntityStore = world.getClass().getMethod("getEntityStore");
                return getEntityStore.invoke(world);
            }
        } catch (Exception e1) {
            try {
                Method getEntityStore = event.getClass().getMethod("getEntityStore");
                return getEntityStore.invoke(event);
            } catch (Exception e2) {
                LOGGER.fine("[Rubidium] Could not extract entity store: " + e2.getMessage());
            }
        }
        return null;
    }
    
    public void handlePlayerQuit(DrainPlayerFromWorldEvent event) {
        ServerPlayer serverPlayer = event.getPlayer();
        if (serverPlayer == null) return;
        
        UUID playerId = serverPlayer.getUuid();
        String playerName = serverPlayer.getUsername();
        
        LOGGER.info("[Rubidium] Player leaving: " + playerName + " (" + playerId + ")");
        
        HytalePlayerImpl player = playerMap.remove(playerId);
        
        try {
            UIInitializer.get().onPlayerQuit(player, playerId);
            LOGGER.info("[Rubidium] UI cleaned up for player: " + playerName);
        } catch (Exception e) {
            LOGGER.warning("[Rubidium] Failed to cleanup UI for player " + playerName + ": " + e.getMessage());
        }
        
        HytalePlayerBridge.get().unregisterPlayer(playerId);
        Server.unregisterPlayer(playerId);
        HytaleRuntimeBridge.get().unregisterPlayer(playerId);
        
        if (player != null) {
            player.setOnline(false);
        }
    }
    
    public Player getPlayer(UUID playerId) {
        return playerMap.get(playerId);
    }
    
    public void shutdown() {
        LOGGER.info("[Rubidium] Cleaning up player event handlers...");
        
        for (Map.Entry<UUID, HytalePlayerImpl> entry : playerMap.entrySet()) {
            try {
                UIInitializer.get().onPlayerQuit(entry.getValue(), entry.getKey());
            } catch (Exception e) {
                LOGGER.warning("[Rubidium] Error during player cleanup: " + e.getMessage());
            }
        }
        
        playerMap.clear();
        UIInitializer.get().shutdown();
    }
}
