package rubidium.cosmetics.types;

import rubidium.cosmetics.Cosmetic;
import rubidium.cosmetics.CosmeticRarity;
import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.world.Location;

/**
 * Particle trail cosmetic that follows the player.
 */
public class ParticleTrail extends Cosmetic {
    
    private final String particleType;
    private final int particleCount;
    private final float spread;
    private final int tickInterval;
    
    public ParticleTrail(String id, String name, String description, CosmeticRarity rarity,
                         String iconPath, boolean purchasable, int price,
                         String particleType, int particleCount, float spread, int tickInterval) {
        super(id, name, description, "particle", rarity, iconPath, purchasable, price);
        this.particleType = particleType;
        this.particleCount = particleCount;
        this.spread = spread;
        this.tickInterval = tickInterval;
    }
    
    @Override
    public void onEquip(Player player) {
        player.sendMessage("§aEquipped particle trail: " + getFormattedName());
    }
    
    @Override
    public void onUnequip(Player player) {
        player.sendMessage("§cUnequipped particle trail: " + getName());
    }
    
    @Override
    public void tick(Player player) {
        if (!player.isOnline()) return;
        
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(loc, particleType, particleCount);
    }
    
    public String getParticleType() { return particleType; }
    public int getParticleCount() { return particleCount; }
    public float getSpread() { return spread; }
    public int getTickInterval() { return tickInterval; }
}
