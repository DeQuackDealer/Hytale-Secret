package rubidium.hytale.api.event;

/**
 * Rubidium's block place event.
 */
public class BlockPlaceEvent extends CancellableEvent {
    
    private final int x, y, z;
    private final String world;
    private final String blockType;
    
    public BlockPlaceEvent(int x, int y, int z, String world, String blockType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.blockType = blockType;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getWorld() { return world; }
    public String getBlockType() { return blockType; }
}
