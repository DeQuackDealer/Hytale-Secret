package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

public class PlayerPingPlaceholder implements PlaceholderProvider {
    
    @Override
    public String getName() { return "Player Ping"; }
    
    @Override
    public String getIdentifier() { return "player_ping"; }
    
    @Override
    public String parse(Player player, String input) {
        return player != null ? String.valueOf(player.getPing()) : "0";
    }
}
