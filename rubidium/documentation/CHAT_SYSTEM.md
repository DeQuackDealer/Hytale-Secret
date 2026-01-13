# Chat System

> **Document Purpose**: Complete reference for Rubidium's chat channels, filtering, and messaging system.

## Overview

Rubidium's chat system provides:
- **Channels**: Multiple chat channels with configurable access
- **Private Messaging**: Direct player-to-player communication
- **Formatting**: Color codes, formatting, and placeholders
- **Filters**: Profanity filter, spam protection, link blocking
- **Moderation**: Mute, channel moderation, logging

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       ChatManager                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Core Components                                           │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Channel  │ │ Filter   │ │ Format   │ │ History  │     │   │
│  │ │ Manager  │ │ Chain    │ │ Processor│ │ Logger   │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Features                                                  │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Private  │ │ Mentions │ │ Ignore   │ │ Broadcasts│    │   │
│  │ │ Messages │ │ System   │ │ List     │ │           │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Core Classes

### ChatChannel

```java
public class ChatChannel {
    private String id;
    private String name;
    private String shortcut;
    private String format;
    private int color;
    private double radius;
    private ChannelType type;
    private String permission;
    private boolean defaultChannel;
    private boolean persistent;
    
    public enum ChannelType {
        GLOBAL,         // Server-wide
        LOCAL,          // Proximity-based
        WORLD,          // World-specific
        STAFF,          // Staff only
        PARTY,          // Party members
        FACTION,        // Faction members
        PRIVATE         // Custom private channels
    }
    
    public static final ChatChannel GLOBAL = new ChatChannel(
        "global", "Global", "g", 
        "&7[G] {prefix}{player}{suffix}: {message}",
        0xFFFFFF, -1, ChannelType.GLOBAL, null, true
    );
    
    public static final ChatChannel LOCAL = new ChatChannel(
        "local", "Local", "l",
        "&e[L] {player}: {message}",
        0xFFEB3B, 100, ChannelType.LOCAL, null, false
    );
    
    public static final ChatChannel STAFF = new ChatChannel(
        "staff", "Staff", "s",
        "&c[Staff] {player}: {message}",
        0xFF5555, -1, ChannelType.STAFF, "chat.channel.staff", false
    );
}
```

### ChatMessage

```java
public class ChatMessage {
    private UUID id;
    private UUID sender;
    private String senderName;
    private String content;
    private String formattedContent;
    private ChatChannel channel;
    private long timestamp;
    private boolean cancelled;
    private String cancelReason;
    private Set<UUID> recipients;
    private Map<String, String> metadata;
    
    public ChatMessage(UUID sender, String content, ChatChannel channel) {
        this.id = UUID.randomUUID();
        this.sender = sender;
        this.content = content;
        this.channel = channel;
        this.timestamp = System.currentTimeMillis();
        this.recipients = new HashSet<>();
        this.metadata = new HashMap<>();
    }
}
```

### ChatManager

```java
public class ChatManager {
    
    private Map<String, ChatChannel> channels;
    private Map<UUID, String> playerFocusedChannel;
    private Map<UUID, Set<String>> playerMutedChannels;
    private Map<UUID, Set<UUID>> ignoreList;
    private Map<UUID, UUID> lastMessageFrom;
    private FilterChain filterChain;
    private FormatProcessor formatProcessor;
    private ChatConfig config;
    
    // Channel Management
    public void registerChannel(ChatChannel channel);
    public void unregisterChannel(String channelId);
    public Optional<ChatChannel> getChannel(String id);
    public List<ChatChannel> getChannels();
    public List<ChatChannel> getAccessibleChannels(UUID playerId);
    
    // Messaging
    public void sendMessage(UUID sender, String message);
    public void sendMessage(UUID sender, String message, String channelId);
    public void sendPrivateMessage(UUID sender, UUID recipient, String message);
    public void broadcast(String message);
    public void broadcast(String message, String permission);
    
    // Channel Participation
    public void joinChannel(UUID playerId, String channelId);
    public void leaveChannel(UUID playerId, String channelId);
    public void setFocusedChannel(UUID playerId, String channelId);
    public String getFocusedChannel(UUID playerId);
    public void muteChannel(UUID playerId, String channelId);
    public void unmuteChannel(UUID playerId, String channelId);
    
    // Ignore System
    public void ignorePlayer(UUID playerId, UUID targetId);
    public void unignorePlayer(UUID playerId, UUID targetId);
    public boolean isIgnoring(UUID playerId, UUID targetId);
    public Set<UUID> getIgnoreList(UUID playerId);
    
    // Reply System
    public void reply(UUID sender, String message);
    public Optional<UUID> getLastMessageFrom(UUID playerId);
    
    // Moderation
    public void mutePlayer(UUID playerId, Duration duration, String reason);
    public void unmutePlayer(UUID playerId);
    public boolean isMuted(UUID playerId);
    public MuteInfo getMuteInfo(UUID playerId);
}
```

