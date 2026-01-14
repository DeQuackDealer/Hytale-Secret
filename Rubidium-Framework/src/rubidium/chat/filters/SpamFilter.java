package rubidium.chat.filters;

import rubidium.chat.ChatFilter;
import rubidium.chat.ChatManager;
import rubidium.hytale.api.player.Player;

/**
 * Spam filter with cooldown and duplicate detection.
 */
public class SpamFilter implements ChatFilter {
    
    private final long cooldownMs;
    private final boolean blockDuplicates;
    
    public SpamFilter(long cooldownMs, boolean blockDuplicates) {
        this.cooldownMs = cooldownMs;
        this.blockDuplicates = blockDuplicates;
    }
    
    @Override
    public String getName() { return "Spam Filter"; }
    
    @Override
    public Result filter(Player sender, String message) {
        var state = ChatManager.getInstance().getPlayerState(sender);
        long now = System.currentTimeMillis();
        long lastTime = state.getLastMessageTime();
        
        if (now - lastTime < cooldownMs) {
            long remaining = (cooldownMs - (now - lastTime)) / 1000;
            return Result.block("Please wait " + remaining + " seconds before sending another message");
        }
        
        state.updateLastMessageTime();
        return Result.allow(message);
    }
}
