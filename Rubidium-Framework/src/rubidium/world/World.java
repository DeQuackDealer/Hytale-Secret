package rubidium.world;

import rubidium.api.player.Player;

import java.util.*;

public interface World {
    
    String getName();
    
    UUID getUUID();
    
    long getSeed();
    
    WorldType getWorldType();
    
    int getMaxHeight();
    
    int getMinHeight();
    
    Block getBlockAt(int x, int y, int z);
    
    Block getBlockAt(BlockPos pos);
    
    void setBlock(int x, int y, int z, BlockData blockData);
    
    void setBlock(BlockPos pos, BlockData blockData);
    
    Collection<Player> getPlayers();
    
    Collection<Entity> getEntities();
    
    <T extends Entity> Collection<T> getEntitiesByType(Class<T> type);
    
    Collection<Entity> getNearbyEntities(double x, double y, double z, double radius);
    
    Entity spawnEntity(double x, double y, double z, String entityType);
    
    Entity spawnEntity(BlockPos pos, String entityType);
    
    Chunk getChunkAt(int x, int z);
    
    Chunk getChunkAt(BlockPos pos);
    
    boolean isChunkLoaded(int x, int z);
    
    void loadChunk(int x, int z);
    
    void unloadChunk(int x, int z);
    
    long getTime();
    
    void setTime(long time);
    
    Weather getWeather();
    
    void setWeather(Weather weather);
    
    void playSound(double x, double y, double z, String sound, float volume, float pitch);
    
    void spawnParticle(String particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ);
    
    void strikeLightning(double x, double y, double z, boolean damage);
    
    void createExplosion(double x, double y, double z, float power, boolean fire, boolean breakBlocks);
    
    boolean isDay();
    
    boolean isNight();
    
    int getHighestBlockYAt(int x, int z);
    
    enum WorldType {
        NORMAL, FLAT, VOID, CUSTOM
    }
    
    enum Weather {
        CLEAR, RAIN, THUNDER
    }
    
    interface Entity {
        long getEntityId();
        String getType();
        double getX();
        double getY();
        double getZ();
        World getWorld();
        void remove();
        boolean isValid();
    }
    
    interface Chunk {
        int getX();
        int getZ();
        World getWorld();
        Block getBlock(int x, int y, int z);
        boolean isLoaded();
        void load();
        void unload();
    }
    
    interface Block {
        BlockPos getPosition();
        BlockData getBlockData();
        String getType();
        World getWorld();
        void setType(String type);
        void setBlockData(BlockData data);
        boolean isEmpty();
        boolean isSolid();
        boolean isLiquid();
    }
    
    record BlockPos(int x, int y, int z) {
        public BlockPos add(int dx, int dy, int dz) {
            return new BlockPos(x + dx, y + dy, z + dz);
        }
        
        public BlockPos up() { return add(0, 1, 0); }
        public BlockPos down() { return add(0, -1, 0); }
        public BlockPos north() { return add(0, 0, -1); }
        public BlockPos south() { return add(0, 0, 1); }
        public BlockPos east() { return add(1, 0, 0); }
        public BlockPos west() { return add(-1, 0, 0); }
        
        public double distanceTo(BlockPos other) {
            int dx = x - other.x;
            int dy = y - other.y;
            int dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        
        public int chunkX() { return x >> 4; }
        public int chunkZ() { return z >> 4; }
    }
    
    interface BlockData {
        String getType();
        Map<String, String> getProperties();
        String getProperty(String key);
        BlockData withProperty(String key, String value);
        BlockData clone();
    }
}
