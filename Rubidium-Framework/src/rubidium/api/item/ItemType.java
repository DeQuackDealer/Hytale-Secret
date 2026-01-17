package rubidium.api.item;

public class ItemType {
    
    private final String id;
    private final String name;
    private final String category;
    private final int maxStackSize;
    private final String iconPath;
    
    public ItemType(String id, String name) {
        this(id, name, "misc", 64, null);
    }
    
    public ItemType(String id, String name, String category) {
        this(id, name, category, 64, null);
    }
    
    public ItemType(String id, String name, String category, int maxStackSize, String iconPath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.maxStackSize = maxStackSize;
        this.iconPath = iconPath;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getMaxStackSize() { return maxStackSize; }
    public String getIconPath() { return iconPath; }
}
