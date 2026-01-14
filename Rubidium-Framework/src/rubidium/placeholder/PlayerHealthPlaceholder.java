package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

public class PlayerHealthPlaceholder implements PlaceholderProvider {
    
    @Override
    public String getName() { return "Player Health"; }
    
    @Override
    public String getIdentifier() { return "player_health"; }
    
    @Override
    public String parse(Player player, String input) {
        return player != null ? String.format("%.1f", player.getHealth()) : "0";
    }
}
