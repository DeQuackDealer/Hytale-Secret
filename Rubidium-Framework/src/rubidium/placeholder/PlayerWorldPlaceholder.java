package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

public class PlayerWorldPlaceholder implements PlaceholderProvider {
    
    @Override
    public String getName() { return "Player World"; }
    
    @Override
    public String getIdentifier() { return "player_world"; }
    
    @Override
    public String parse(Player player, String input) {
        return player != null && player.getWorld() != null 
            ? player.getWorld().getName() : "Unknown";
    }
}
