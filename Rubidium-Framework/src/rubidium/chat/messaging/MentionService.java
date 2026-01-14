package rubidium.chat.messaging;

import rubidium.hytale.api.player.Player;
import rubidium.core.RubidiumLogger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class MentionService {
    
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{3,16})");
    private static final Pattern REPLY_INDICATOR = Pattern.compile("^/r\\s+(.*)$", Pattern.CASE_INSENSITIVE);
    
    public record Mention(
        String rawText,
        String targetName,
        int startIndex,
        int endIndex
    ) {}
    
    public record ParsedMessage(
        String originalMessage,
        String formattedMessage,
        List<Mention> mentions,
        List<UUID> mentionedPlayerIds
    ) {}
    
    public record ReplyTarget(
        UUID targetId,
        String targetName,
        Instant lastMessageTime
    ) {}
    
    public record MentionSettings(
        boolean soundEnabled,
        boolean highlightEnabled,
        String highlightColor,
        boolean privateMessagesEnabled,
        Set<UUID> blockedUsers
    ) {
        public static MentionSettings defaults() {
            return new MentionSettings(true, true, "&e", true, new HashSet<>());
        }
    }
    
    private final RubidiumLogger logger;
    private final Map<UUID, ReplyTarget> replyTargets = new ConcurrentHashMap<>();
    private final Map<UUID, MentionSettings> playerSettings = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> mentionCooldowns = new ConcurrentHashMap<>();
    
    private java.util.function.Function<String, Optional<Player>> playerLookup;
    private java.util.function.Consumer<MentionNotification> notificationHandler;
    
    private volatile long cooldownMillis = 1000;
    private volatile String mentionHighlightFormat = "&e@%s&r";
    private volatile String mentionSound = "entity.experience_orb.pickup";
    
    public MentionService(RubidiumLogger logger) {
        this.logger = logger;
    }
    
    public void setPlayerLookup(java.util.function.Function<String, Optional<Player>> lookup) {
        this.playerLookup = lookup;
    }
    
    public void setNotificationHandler(java.util.function.Consumer<MentionNotification> handler) {
        this.notificationHandler = handler;
    }
    
    public ParsedMessage parseMessage(Player sender, String message) {
        var mentions = new ArrayList<Mention>();
        var mentionedPlayerIds = new ArrayList<UUID>();
        var formattedMessage = new StringBuilder(message);
        
        var matcher = MENTION_PATTERN.matcher(message);
        int offset = 0;
        
        while (matcher.find()) {
            var targetName = matcher.group(1);
            var mention = new Mention(
                matcher.group(),
                targetName,
                matcher.start(),
                matcher.end()
            );
            mentions.add(mention);
            
            if (playerLookup != null) {
                playerLookup.apply(targetName).ifPresent(target -> {
                    mentionedPlayerIds.add(target.getUniqueId());
                    
                    var settings = getSettings(target.getUniqueId());
                    if (!settings.blockedUsers().contains(sender.getUniqueId())) {
                        notifyMention(sender, target, message);
                    }
                });
            }
            
            var replacement = String.format(mentionHighlightFormat, targetName);
            formattedMessage.replace(
                matcher.start() + offset,
                matcher.end() + offset,
                replacement
            );
            offset += replacement.length() - matcher.group().length();
        }
        
        updateReplyTarget(sender, mentionedPlayerIds);
        
        return new ParsedMessage(message, formattedMessage.toString(), mentions, mentionedPlayerIds);
    }
    
    public Optional<String> handleReplyCommand(Player sender, String message) {
        var matcher = REPLY_INDICATOR.matcher(message);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        
        var replyContent = matcher.group(1);
        var target = replyTargets.get(sender.getUniqueId());
        
        if (target == null) {
            sender.sendMessage("&cNo one to reply to. Send a message to someone first!");
            return Optional.empty();
        }
        
        if (playerLookup != null) {
            var targetPlayer = playerLookup.apply(target.targetName());
            if (targetPlayer.isEmpty()) {
                sender.sendMessage("&c" + target.targetName() + " is no longer online.");
                return Optional.empty();
            }
            
            return Optional.of("/msg " + target.targetName() + " " + replyContent);
        }
        
        return Optional.empty();
    }
    
    public void registerIncomingMessage(UUID senderId, String senderName, UUID recipientId) {
        replyTargets.put(recipientId, new ReplyTarget(senderId, senderName, Instant.now()));
    }
    
    public Optional<ReplyTarget> getReplyTarget(UUID playerId) {
        return Optional.ofNullable(replyTargets.get(playerId));
    }
    
    public void setReplyTarget(UUID playerId, UUID targetId, String targetName) {
        replyTargets.put(playerId, new ReplyTarget(targetId, targetName, Instant.now()));
    }
    
    public MentionSettings getSettings(UUID playerId) {
        return playerSettings.computeIfAbsent(playerId, k -> MentionSettings.defaults());
    }
    
    public void updateSettings(UUID playerId, MentionSettings settings) {
        playerSettings.put(playerId, settings);
    }
    
    public boolean canMention(UUID senderId) {
        var lastMention = mentionCooldowns.get(senderId);
        if (lastMention == null) {
            mentionCooldowns.put(senderId, Instant.now());
            return true;
        }
        
        var elapsed = Instant.now().toEpochMilli() - lastMention.toEpochMilli();
        if (elapsed >= cooldownMillis) {
            mentionCooldowns.put(senderId, Instant.now());
            return true;
        }
        
        return false;
    }
    
    public void setCooldownMillis(long millis) {
        this.cooldownMillis = millis;
    }
    
    public void setMentionHighlightFormat(String format) {
        this.mentionHighlightFormat = format;
    }
    
    public void setMentionSound(String sound) {
        this.mentionSound = sound;
    }
    
    private void notifyMention(Player sender, Player target, String message) {
        if (notificationHandler == null) return;
        
        var settings = getSettings(target.getUniqueId());
        
        notificationHandler.accept(new MentionNotification(
            sender.getUniqueId(),
            sender.getName(),
            target.getUniqueId(),
            target.getName(),
            message,
            settings.soundEnabled() ? mentionSound : null,
            settings.highlightEnabled()
        ));
    }
    
    private void updateReplyTarget(Player sender, List<UUID> mentionedPlayerIds) {
        if (!mentionedPlayerIds.isEmpty()) {
            var lastMentioned = mentionedPlayerIds.getLast();
            if (playerLookup != null) {
                playerLookup.apply(null);
            }
        }
    }
    
    public record MentionNotification(
        UUID senderId,
        String senderName,
        UUID targetId,
        String targetName,
        String message,
        String soundToPlay,
        boolean shouldHighlight
    ) {}
}
