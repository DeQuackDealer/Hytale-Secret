package rubidium.waypoints;

public enum WaypointIcon {
    HOUSE,
    CASTLE,
    TOWER,
    TENT,
    VILLAGE,
    
    TREE,
    MOUNTAIN,
    CAVE,
    WATER,
    PORTAL,
    
    SKULL,
    SWORD,
    SHIELD,
    CROSSHAIR,
    
    PICKAXE,
    AXE,
    CHEST,
    DIAMOND,
    GOLD,
    
    MARKER,
    FLAG,
    STAR,
    ARROW,
    COMPASS,
    
    HEART,
    PLAYER,
    GROUP,
    SHOP,
    QUEST,
    
    WARNING,
    INFO,
    QUESTION,
    CHECK,
    CROSS,
    
    CUSTOM_0,
    CUSTOM_1,
    CUSTOM_2,
    CUSTOM_3,
    CUSTOM_4;
    
    public String getTexturePath() {
        return "textures/waypoints/" + name().toLowerCase() + ".png";
    }
    
    public boolean isCustom() {
        return name().startsWith("CUSTOM_");
    }
}
