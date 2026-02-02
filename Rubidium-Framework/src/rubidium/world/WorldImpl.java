package rubidium.world;

import rubidium.api.player.Player;
import rubidium.api.world.World;
import rubidium.api.world.Chunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorldImpl implements World {
    
    private static final Map<String, WorldImpl> worlds = new ConcurrentHashMap<>();
    
    private final String id;
    private final String name;
    private final long seed;
    private volatile boolean loaded = true;
    private final Map<Long, ChunkImpl> chunks = new ConcurrentHashMap<>();
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    
    public WorldImpl(String id, String name, long seed) {
        this.id = id;
        this.name = name;
        this.seed = seed;
    }
    
    public static WorldImpl getOrCreate(String name) {
        return worlds.computeIfAbsent(name, n -> new WorldImpl(n, n, new Random().nextLong()));
    }
    
    public static Optional<WorldImpl> get(String name) {
        return Optional.ofNullable(worlds.get(name));
    }
    
    public static Collection<WorldImpl> getAll() {
        return Collections.unmodifiableCollection(worlds.values());
    }
    
    public static void register(WorldImpl world) {
        worlds.put(world.getId(), world);
    }
    
    public static void unregister(String id) {
        worlds.remove(id);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public long getSeed() {
        return seed;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
    
    public Chunk getChunkAt(int x, int z) {
        long key = chunkKey(x, z);
        return chunks.computeIfAbsent(key, k -> new ChunkImpl(this, x, z));
    }
    
    public Chunk getChunkIfLoaded(int x, int z) {
        return chunks.get(chunkKey(x, z));
    }
    
    public Collection<Chunk> getLoadedChunks() {
        return new ArrayList<>(chunks.values());
    }
    
    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }
    
    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }
    
    public Set<UUID> getPlayerIds() {
        return Collections.unmodifiableSet(players);
    }
    
    public int getPlayerCount() {
        return players.size();
    }
    
    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
    
    public static class ChunkImpl implements Chunk {
        private final WorldImpl world;
        private final int x;
        private final int z;
        private volatile boolean loaded = true;
        
        public ChunkImpl(WorldImpl world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }
        
        @Override
        public int getX() {
            return x;
        }
        
        @Override
        public int getZ() {
            return z;
        }
        
        @Override
        public World getWorld() {
            return world;
        }
        
        @Override
        public boolean isLoaded() {
            return loaded;
        }
        
        @Override
        public void load() {
            this.loaded = true;
        }
        
        @Override
        public void unload() {
            this.loaded = false;
        }
    }
}
