package rubidium.hytale.adapter;

import rubidium.api.player.Player;
import rubidium.hytale.player.HytalePlayerWrapper;
import rubidium.hytale.entity.HytaleEntityWrapper;
import rubidium.hytale.event.HytaleEventBridge;
import rubidium.hytale.packet.HytalePacketAdapter;
import rubidium.core.RubidiumCore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class HytaleAdapter {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytaleAdapter");
    
    private static HytaleAdapter instance;
    
    private final Map<UUID, HytalePlayerWrapper> playerCache;
    private final Map<Long, HytaleEntityWrapper> entityCache;
    private final HytaleEventBridge eventBridge;
    private final HytalePacketAdapter packetAdapter;
    
    private Object hytaleServer;
    private Object hytalePlayerManager;
    private Object hytaleEntityManager;
    private Object hytaleWorldManager;
    
    private boolean initialized = false;
    
    private HytaleAdapter() {
        this.playerCache = new ConcurrentHashMap<>();
        this.entityCache = new ConcurrentHashMap<>();
        this.eventBridge = new HytaleEventBridge(this);
        this.packetAdapter = new HytalePacketAdapter(this);
    }
    
    public static HytaleAdapter getInstance() {
        if (instance == null) {
            synchronized (HytaleAdapter.class) {
                if (instance == null) {
                    instance = new HytaleAdapter();
                }
            }
        }
        return instance;
    }
    
    public void initialize(Object hytaleServer) {
        if (initialized) {
            logger.warning("HytaleAdapter already initialized");
            return;
        }
        
        this.hytaleServer = hytaleServer;
        
        try {
            this.hytalePlayerManager = extractManager(hytaleServer, "getPlayerManager", "players");
            this.hytaleEntityManager = extractManager(hytaleServer, "getEntityManager", "entities");
            this.hytaleWorldManager = extractManager(hytaleServer, "getWorldManager", "worlds");
            
            eventBridge.registerHytaleEventListeners(hytaleServer);
            packetAdapter.initialize(hytaleServer);
            
            initialized = true;
            logger.info("HytaleAdapter initialized successfully - Rubidium is now connected to Hytale!");
            
        } catch (Exception e) {
            logger.severe("Failed to initialize HytaleAdapter: " + e.getMessage());
            throw new RuntimeException("HytaleAdapter initialization failed", e);
        }
    }
    
    private Object extractManager(Object server, String methodName, String fallbackField) {
        try {
            var method = server.getClass().getMethod(methodName);
            return method.invoke(server);
        } catch (NoSuchMethodException e) {
            try {
                var field = server.getClass().getDeclaredField(fallbackField);
                field.setAccessible(true);
                return field.get(server);
            } catch (Exception ex) {
                logger.warning("Could not extract manager via " + methodName + " or field " + fallbackField);
                return null;
            }
        } catch (Exception e) {
            logger.warning("Failed to invoke " + methodName + ": " + e.getMessage());
            return null;
        }
    }
    
    public Player wrapPlayer(Object hytalePlayer) {
        if (hytalePlayer == null) return null;
        
        UUID uuid = extractPlayerUUID(hytalePlayer);
        if (uuid == null) return null;
        
        return playerCache.computeIfAbsent(uuid, 
            k -> new HytalePlayerWrapper(hytalePlayer, this));
    }
    
    public HytaleEntityWrapper wrapEntity(Object hytaleEntity) {
        if (hytaleEntity == null) return null;
        
        long entityId = extractEntityId(hytaleEntity);
        return entityCache.computeIfAbsent(entityId,
            k -> new HytaleEntityWrapper(hytaleEntity, this));
    }
    
    public void unwrapPlayer(UUID uuid) {
        playerCache.remove(uuid);
    }
    
    public void unwrapEntity(long entityId) {
        entityCache.remove(entityId);
    }
    
    private UUID extractPlayerUUID(Object hytalePlayer) {
        try {
            var method = hytalePlayer.getClass().getMethod("getUUID");
            return (UUID) method.invoke(hytalePlayer);
        } catch (NoSuchMethodException e) {
            try {
                var method = hytalePlayer.getClass().getMethod("getUniqueId");
                return (UUID) method.invoke(hytalePlayer);
            } catch (Exception ex) {
                try {
                    var method = hytalePlayer.getClass().getMethod("getId");
                    Object result = method.invoke(hytalePlayer);
                    if (result instanceof UUID) return (UUID) result;
                    if (result instanceof String) return UUID.fromString((String) result);
                } catch (Exception e2) {
                    logger.warning("Could not extract UUID from Hytale player");
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to extract player UUID: " + e.getMessage());
        }
        return null;
    }
    
    private long extractEntityId(Object hytaleEntity) {
        try {
            var method = hytaleEntity.getClass().getMethod("getEntityId");
            return (long) method.invoke(hytaleEntity);
        } catch (NoSuchMethodException e) {
            try {
                var method = hytaleEntity.getClass().getMethod("getId");
                Object result = method.invoke(hytaleEntity);
                if (result instanceof Long) return (Long) result;
                if (result instanceof Integer) return ((Integer) result).longValue();
            } catch (Exception ex) {
                logger.warning("Could not extract entity ID");
            }
        } catch (Exception e) {
            logger.warning("Failed to extract entity ID: " + e.getMessage());
        }
        return -1;
    }
    
    public Collection<Player> getOnlinePlayers() {
        if (hytalePlayerManager == null) return Collections.emptyList();
        
        try {
            var method = hytalePlayerManager.getClass().getMethod("getOnlinePlayers");
            @SuppressWarnings("unchecked")
            Collection<Object> hytalePlayers = (Collection<Object>) method.invoke(hytalePlayerManager);
            
            List<Player> players = new ArrayList<>();
            for (Object hp : hytalePlayers) {
                Player wrapped = wrapPlayer(hp);
                if (wrapped != null) players.add(wrapped);
            }
            return players;
            
        } catch (Exception e) {
            logger.warning("Failed to get online players: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    public Optional<Player> getPlayer(UUID uuid) {
        HytalePlayerWrapper cached = playerCache.get(uuid);
        if (cached != null && cached.isOnline()) {
            return Optional.of(cached);
        }
        
        if (hytalePlayerManager == null) return Optional.empty();
        
        try {
            var method = hytalePlayerManager.getClass().getMethod("getPlayer", UUID.class);
            Object hytalePlayer = method.invoke(hytalePlayerManager, uuid);
            if (hytalePlayer != null) {
                return Optional.of(wrapPlayer(hytalePlayer));
            }
        } catch (Exception e) {
            logger.warning("Failed to get player by UUID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public Optional<Player> getPlayer(String name) {
        for (HytalePlayerWrapper player : playerCache.values()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return Optional.of(player);
            }
        }
        
        if (hytalePlayerManager == null) return Optional.empty();
        
        try {
            var method = hytalePlayerManager.getClass().getMethod("getPlayer", String.class);
            Object hytalePlayer = method.invoke(hytalePlayerManager, name);
            if (hytalePlayer != null) {
                return Optional.of(wrapPlayer(hytalePlayer));
            }
        } catch (Exception e) {
            logger.warning("Failed to get player by name: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public void sendPacketToPlayer(Player player, Object packet) {
        packetAdapter.sendPacket(player, packet);
    }
    
    public void broadcastPacket(Object packet) {
        packetAdapter.broadcastPacket(packet);
    }
    
    public HytaleEventBridge getEventBridge() {
        return eventBridge;
    }
    
    public HytalePacketAdapter getPacketAdapter() {
        return packetAdapter;
    }
    
    public Object getHytaleServer() {
        return hytaleServer;
    }
    
    public Object getHytalePlayerManager() {
        return hytalePlayerManager;
    }
    
    public Object getHytaleEntityManager() {
        return hytaleEntityManager;
    }
    
    public Object getHytaleWorldManager() {
        return hytaleWorldManager;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void shutdown() {
        eventBridge.unregisterAll();
        playerCache.clear();
        entityCache.clear();
        initialized = false;
        logger.info("HytaleAdapter shut down");
    }
}