## Filter System

### FilterChain

```java
public class FilterChain {
    private List<ChatFilter> filters;
    
    public FilterResult process(ChatMessage message) {
        for (ChatFilter filter : filters) {
            FilterResult result = filter.filter(message);
            if (result.isBlocked()) {
                return result;
            }
            if (result.isModified()) {
                message.setContent(result.getModifiedContent());
            }
        }
        return FilterResult.allowed();
    }
}

public interface ChatFilter {
    FilterResult filter(ChatMessage message);
    String getName();
    int getPriority();
}

public record FilterResult(
    boolean blocked,
    boolean modified,
    String modifiedContent,
    String blockReason
) {
    public static FilterResult allowed() {
        return new FilterResult(false, false, null, null);
    }
    
    public static FilterResult blocked(String reason) {
        return new FilterResult(true, false, null, reason);
    }
    
    public static FilterResult modified(String newContent) {
        return new FilterResult(false, true, newContent, null);
    }
}
```

### Built-in Filters

```java
public class ProfanityFilter implements ChatFilter {
    private Set<String> blockedWords;
    private ReplacementMode mode;
    
    public enum ReplacementMode {
        BLOCK,          // Block entire message
        CENSOR,         // Replace with ***
        REMOVE,         // Remove the word
        REPLACE         // Replace with safe alternative
    }
}

public class SpamFilter implements ChatFilter {
    private int maxMessagesPerMinute;
    private int maxRepetitions;
    private int minMessageInterval;
    private Map<UUID, List<Long>> messageHistory;
}

public class CapsFilter implements ChatFilter {
    private double maxCapsPercentage;
    private int minMessageLength;
}

public class LinkFilter implements ChatFilter {
    private boolean allowLinks;
    private Set<String> whitelistedDomains;
    private String permission;
}

public class AdvertisingFilter implements ChatFilter {
    private Set<String> blockedPatterns;
    private boolean blockIPs;
    private boolean blockServerNames;
}
```

## Format System

### FormatProcessor

```java
public class FormatProcessor {
    private PermissionManager permissions;
    
    public String format(ChatMessage message, UUID viewer) {
        String format = message.getChannel().getFormat();
        
        format = format.replace("{player}", message.getSenderName());
        format = format.replace("{message}", processMessageFormat(message, viewer));
        format = format.replace("{prefix}", permissions.getPrefix(message.getSender()));
        format = format.replace("{suffix}", permissions.getSuffix(message.getSender()));
        format = format.replace("{channel}", message.getChannel().getName());
        
        return translateColorCodes(format, message.getSender());
    }
    
    private String processMessageFormat(ChatMessage message, UUID viewer) {
        String content = message.getContent();
        
        if (permissions.hasPermission(message.getSender(), "chat.color")) {
            content = translateColorCodes(content, message.getSender());
        }
        if (permissions.hasPermission(message.getSender(), "chat.format")) {
            content = translateFormatCodes(content);
        }
        
        content = processMentions(content, message.getSender(), viewer);
        
        return content;
    }
}
```

### Color Codes

| Code | Color | Code | Style |
|------|-------|------|-------|
| `&0` | Black | `&l` | Bold |
| `&1` | Dark Blue | `&m` | Strikethrough |
| `&2` | Dark Green | `&n` | Underline |
| `&3` | Dark Aqua | `&o` | Italic |
| `&4` | Dark Red | `&r` | Reset |
| `&5` | Dark Purple | | |
| `&6` | Gold | | |
| `&7` | Gray | | |
| `&8` | Dark Gray | | |
| `&9` | Blue | | |
| `&a` | Green | | |
| `&b` | Aqua | | |
| `&c` | Red | | |
| `&d` | Light Purple | | |
| `&e` | Yellow | | |
| `&f` | White | | |

### Hex Colors

```
&#RRGGBB - Hex color code
Example: &#FF5555Hello&#55FF55World
```

## Commands

### Channel Commands

```
/channel list                   - List available channels
/channel join <name>            - Join a channel
/channel leave [name]           - Leave a channel
/channel focus <name>           - Set default channel
/ch <name> <message>            - Send to specific channel
```

### Message Shortcuts

