package com.yellowtale.rubidium.waypoints;

public class WaypointCategory {
    private final String id;
    private String name;
    private int color;
    private WaypointIcon defaultIcon;
    private final boolean system;
    
    public static final WaypointCategory HOME = new WaypointCategory("home", "Home", 0x4CAF50, WaypointIcon.HOUSE, true);
    public static final WaypointCategory DEATH = new WaypointCategory("death", "Death", 0xF44336, WaypointIcon.SKULL, true);
    public static final WaypointCategory SPAWN = new WaypointCategory("spawn", "Spawn", 0x2196F3, WaypointIcon.STAR, true);
    public static final WaypointCategory POI = new WaypointCategory("poi", "Point of Interest", 0xFFEB3B, WaypointIcon.FLAG, true);
    public static final WaypointCategory DUNGEON = new WaypointCategory("dungeon", "Dungeon", 0x9C27B0, WaypointIcon.CAVE, true);
    public static final WaypointCategory RESOURCE = new WaypointCategory("resource", "Resource", 0xFF9800, WaypointIcon.PICKAXE, true);
    public static final WaypointCategory QUEST = new WaypointCategory("quest", "Quest", 0xE91E63, WaypointIcon.QUEST, true);
    public static final WaypointCategory SHOP = new WaypointCategory("shop", "Shop", 0x00BCD4, WaypointIcon.SHOP, true);
    public static final WaypointCategory CUSTOM = new WaypointCategory("custom", "Custom", 0xFFFFFF, WaypointIcon.MARKER, false);
    
    public WaypointCategory(String id, String name, int color, WaypointIcon defaultIcon, boolean system) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.defaultIcon = defaultIcon;
        this.system = system;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { 
        if (!system) this.name = name; 
    }
    public int getColor() { return color; }
    public void setColor(int color) { 
        if (!system) this.color = color; 
    }
    public WaypointIcon getDefaultIcon() { return defaultIcon; }
    public void setDefaultIcon(WaypointIcon defaultIcon) { 
        if (!system) this.defaultIcon = defaultIcon; 
    }
    public boolean isSystem() { return system; }
    
    public String getColorHex() {
        return String.format("#%06X", color);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaypointCategory that = (WaypointCategory) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
