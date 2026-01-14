package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

public class PlayerNamePlaceholder implements PlaceholderProvider {
    
    public static final PlayerNamePlaceholder INSTANCE = new PlayerNamePlaceholder();
    
    @Override
    public String getName() { return "Player Name"; }
    
    @Override
    public String getIdentifier() { return "player_name"; }
    
    @Override
    public String parse(Player player, String input) {
        return player != null ? player.getUsername() : "Unknown";
    }
}
