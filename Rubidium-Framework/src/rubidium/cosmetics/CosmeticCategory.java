package rubidium.cosmetics;

/**
 * Cosmetic category definition.
 */
public class CosmeticCategory {
    
    private final String id;
    private final String displayName;
    private final int order;
    
    public CosmeticCategory(String id, String displayName, int order) {
        this.id = id;
        this.displayName = displayName;
        this.order = order;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getOrder() { return order; }
}
