package rubidium.cosmetics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player's owned and equipped cosmetics.
 */
public class PlayerCosmetics {
    
    private final UUID playerId;
    private final Set<String> ownedCosmetics;
    private final Map<String, String> equippedCosmetics;
    
    public PlayerCosmetics(UUID playerId) {
        this.playerId = playerId;
        this.ownedCosmetics = ConcurrentHashMap.newKeySet();
        this.equippedCosmetics = new ConcurrentHashMap<>();
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public void addCosmetic(String cosmeticId) {
        ownedCosmetics.add(cosmeticId);
    }
    
    public void removeCosmetic(String cosmeticId) {
        ownedCosmetics.remove(cosmeticId);
    }
    
    public boolean ownsCosmetic(String cosmeticId) {
        return ownedCosmetics.contains(cosmeticId);
    }
    
    public Set<String> getOwnedCosmetics() {
        return Collections.unmodifiableSet(ownedCosmetics);
    }
    
    public void equipCosmetic(String category, String cosmeticId) {
        equippedCosmetics.put(category, cosmeticId);
    }
    
    public void unequipCosmetic(String category) {
        equippedCosmetics.remove(category);
    }
    
    public String getEquippedCosmetic(String category) {
        return equippedCosmetics.get(category);
    }
    
    public Map<String, String> getEquippedCosmetics() {
        return Collections.unmodifiableMap(equippedCosmetics);
    }
    
    public boolean hasEquipped(String category) {
        return equippedCosmetics.containsKey(category);
    }
}
