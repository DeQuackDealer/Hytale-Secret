package rubidium.chat;

/**
 * Chat channel definition.
 */
public class ChatChannel {
    
    private final String id;
    private final String displayName;
    private final String prefix;
    private final int range;
    private final boolean isDefault;
    private String format;
    private String permission;
    
    public ChatChannel(String id, String displayName, String prefix, int range, boolean isDefault) {
        this.id = id;
        this.displayName = displayName;
        this.prefix = prefix;
        this.range = range;
        this.isDefault = isDefault;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getPrefix() { return prefix; }
    public int getRange() { return range; }
    public boolean isDefault() { return isDefault; }
    public String getFormat() { return format; }
    public String getPermission() { return permission; }
    
    public ChatChannel setFormat(String format) {
        this.format = format;
        return this;
    }
    
    public ChatChannel setPermission(String permission) {
        this.permission = permission;
        return this;
    }
}