```
/g <message>                    - Send to global
/l <message>                    - Send to local
/s <message>                    - Send to staff
/p <message>                    - Send to party
```

### Private Messages

```
/msg <player> <message>         - Send private message
/tell <player> <message>        - Alias for /msg
/whisper <player> <message>     - Alias for /msg
/w <player> <message>           - Alias for /msg
/r <message>                    - Reply to last message
```

### Ignore Commands

```
/ignore <player>                - Ignore a player
/ignore list                    - Show ignored players
/unignore <player>              - Unignore a player
```

### Moderation Commands

```
/mute <player> <duration> [reason] - Mute a player
/unmute <player>                - Unmute a player
/mutelist                       - List muted players
/clearchat                      - Clear chat for all
/slowmode <seconds>             - Enable slow mode
```

### Admin Commands

```
/broadcast <message>            - Server broadcast
/bc <message>                   - Alias for /broadcast
/chatlog <player> [lines]       - View chat history
/chatchannel create <name>      - Create channel
/chatchannel delete <name>      - Delete channel
```

## Events

```java
public class ChatMessageEvent extends Event implements Cancellable {
    private ChatMessage message;
    private boolean cancelled;
    private String cancelReason;
}

public class PrivateMessageEvent extends Event implements Cancellable {
    private UUID sender;
    private UUID recipient;
    private String message;
    private boolean cancelled;
}

public class ChannelJoinEvent extends Event implements Cancellable {
    private UUID playerId;
    private ChatChannel channel;
    private boolean cancelled;
}

public class PlayerMutedEvent extends Event {
    private UUID playerId;
    private UUID mutedBy;
    private Duration duration;
    private String reason;
}
```

## Integration Examples

### Party Chat

```java
partyManager.onPartyCreated((party) -> {
    ChatChannel channel = chatManager.createChannel(
        "party-" + party.getId(),
        "Party",
        ChannelType.PARTY
    );
    channel.setFormat("&d[Party] {player}: {message}");
    party.setChatChannel(channel);
});

partyManager.onMemberJoined((party, member) -> {
    chatManager.joinChannel(member, party.getChatChannel().getId());
});
```

### Permission-Based Formatting

```java
chatManager.onMessageFormat((message) -> {
    UUID sender = message.getSender();
    Role role = permissions.getPrimaryRole(sender);
    
    String format = message.getChannel().getFormat();
    format = format.replace("{role}", role.getName());
    format = format.replace("{rolecolor}", role.getColorHex());
    
    message.setFormat(format);
});
```

### Chat Logging

```java
chatManager.onMessage((message) -> {
    logger.info("[{}] {}: {}",
        message.getChannel().getName(),
        message.getSenderName(),
        message.getContent()
    );
});
```

## Configuration

```yaml
# chat.yml
channels:
  global:
    name: "Global"
    shortcut: "g"
    format: "&7[G] {prefix}{player}{suffix}: {message}"
    default: true
    
  local:
    name: "Local"
    shortcut: "l"
    format: "&e[L] {player}: {message}"
    radius: 100
    
  staff:
    name: "Staff"
    shortcut: "s"
    format: "&c[Staff] {player}: {message}"
    permission: "chat.channel.staff"

filters:
  profanity:
    enabled: true
    mode: "CENSOR"
    words:
      - "badword1"
      - "badword2"
      
  spam:
    enabled: true
    max_messages_per_minute: 10
    max_repetitions: 3
    min_interval_ms: 500
    
  caps:
    enabled: true
    max_percentage: 0.7
    min_length: 10
    
  links:
    enabled: true
    allow_whitelisted: true
    whitelist:
      - "youtube.com"
      - "twitch.tv"
    permission: "chat.links"

format:
  allow_colors: false
  color_permission: "chat.color"
  allow_format: false
  format_permission: "chat.format"

moderation:
  log_messages: true
  log_private_messages: false
  cooldown_enabled: true
  cooldown_ms: 1000
  cooldown_bypass_permission: "chat.bypass.cooldown"
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `chat.speak` | Send messages | All |
| `chat.color` | Use color codes | VIP |
| `chat.format` | Use formatting | VIP |
| `chat.links` | Post links | VIP |
| `chat.channel.*` | Access all channels | Staff |
| `chat.channel.staff` | Access staff channel | Staff |
| `chat.bypass.cooldown` | Bypass chat cooldown | VIP |
| `chat.bypass.filter` | Bypass chat filter | Staff |
| `chat.mute` | Mute players | Staff |
| `chat.broadcast` | Send broadcasts | Admin |
