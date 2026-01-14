package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

/**
 * Interface for placeholder expansions with prefixes.
 * Example: %economy_balance% -> expansion prefix "economy"
 */
public interface PlaceholderExpansion {
    
    String getName();
    
    String getPrefix();
    
    String getAuthor();
    
    String getVersion();
    
    String parse(Player player, String params);
}
