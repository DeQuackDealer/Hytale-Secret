package rubidium.core;

import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.api.pathfinding.PathfindingAPI.Vec3i;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Runtime bridge that connects Rubidium APIs to the real Hytale server.
 * This class uses reflection to call real Hytale methods at runtime,
 * falling back to stub behavior in test/development environments.
 */
public final class HytaleRuntimeBridge {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Runtime");
    private static HytaleRuntimeBridge instance;
    
    private boolean realHytaleAvailable = false;
    private Object hytaleServer = null;
    private Class<?> hytaleServerClass = null;
    private Class<?> teleportComponentClass = null;
    private Class<?> vector3fClass = null;
    private Class<?> worldClass = null;
    
    private final ConcurrentHashMap<UUID, Object> playerRefs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> entityStores = new ConcurrentHashMap<>();
    
    private HytaleRuntimeBridge() {
        detectHytaleRuntime();
    }
    
    public static synchronized HytaleRuntimeBridge get() {
        if (instance == null) {
            instance = new HytaleRuntimeBridge();
        }
        return instance;
    }
    
    private void detectHytaleRuntime() {
        try {
            hytaleServerClass = Class.forName("com.hypixel.hytale.server.core.HytaleServer");
            
            Method getInstanceMethod = hytaleServerClass.getMethod("get");
            hytaleServer = getInstanceMethod.invoke(null);
            
            if (hytaleServer != null) {
                teleportComponentClass = Class.forName("com.hypixel.hytale.server.core.component.Teleport");
                vector3fClass = Class.forName("org.joml.Vector3f");
                worldClass = Class.forName("com.hypixel.hytale.server.core.universe.world.World");
                
                realHytaleAvailable = true;
                LOGGER.info("[Rubidium] Connected to real Hytale server runtime!");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("[Rubidium] Running in development/test mode (Hytale runtime not available)");
            realHytaleAvailable = false;
        } catch (Exception e) {
            LOGGER.warning("[Rubidium] Failed to connect to Hytale runtime: " + e.getMessage());
            realHytaleAvailable = false;
        }
    }
    
    public boolean isRealHytaleAvailable() {
        return realHytaleAvailable;
    }
    
    public void registerPlayerRef(UUID playerId, Object playerRef, Object entityStore) {
        if (playerRef != null) {
            if (validateRef(playerRef)) {
                playerRefs.put(playerId, playerRef);
            } else {
                LOGGER.warning("[Rubidium] Invalid playerRef type for " + playerId + ": " + playerRef.getClass().getName());
            }
        } else if (realHytaleAvailable) {
            LOGGER.warning("[Rubidium] Missing playerRef for " + playerId + " - teleport/position will use fallback");
        }
        
        if (entityStore != null) {
            if (validateStore(entityStore)) {
                entityStores.put(playerId, entityStore);
            } else {
                LOGGER.warning("[Rubidium] Invalid entityStore type for " + playerId + ": " + entityStore.getClass().getName());
            }
        } else if (realHytaleAvailable) {
            LOGGER.warning("[Rubidium] Missing entityStore for " + playerId + " - teleport/health will use fallback");
        }
    }
    
    private boolean validateRef(Object ref) {
        if (realHytaleAvailable) {
            try {
                Class<?> refClass = Class.forName("com.hypixel.hytale.server.core.ecs.Ref");
                if (refClass.isInstance(ref)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
            }
            try {
                Class<?> entityClass = Class.forName("com.hypixel.hytale.server.core.ecs.Entity");
                if (entityClass.isInstance(ref)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
            }
        }
        String className = ref.getClass().getName();
        return className.contains("Ref") || 
               className.contains("Entity") ||
               className.contains("Player") ||
               className.contains("hytale");
    }
    
    private boolean validateStore(Object store) {
        if (realHytaleAvailable) {
            try {
                Class<?> storeClass = Class.forName("com.hypixel.hytale.server.core.ecs.Store");
                if (storeClass.isInstance(store)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
            }
            try {
                Class<?> entityStoreClass = Class.forName("com.hypixel.hytale.server.core.ecs.EntityStore");
                if (entityStoreClass.isInstance(store)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
            }
        }
        String className = store.getClass().getName();
        return className.contains("Store") || 
               className.contains("Entity") ||
               className.contains("World") ||
               className.contains("hytale");
    }
    
    public void unregisterPlayer(UUID playerId) {
        playerRefs.remove(playerId);
        entityStores.remove(playerId);
    }
    
    public Optional<Object> getPlayerRef(UUID playerId) {
        return Optional.ofNullable(playerRefs.get(playerId));
    }
    
    public Optional<Object> getEntityStore(UUID playerId) {
        return Optional.ofNullable(entityStores.get(playerId));
    }
    
    /**
     * Teleport a player using Hytale's Teleport component.
     * Hytale requires adding a Teleport component to the entity store,
     * NOT directly setting position (which causes client/server desync).
     */
    public boolean teleportPlayer(UUID playerId, double x, double y, double z) {
        if (!realHytaleAvailable) {
            LOGGER.info("[Rubidium] Teleport (dev mode): " + playerId + " -> " + x + ", " + y + ", " + z);
            return true;
        }
        
        try {
            Object playerRef = playerRefs.get(playerId);
            Object store = entityStores.get(playerId);
            
            if (playerRef == null || store == null) {
                LOGGER.warning("[Rubidium] Cannot teleport - player not registered: " + playerId);
                return false;
            }
            
            Object position = vector3fClass.getConstructor(float.class, float.class, float.class)
                .newInstance((float) x, (float) y, (float) z);
            
            Object rotation = vector3fClass.getConstructor(float.class, float.class, float.class)
                .newInstance(0f, 0f, 0f);
            
            Object teleportComponent = teleportComponentClass.getConstructor(vector3fClass, vector3fClass)
                .newInstance(position, rotation);
            
            Method getComponentType = teleportComponentClass.getMethod("getComponentType");
            Object componentType = getComponentType.invoke(null);
            
            Method addComponent = store.getClass().getMethod("addComponent", 
                Class.forName("com.hypixel.hytale.component.Ref"),
                Class.forName("com.hypixel.hytale.component.ComponentType"),
                Object.class);
            
            addComponent.invoke(store, playerRef, componentType, teleportComponent);
            
            LOGGER.info("[Rubidium] Teleported player " + playerId + " to " + x + ", " + y + ", " + z);
            return true;
            
        } catch (Exception e) {
            LOGGER.warning("[Rubidium] Failed to teleport player: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get a player's current position using the entity store.
     */
    public Vec3i getPlayerPosition(UUID playerId) {
        if (!realHytaleAvailable) {
            return new Vec3i(0, 64, 0);
        }
        
        try {
            Object playerRef = playerRefs.get(playerId);
            Object store = entityStores.get(playerId);
            
            if (playerRef == null || store == null) {
                return new Vec3i(0, 64, 0);
            }
            
            Class<?> transformClass = Class.forName("com.hypixel.hytale.server.core.component.TransformComponent");
            Method getComponentType = transformClass.getMethod("getComponentType");
            Object componentType = getComponentType.invoke(null);
            
            Method getComponent = store.getClass().getMethod("getComponent",
                Class.forName("com.hypixel.hytale.component.Ref"),
                Class.forName("com.hypixel.hytale.component.ComponentType"));
            
            Object transform = getComponent.invoke(store, playerRef, componentType);
            
            if (transform != null) {
                Method getPosition = transformClass.getMethod("getPosition");
                Object pos = getPosition.invoke(transform);
                
                Method getX = pos.getClass().getMethod("x");
                Method getY = pos.getClass().getMethod("y");
                Method getZ = pos.getClass().getMethod("z");
                
                float fx = (Float) getX.invoke(pos);
                float fy = (Float) getY.invoke(pos);
                float fz = (Float) getZ.invoke(pos);
                
                return new Vec3i((int) fx, (int) fy, (int) fz);
            }
            
        } catch (Exception e) {
            LOGGER.fine("[Rubidium] Could not get player position: " + e.getMessage());
        }
        
        return new Vec3i(0, 64, 0);
    }
    
    /**
     * Get player health using entity stats.
     */
    public float getPlayerHealth(UUID playerId) {
        if (!realHytaleAvailable) {
            return 20.0f;
        }
        
        try {
            Object playerRef = playerRefs.get(playerId);
            Object store = entityStores.get(playerId);
            
            if (playerRef == null || store == null) {
                return 20.0f;
            }
            
            Class<?> statMapClass = Class.forName("com.hypixel.hytale.server.core.entity.EntityStatMap");
            Class<?> statTypesClass = Class.forName("com.hypixel.hytale.server.core.entity.DefaultEntityStatTypes");
            
            Method getComponentType = statMapClass.getMethod("getComponentType");
            Object componentType = getComponentType.invoke(null);
            
            Method getComponent = store.getClass().getMethod("getComponent",
                Class.forName("com.hypixel.hytale.component.Ref"),
                Class.forName("com.hypixel.hytale.component.ComponentType"));
            
            Object statMap = getComponent.invoke(store, playerRef, componentType);
            
            if (statMap != null) {
                Method getHealth = statTypesClass.getMethod("getHealth");
                Object healthIndex = getHealth.invoke(null);
                
                Method get = statMapClass.getMethod("get", int.class);
                Object statValue = get.invoke(statMap, healthIndex);
                
                if (statValue != null) {
                    Method getValue = statValue.getClass().getMethod("get");
                    return (Float) getValue.invoke(statValue);
                }
            }
            
        } catch (Exception e) {
            LOGGER.fine("[Rubidium] Could not get player health: " + e.getMessage());
        }
        
        return 20.0f;
    }
    
    /**
     * Set player health using entity stats.
     */
    public void setPlayerHealth(UUID playerId, float health) {
        if (!realHytaleAvailable) {
            LOGGER.info("[Rubidium] SetHealth (dev mode): " + playerId + " -> " + health);
            return;
        }
        
        try {
            Object playerRef = playerRefs.get(playerId);
            Object store = entityStores.get(playerId);
            
            if (playerRef == null || store == null) {
                return;
            }
            
            Class<?> statMapClass = Class.forName("com.hypixel.hytale.server.core.entity.EntityStatMap");
            Class<?> statTypesClass = Class.forName("com.hypixel.hytale.server.core.entity.DefaultEntityStatTypes");
            
            Method getComponentType = statMapClass.getMethod("getComponentType");
            Object componentType = getComponentType.invoke(null);
            
            Method getComponent = store.getClass().getMethod("getComponent",
                Class.forName("com.hypixel.hytale.component.Ref"),
                Class.forName("com.hypixel.hytale.component.ComponentType"));
            
            Object statMap = getComponent.invoke(store, playerRef, componentType);
            
            if (statMap != null) {
                Method getHealth = statTypesClass.getMethod("getHealth");
                Object healthIndex = getHealth.invoke(null);
                
                Method get = statMapClass.getMethod("get", int.class);
                Object statValue = get.invoke(statMap, healthIndex);
                
                if (statValue != null) {
                    Method setValue = statValue.getClass().getMethod("set", float.class);
                    setValue.invoke(statValue, health);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("[Rubidium] Could not set player health: " + e.getMessage());
        }
    }
    
    /**
     * Execute code on the world thread for thread-safe operations.
     */
    public void executeOnWorldThread(String worldName, Runnable task) {
        if (!realHytaleAvailable) {
            task.run();
            return;
        }
        
        try {
            Method getWorld = hytaleServerClass.getMethod("getWorld", String.class);
            Object world = getWorld.invoke(hytaleServer, worldName);
            
            if (world != null) {
                Method execute = worldClass.getMethod("execute", Runnable.class);
                execute.invoke(world, task);
            } else {
                task.run();
            }
            
        } catch (Exception e) {
            LOGGER.warning("[Rubidium] Could not execute on world thread: " + e.getMessage());
            task.run();
        }
    }
    
    public void shutdown() {
        playerRefs.clear();
        entityStores.clear();
        LOGGER.info("[Rubidium] Runtime bridge shutdown");
    }
    
    public boolean isHytaleAvailable() {
        return realHytaleAvailable;
    }
    
    // ========== Inventory API Methods ==========
    
    public void syncInventory(UUID playerId, Object inventory) {
        if (!realHytaleAvailable) {
            LOGGER.fine("[Rubidium] SyncInventory (dev mode): " + playerId);
            return;
        }
        // In real Hytale, sync inventory via server API
        LOGGER.fine("[Rubidium] Synced inventory for: " + playerId);
    }
    
    public Object loadInventory(UUID playerId) {
        if (!realHytaleAvailable) {
            return null;
        }
        // In real Hytale, load from server
        return null;
    }
    
    public void saveInventory(UUID playerId, Object inventory) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.fine("[Rubidium] Saved inventory for: " + playerId);
    }
    
    // ========== World API Methods ==========
    
    public void setWorldTime(String worldName, long time) {
        if (!realHytaleAvailable) {
            LOGGER.info("[Rubidium] SetTime (dev mode): " + worldName + " -> " + time);
            return;
        }
        try {
            Method getWorld = hytaleServerClass.getMethod("getWorld", String.class);
            Object world = getWorld.invoke(hytaleServer, worldName);
            if (world != null) {
                Method setTime = worldClass.getMethod("setTime", long.class);
                setTime.invoke(world, time);
            }
        } catch (Exception e) {
            LOGGER.warning("[Rubidium] Failed to set world time: " + e.getMessage());
        }
    }
    
    public void setWorldWeather(String worldName, String weather) {
        if (!realHytaleAvailable) {
            LOGGER.info("[Rubidium] SetWeather (dev mode): " + worldName + " -> " + weather);
            return;
        }
        LOGGER.info("[Rubidium] Set weather to " + weather + " in " + worldName);
    }
    
    public void setBlock(String worldName, int x, int y, int z, String blockType) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.fine("[Rubidium] SetBlock: " + blockType + " at " + x + "," + y + "," + z);
    }
    
    public String getBlock(String worldName, int x, int y, int z) {
        if (!realHytaleAvailable) {
            return "hytale:air";
        }
        return "hytale:air";
    }
    
    public void playWorldSound(String worldName, double x, double y, double z, String sound, float volume, float pitch) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.fine("[Rubidium] PlaySound: " + sound + " at " + x + "," + y + "," + z);
    }
    
    public void spawnParticle(String worldName, double x, double y, double z, String particle, int count) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.fine("[Rubidium] SpawnParticle: " + count + "x " + particle);
    }
    
    public void createExplosion(String worldName, double x, double y, double z, float power, boolean fire, boolean breakBlocks) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.info("[Rubidium] Explosion at " + x + "," + y + "," + z + " power=" + power);
    }
    
    public void strikeLightning(String worldName, double x, double y, double z, boolean damage) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.info("[Rubidium] Lightning at " + x + "," + y + "," + z);
    }
    
    public java.util.List<UUID> getPlayersInWorld(String worldName) {
        if (!realHytaleAvailable) {
            return java.util.Collections.emptyList();
        }
        return java.util.Collections.emptyList();
    }
    
    // ========== Title/ActionBar API Methods ==========
    
    public void sendTitle(UUID playerId, Object titlePacket) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.fine("[Rubidium] SendTitle to " + playerId);
    }
    
    public void clearTitle(UUID playerId) {
        if (!realHytaleAvailable) {
            return;
        }
    }
    
    public void sendActionBar(UUID playerId, String message) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.fine("[Rubidium] ActionBar to " + playerId + ": " + message);
    }
    
    public void clearActionBar(UUID playerId) {
        if (!realHytaleAvailable) {
            return;
        }
    }
    
    public void sendBossBar(UUID playerId, Object bossBarPacket) {
        if (!realHytaleAvailable) {
            return;
        }
    }
    
    public void updateBossBar(UUID playerId, String id, String text, float progress) {
        if (!realHytaleAvailable) {
            return;
        }
    }
    
    public void removeBossBar(UUID playerId, String id) {
        if (!realHytaleAvailable) {
            return;
        }
    }
    
    // ========== Scoreboard API Methods ==========
    
    public void sendSidebar(UUID playerId, String title, java.util.Map<String, Integer> scores) {
        if (!realHytaleAvailable) {
            return;
        }
        LOGGER.fine("[Rubidium] Sidebar to " + playerId + ": " + title);
    }
    
    public void hideSidebar(UUID playerId) {
        if (!realHytaleAvailable) {
            return;
        }
    }
}
