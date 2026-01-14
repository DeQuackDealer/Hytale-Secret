package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

public class PlayerUuidPlaceholder implements PlaceholderProvider {
    
    @Override
    public String getName() { return "Player UUID"; }
    
    @Override
    public String getIdentifier() { return "player_uuid"; }
    
    @Override
    public String parse(Player player, String input) {
        return player != null ? player.getUuid().toString() : "N/A";
    }
}
