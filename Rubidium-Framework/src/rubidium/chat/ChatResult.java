package rubidium.chat;

/**
 * Result of chat message processing.
 */
public record ChatResult(
    boolean success,
    String formattedMessage,
    ChatChannel channel,
    String blockedReason
) {
    public static ChatResult success(String formattedMessage, ChatChannel channel) {
        return new ChatResult(true, formattedMessage, channel, null);
    }
    
    public static ChatResult blocked(String reason) {
        return new ChatResult(false, null, null, reason);
    }
}
