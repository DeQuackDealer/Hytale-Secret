package rubidium.api.world;

import rubidium.core.HytaleRuntimeBridge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class WorldAPI {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-World");
    private static WorldAPI instance;
    
    private final Map<String, WorldState> worlds = new ConcurrentHashMap<>();
    
    private WorldAPI() {
        worlds.put("world", new WorldState("world"));
        worlds.put("world_nether", new WorldState("world_nether"));
        worlds.put("world_the_end", new WorldState("world_the_end"));
    }
    
    public static WorldAPI get() {
        if (instance == null) {
            instance = new WorldAPI();
        }
        return instance;
    }
    
    public WorldState getWorld(String name) {
        return worlds.computeIfAbsent(name, WorldState::new);
    }
    
    public Collection<String> getWorldNames() {
        return Collections.unmodifiableSet(worlds.keySet());
    }
    
    public void setTime(String worldName, long time) {
        WorldState world = getWorld(worldName);
        world.setTime(time);
        
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().setWorldTime(worldName, time);
        }
        LOGGER.info("[Rubidium] Set time to " + time + " in " + worldName);
    }
    
    public long getTime(String worldName) {
        return getWorld(worldName).getTime();
    }
    
    public void setWeather(String worldName, Weather weather) {
        WorldState world = getWorld(worldName);
        world.setWeather(weather);
        
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().setWorldWeather(worldName, weather.name());
        }
        LOGGER.info("[Rubidium] Set weather to " + weather + " in " + worldName);
    }
    
    public Weather getWeather(String worldName) {
        return getWorld(worldName).getWeather();
    }
    
    public void setBlock(String worldName, int x, int y, int z, String blockType) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().setBlock(worldName, x, y, z, blockType);
        }
        LOGGER.fine("[Rubidium] Set block at " + x + "," + y + "," + z + " to " + blockType);
    }
    
    public String getBlock(String worldName, int x, int y, int z) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            return HytaleRuntimeBridge.get().getBlock(worldName, x, y, z);
        }
        return "hytale:air";
    }
    
    public void playSound(String worldName, double x, double y, double z, String sound, float volume, float pitch) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().playWorldSound(worldName, x, y, z, sound, volume, pitch);
        }
        LOGGER.fine("[Rubidium] Playing sound " + sound + " at " + x + "," + y + "," + z);
    }
    
    public void spawnParticle(String worldName, double x, double y, double z, String particle, int count) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().spawnParticle(worldName, x, y, z, particle, count);
        }
        LOGGER.fine("[Rubidium] Spawning " + count + "x " + particle + " at " + x + "," + y + "," + z);
    }
    
    public void createExplosion(String worldName, double x, double y, double z, float power, boolean fire, boolean breakBlocks) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().createExplosion(worldName, x, y, z, power, fire, breakBlocks);
        }
        LOGGER.info("[Rubidium] Created explosion at " + x + "," + y + "," + z + " power=" + power);
    }
    
    public void strikeLightning(String worldName, double x, double y, double z, boolean damage) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            HytaleRuntimeBridge.get().strikeLightning(worldName, x, y, z, damage);
        }
        LOGGER.info("[Rubidium] Lightning strike at " + x + "," + y + "," + z);
    }
    
    public List<UUID> getPlayersInWorld(String worldName) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            return HytaleRuntimeBridge.get().getPlayersInWorld(worldName);
        }
        return Collections.emptyList();
    }
    
    public int getPlayerCount(String worldName) {
        return getPlayersInWorld(worldName).size();
    }
    
    public enum Weather {
        CLEAR,
        RAIN,
        THUNDER,
        SNOW
    }
    
    public static class WorldState {
        private final String name;
        private long time;
        private Weather weather;
        private boolean pvpEnabled;
        private int spawnX, spawnY, spawnZ;
        
        public WorldState(String name) {
            this.name = name;
            this.time = 6000;
            this.weather = Weather.CLEAR;
            this.pvpEnabled = true;
            this.spawnX = 0;
            this.spawnY = 64;
            this.spawnZ = 0;
        }
        
        public String getName() { return name; }
        
        public long getTime() { return time; }
        public void setTime(long time) { this.time = time % 24000; }
        
        public Weather getWeather() { return weather; }
        public void setWeather(Weather weather) { this.weather = weather; }
        
        public boolean isPvpEnabled() { return pvpEnabled; }
        public void setPvpEnabled(boolean enabled) { this.pvpEnabled = enabled; }
        
        public int getSpawnX() { return spawnX; }
        public int getSpawnY() { return spawnY; }
        public int getSpawnZ() { return spawnZ; }
        
        public void setSpawn(int x, int y, int z) {
            this.spawnX = x;
            this.spawnY = y;
            this.spawnZ = z;
        }
        
        public boolean isDay() {
            return time >= 0 && time < 12000;
        }
        
        public boolean isNight() {
            return time >= 12000 && time < 24000;
        }
    }
}
