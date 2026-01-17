package rubidium.api.world;

public interface Chunk {
    
    int getX();
    
    int getZ();
    
    World getWorld();
    
    boolean isLoaded();
    
    void load();
    
    void unload();
}
