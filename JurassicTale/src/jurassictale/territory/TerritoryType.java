package jurassictale.territory;

public enum TerritoryType {
    GRASSLANDS("Grasslands / Valley", "Starter zone with herbivores and small predators", 1, 0x90EE90),
    JUNGLE("Dense Jungle", "Mid zone with pack predators and rare dinos", 2, 0x228B22),
    SWAMP("Swamp / Riverlands", "Aquatic ambush dinos and water danger", 3, 0x556B2F),
    HIGHLANDS("Highlands / Cliffs", "Flyers and territorial predators", 3, 0x8B4513),
    CAVES("Caves / Underground", "Underground threats and mining danger", 3, 0x2F4F4F),
    STORM_ZONE("Storm-Scarred Zone", "High risk endgame with apex predators", 4, 0x8B0000);
    
    private final String displayName;
    private final String description;
    private final int dangerLevel;
    private final int mapColor;
    
    TerritoryType(String displayName, String description, int dangerLevel, int mapColor) {
        this.displayName = displayName;
        this.description = description;
        this.dangerLevel = dangerLevel;
        this.mapColor = mapColor;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getDangerLevel() { return dangerLevel; }
    public int getMapColor() { return mapColor; }
}
