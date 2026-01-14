package rubidium.chat.messaging;

import rubidium.hytale.api.player.Player;
import rubidium.annotations.Command;
import rubidium.core.logging.RubidiumLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class ReplyManager {
    
    public record Conversation(
        UUID participant1,
        UUID participant2,
        Instant lastActivity,
        List<Message> recentMessages
    ) {
        private static final int MAX_HISTORY = 20;
        
        public void addMessage(Message message) {
            recentMessages.add(message);
            if (recentMessages.size() > MAX_HISTORY) {
                recentMessages.remove(0);
            }
        }
    }
    
    public record Message(
        UUID senderId,
        String senderName,
        String content,
        Instant timestamp
    ) {}
    
    public record PrivateMessageResult(
        boolean success,
        String errorMessage,
        Player recipient
    ) {
        public static PrivateMessageResult success(Player recipient) {
            return new PrivateMessageResult(true, null, recipient);
        }
        
        public static PrivateMessageResult failure(String error) {
            return new PrivateMessageResult(false, error, null);
        }
    }
    
    private final RubidiumLogger logger;
    private final MentionService mentionService;
    private final Map<UUID, UUID> lastMessagePartner = new ConcurrentHashMap<>();
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final Set<UUID> socialSpyEnabled = ConcurrentHashMap.newKeySet();
    
    private java.util.function.Function<String, Optional<Player>> playerLookup;
    private java.util.function.Function<UUID, Optional<Player>> playerById;
    private java.util.function.Consumer<Player> socialSpyNotifier;
    
    private volatile String privateMessageFormat = "&7[&d%sender% &7-> &d%recipient%&7] &f%message%";
    private volatile String replyFormat = "&7[&d%sender% &7-> &d%recipient%&7] &f%message%";
    private volatile Duration conversationTimeout = Duration.ofHours(1);
    
    public ReplyManager(RubidiumLogger logger, MentionService mentionService) {
        this.logger = logger;
        this.mentionService = mentionService;
    }
    
    public void setPlayerLookup(java.util.function.Function<String, Optional<Player>> lookup) {
        this.playerLookup = lookup;
    }
    
    public void setPlayerById(java.util.function.Function<UUID, Optional<Player>> lookup) {
        this.playerById = lookup;
    }
    
    public PrivateMessageResult sendPrivateMessage(Player sender, String recipientName, String message) {
        if (playerLookup == null) {
            return PrivateMessageResult.failure("Player lookup not configured");
        }
        
        var recipientOpt = playerLookup.apply(recipientName);
        if (recipientOpt.isEmpty()) {
            return PrivateMessageResult.failure("Player '" + recipientName + "' is not online.");
        }
        
        var recipient = recipientOpt.get();
        
        if (recipient.getUniqueId().equals(sender.getUniqueId())) {
            return PrivateMessageResult.failure("You cannot message yourself.");
        }
        
        var settings = mentionService.getSettings(recipient.getUniqueId());
        if (!settings.privateMessagesEnabled()) {
            return PrivateMessageResult.failure(recipient.getName() + " has private messages disabled.");
        }
        
        if (settings.blockedUsers().contains(sender.getUniqueId())) {
            return PrivateMessageResult.failure("You cannot message this player.");
        }
        
        deliverPrivateMessage(sender, recipient, message);
        
        return PrivateMessageResult.success(recipient);
    }
    
    public PrivateMessageResult reply(Player sender, String message) {
        var targetId = lastMessagePartner.get(sender.getUniqueId());
        
        if (targetId == null) {
            return PrivateMessageResult.failure("You have no one to reply to.");
        }
        
        if (playerById == null) {
            return PrivateMessageResult.failure("Player lookup not configured");
        }
        
        var recipientOpt = playerById.apply(targetId);
        if (recipientOpt.isEmpty()) {
            return PrivateMessageResult.failure("That player is no longer online.");
        }
        
        var recipient = recipientOpt.get();
        deliverPrivateMessage(sender, recipient, message);
        
        return PrivateMessageResult.success(recipient);
    }
    
    private void deliverPrivateMessage(Player sender, Player recipient, String message) {
        var formattedToSender = formatMessage(privateMessageFormat, sender.getName(), recipient.getName(), message);
        var formattedToRecipient = formatMessage(privateMessageFormat, sender.getName(), recipient.getName(), message);
        
        sender.sendMessage(formattedToSender.replace("%sender%", "You").replace(sender.getName(), "You"));
        recipient.sendMessage(formattedToRecipient);
        
        lastMessagePartner.put(sender.getUniqueId(), recipient.getUniqueId());
        lastMessagePartner.put(recipient.getUniqueId(), sender.getUniqueId());
        
        mentionService.registerIncomingMessage(sender.getUniqueId(), sender.getName(), recipient.getUniqueId());
        
        recordConversation(sender, recipient, message);
        
        notifySocialSpy(sender, recipient, message);
        
        logger.debug("PM: " + sender.getName() + " -> " + recipient.getName() + ": " + message);
    }
    
    private void recordConversation(Player sender, Player recipient, String message) {
        var key = getConversationKey(sender.getUniqueId(), recipient.getUniqueId());
        var conversation = conversations.computeIfAbsent(key, k -> 
            new Conversation(sender.getUniqueId(), recipient.getUniqueId(), Instant.now(), new ArrayList<>())
        );
        
        conversation.addMessage(new Message(
            sender.getUniqueId(),
            sender.getName(),
            message,
            Instant.now()
        ));
    }
    
    private String getConversationKey(UUID id1, UUID id2) {
        if (id1.compareTo(id2) < 0) {
            return id1.toString() + ":" + id2.toString();
        }
        return id2.toString() + ":" + id1.toString();
    }
    
    public void enableSocialSpy(UUID playerId) {
        socialSpyEnabled.add(playerId);
    }
    
    public void disableSocialSpy(UUID playerId) {
        socialSpyEnabled.remove(playerId);
    }
    
    public boolean isSocialSpyEnabled(UUID playerId) {
        return socialSpyEnabled.contains(playerId);
    }
    
    private void notifySocialSpy(Player sender, Player recipient, String message) {
        if (playerById == null) return;
        
        var spyMessage = "&8[SocialSpy] " + formatMessage(privateMessageFormat, sender.getName(), recipient.getName(), message);
        
        for (var spyId : socialSpyEnabled) {
            if (spyId.equals(sender.getUniqueId()) || spyId.equals(recipient.getUniqueId())) {
                continue;
            }
            
            playerById.apply(spyId).ifPresent(spy -> {
                spy.sendMessage(spyMessage);
            });
        }
    }
    
    private String formatMessage(String format, String sender, String recipient, String message) {
        return format
            .replace("%sender%", sender)
            .replace("%recipient%", recipient)
            .replace("%message%", message);
    }
    
    public void setPrivateMessageFormat(String format) {
        this.privateMessageFormat = format;
    }
    
    public void setReplyFormat(String format) {
        this.replyFormat = format;
    }
    
    public void setConversationTimeout(Duration timeout) {
        this.conversationTimeout = timeout;
    }
    
    public Optional<Conversation> getConversation(UUID player1, UUID player2) {
        var key = getConversationKey(player1, player2);
        return Optional.ofNullable(conversations.get(key));
    }
    
    public void clearConversationHistory(UUID playerId) {
        conversations.entrySet().removeIf(entry -> 
            entry.getKey().contains(playerId.toString())
        );
        lastMessagePartner.remove(playerId);
    }
    
    public void cleanupExpiredConversations() {
        var cutoff = Instant.now().minus(conversationTimeout);
        conversations.entrySet().removeIf(entry -> 
            entry.getValue().lastActivity().isBefore(cutoff)
        );
    }
}
