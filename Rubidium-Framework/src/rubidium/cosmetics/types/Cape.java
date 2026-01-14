package rubidium.cosmetics.types;

import rubidium.cosmetics.Cosmetic;
import rubidium.cosmetics.CosmeticRarity;
import rubidium.hytale.api.player.Player;

/**
 * Cape cosmetic.
 */
public class Cape extends Cosmetic {
    
    private final String texturePath;
    private final boolean animated;
    private final String animationPath;
    
    public Cape(String id, String name, String description, CosmeticRarity rarity,
                String iconPath, boolean purchasable, int price,
                String texturePath, boolean animated, String animationPath) {
        super(id, name, description, "cape", rarity, iconPath, purchasable, price);
        this.texturePath = texturePath;
        this.animated = animated;
        this.animationPath = animationPath;
    }
    
    @Override
    public void onEquip(Player player) {
        player.sendMessage("§aEquipped cape: " + getFormattedName());
    }
    
    @Override
    public void onUnequip(Player player) {
        player.sendMessage("§cUnequipped cape: " + getName());
    }
    
    @Override
    public void tick(Player player) {}
    
    public String getTexturePath() { return texturePath; }
    public boolean isAnimated() { return animated; }
    public String getAnimationPath() { return animationPath; }
}
