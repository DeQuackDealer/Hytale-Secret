package rubidium.cosmetics;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cosmetics system for skins, capes, particle trails, and overlays.
 */
public class CosmeticsManager {
    
    private static CosmeticsManager instance;
    
    private final Map<String, Cosmetic> cosmetics;
    private final Map<UUID, PlayerCosmetics> playerCosmetics;
    private final Map<String, CosmeticCategory> categories;
    
    private CosmeticsManager() {
        this.cosmetics = new ConcurrentHashMap<>();
        this.playerCosmetics = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        
        registerDefaultCategories();
    }
    
    public static CosmeticsManager getInstance() {
        if (instance == null) {
            instance = new CosmeticsManager();
        }
        return instance;
    }
    
    private void registerDefaultCategories() {
        registerCategory(new CosmeticCategory("skin", "Skins", 0));
        registerCategory(new CosmeticCategory("cape", "Capes", 1));
        registerCategory(new CosmeticCategory("particle", "Particle Trails", 2));
        registerCategory(new CosmeticCategory("aura", "Auras", 3));
        registerCategory(new CosmeticCategory("hat", "Hats", 4));
        registerCategory(new CosmeticCategory("wings", "Wings", 5));
        registerCategory(new CosmeticCategory("pet", "Pets", 6));
        registerCategory(new CosmeticCategory("emote", "Emotes", 7));
    }
    
    public void registerCategory(CosmeticCategory category) {
        categories.put(category.getId(), category);
    }
    
    public void registerCosmetic(Cosmetic cosmetic) {
        cosmetics.put(cosmetic.getId(), cosmetic);
    }
    
    public Optional<Cosmetic> getCosmetic(String id) {
        return Optional.ofNullable(cosmetics.get(id));
    }
    
    public List<Cosmetic> getCosmeticsByCategory(String category) {
        return cosmetics.values().stream()
            .filter(c -> c.getCategory().equals(category))
            .toList();
    }
    
    public PlayerCosmetics getPlayerCosmetics(Player player) {
        return playerCosmetics.computeIfAbsent(player.getUuid(), 
            k -> new PlayerCosmetics(player.getUuid()));
    }
    
    public void equipCosmetic(Player player, String cosmeticId) {
        Cosmetic cosmetic = cosmetics.get(cosmeticId);
        if (cosmetic == null) return;
        
        PlayerCosmetics pCosmetics = getPlayerCosmetics(player);
        if (!pCosmetics.ownsCosmetic(cosmeticId)) return;
        
        pCosmetics.equipCosmetic(cosmetic.getCategory(), cosmeticId);
        cosmetic.onEquip(player);
    }
    
    public void unequipCosmetic(Player player, String category) {
        PlayerCosmetics pCosmetics = getPlayerCosmetics(player);
        String currentId = pCosmetics.getEquippedCosmetic(category);
        
        if (currentId != null) {
            Cosmetic cosmetic = cosmetics.get(currentId);
            if (cosmetic != null) {
                cosmetic.onUnequip(player);
            }
        }
        
        pCosmetics.unequipCosmetic(category);
    }
    
    public void grantCosmetic(Player player, String cosmeticId) {
        PlayerCosmetics pCosmetics = getPlayerCosmetics(player);
        pCosmetics.addCosmetic(cosmeticId);
    }
    
    public void revokeCosmetic(Player player, String cosmeticId) {
        PlayerCosmetics pCosmetics = getPlayerCosmetics(player);
        pCosmetics.removeCosmetic(cosmeticId);
        
        Cosmetic cosmetic = cosmetics.get(cosmeticId);
        if (cosmetic != null && cosmeticId.equals(pCosmetics.getEquippedCosmetic(cosmetic.getCategory()))) {
            unequipCosmetic(player, cosmetic.getCategory());
        }
    }
    
    public boolean playerOwnsCosmetic(Player player, String cosmeticId) {
        return getPlayerCosmetics(player).ownsCosmetic(cosmeticId);
    }
    
    public Collection<CosmeticCategory> getCategories() {
        return categories.values();
    }
    
    public Collection<Cosmetic> getAllCosmetics() {
        return cosmetics.values();
    }
}
