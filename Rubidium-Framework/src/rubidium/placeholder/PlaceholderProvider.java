package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

/**
 * Interface for simple placeholders.
 */
public interface PlaceholderProvider {
    
    String getName();
    
    String getIdentifier();
    
    String parse(Player player, String input);
}
