package rubidium.cosmetics;

import rubidium.hytale.api.player.Player;

/**
 * Base cosmetic definition.
 */
public abstract class Cosmetic {
    
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final CosmeticRarity rarity;
    private final String iconPath;
    private final boolean purchasable;
    private final int price;
    
    public Cosmetic(String id, String name, String description, String category,
                    CosmeticRarity rarity, String iconPath, boolean purchasable, int price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.iconPath = iconPath;
        this.purchasable = purchasable;
        this.price = price;
    }
    
    public abstract void onEquip(Player player);
    public abstract void onUnequip(Player player);
    public abstract void tick(Player player);
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public CosmeticRarity getRarity() { return rarity; }
    public String getIconPath() { return iconPath; }
    public boolean isPurchasable() { return purchasable; }
    public int getPrice() { return price; }
    
    public String getFormattedName() {
        return rarity.getColor() + name;
    }
}
