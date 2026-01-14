package rubidium.cosmetics;

/**
 * Cosmetic rarity levels.
 */
public enum CosmeticRarity {
    COMMON("§f", "Common", 1.0),
    UNCOMMON("§a", "Uncommon", 0.5),
    RARE("§9", "Rare", 0.25),
    EPIC("§5", "Epic", 0.1),
    LEGENDARY("§6", "Legendary", 0.05),
    MYTHIC("§d", "Mythic", 0.01),
    EXCLUSIVE("§c", "Exclusive", 0.0);
    
    private final String color;
    private final String displayName;
    private final double dropChance;
    
    CosmeticRarity(String color, String displayName, double dropChance) {
        this.color = color;
        this.displayName = displayName;
        this.dropChance = dropChance;
    }
    
    public String getColor() { return color; }
    public String getDisplayName() { return displayName; }
    public double getDropChance() { return dropChance; }
    
    public String getFormatted() {
        return color + displayName;
    }
}
