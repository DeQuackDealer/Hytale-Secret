package rubidium.i18n;

import rubidium.hytale.api.player.Player;
import rubidium.placeholder.PlaceholderService;

/**
 * Translatable message with placeholder support.
 */
public class Message {
    
    private final String key;
    private final String raw;
    private String parsed;
    
    public Message(String key, String message) {
        this.key = key;
        this.raw = message;
        this.parsed = message;
    }
    
    public Message replace(String placeholder, String replacement) {
        this.parsed = this.parsed
            .replace("{" + placeholder + "}", replacement)
            .replace("%" + placeholder + "%", replacement);
        return this;
    }
    
    public Message parse(Player player) {
        this.parsed = PlaceholderService.getInstance().parse(player, this.parsed);
        return this;
    }
    
    public void sendTo(Player player) {
        if (player != null && player.isOnline()) {
            player.sendMessage(this.parsed);
        }
    }
    
    public String getKey() { return key; }
    public String getRawMessage() { return raw; }
    public String getParsedMessage() { return parsed; }
    
    @Override
    public String toString() {
        return parsed;
    }
}
