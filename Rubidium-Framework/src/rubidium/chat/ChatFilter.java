package rubidium.chat;

import rubidium.hytale.api.player.Player;

/**
 * Chat filter interface for blocking/modifying messages.
 */
public interface ChatFilter {
    
    String getName();
    
    Result filter(Player sender, String message);
    
    record Result(String message, boolean blocked, String reason) {
        public static Result allow(String message) {
            return new Result(message, false, null);
        }
        
        public static Result block(String reason) {
            return new Result(null, true, reason);
        }
    }
}
