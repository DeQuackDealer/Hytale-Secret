package rubidium.world;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class WorldManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-WorldManager");
    
    private final Map<String, World> worlds;
    private final Map<UUID, World> worldsByUUID;
    private String defaultWorldName = "world";
    
    public WorldManager() {
        this.worlds = new ConcurrentHashMap<>();
        this.worldsByUUID = new ConcurrentHashMap<>();
    }
    
    public void registerWorld(World world) {
        worlds.put(world.getName(), world);
        worldsByUUID.put(world.getUUID(), world);
        logger.fine("Registered world: " + world.getName());
    }
    
    public void unregisterWorld(String name) {
        World world = worlds.remove(name);
        if (world != null) {
            worldsByUUID.remove(world.getUUID());
            logger.fine("Unregistered world: " + name);
        }
    }
    
    public Optional<World> getWorld(String name) {
        return Optional.ofNullable(worlds.get(name));
    }
    
    public Optional<World> getWorld(UUID uuid) {
        return Optional.ofNullable(worldsByUUID.get(uuid));
    }
    
    public World getDefaultWorld() {
        return worlds.get(defaultWorldName);
    }
    
    public void setDefaultWorld(String name) {
        this.defaultWorldName = name;
    }
    
    public Collection<World> getWorlds() {
        return Collections.unmodifiableCollection(worlds.values());
    }
    
    public Set<String> getWorldNames() {
        return Collections.unmodifiableSet(worlds.keySet());
    }
    
    public boolean worldExists(String name) {
        return worlds.containsKey(name);
    }
    
    public int getWorldCount() {
        return worlds.size();
    }
}
