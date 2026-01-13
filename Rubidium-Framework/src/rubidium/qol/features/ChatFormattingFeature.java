package rubidium.qol.features;

import rubidium.core.logging.RubidiumLogger;
import rubidium.qol.QoLFeature;

import java.util.*;
import java.util.regex.*;

public class ChatFormattingFeature extends QoLFeature {
    
    public record ChatConfig(
        String chatFormat,
        boolean enableColorCodes,
        boolean enableMentions,
        boolean enableLinks,
        String mentionColor,
        String linkColor,
        Map<String, String> customPlaceholders
    ) {
        public static ChatConfig defaults() {
            return new ChatConfig(
                "[{prefix}] {player}: {message}",
                true,
                true,
                false,
                "&e",
                "&9",
                new HashMap<>()
            );
        }
    }
    
    public enum ColorCode {
        BLACK('0', "\u001B[30m"),
        DARK_BLUE('1', "\u001B[34m"),
        DARK_GREEN('2', "\u001B[32m"),
        DARK_AQUA('3', "\u001B[36m"),
        DARK_RED('4', "\u001B[31m"),
        DARK_PURPLE('5', "\u001B[35m"),
        GOLD('6', "\u001B[33m"),
        GRAY('7', "\u001B[37m"),
        DARK_GRAY('8', "\u001B[90m"),
        BLUE('9', "\u001B[94m"),
        GREEN('a', "\u001B[92m"),
        AQUA('b', "\u001B[96m"),
        RED('c', "\u001B[91m"),
        LIGHT_PURPLE('d', "\u001B[95m"),
        YELLOW('e', "\u001B[93m"),
        WHITE('f', "\u001B[97m"),
        BOLD('l', "\u001B[1m"),
        ITALIC('o', "\u001B[3m"),
        UNDERLINE('n', "\u001B[4m"),
        STRIKETHROUGH('m', "\u001B[9m"),
        RESET('r', "\u001B[0m");
        
        private final char code;
        private final String ansi;
        
        ColorCode(char code, String ansi) {
            this.code = code;
            this.ansi = ansi;
        }
        
        public char getCode() { return code; }
        public String getAnsi() { return ansi; }
        
        private static final Map<Character, ColorCode> BY_CODE = new HashMap<>();
        static {
            for (ColorCode cc : values()) {
                BY_CODE.put(cc.code, cc);
            }
        }
        
        public static Optional<ColorCode> fromCode(char code) {
            return Optional.ofNullable(BY_CODE.get(Character.toLowerCase(code)));
        }
    }
    
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fklmnor])");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://[\\w.-]+(?:\\.[\\w.-]+)+(?:/[\\w._~:/?#\\[\\]@!$&'()*+,;=-]*)?)"
    );
    
    private ChatConfig config;
    private final Map<String, String> playerPrefixes = new HashMap<>();
    
    public ChatFormattingFeature(RubidiumLogger logger) {
        super("chat-formatting", "Chat Formatting", 
            "Provides color codes, mentions, and customizable chat format",
            logger);
        this.config = ChatConfig.defaults();
    }
    
    public void setConfig(ChatConfig config) {
        this.config = config;
    }
    
    public ChatConfig getConfig() {
        return config;
    }
    
    public void setPlayerPrefix(String playerId, String prefix) {
        playerPrefixes.put(playerId, prefix);
    }
    
    public void removePlayerPrefix(String playerId) {
        playerPrefixes.remove(playerId);
    }
    
    public String getPlayerPrefix(String playerId) {
        return playerPrefixes.getOrDefault(playerId, "Player");
    }
    
    @Override
    protected void onEnable() {
        logger.debug("Chat formatting enabled");
    }
    
    @Override
    protected void onDisable() {
        playerPrefixes.clear();
    }
    
    public String formatMessage(String sender, String message, Set<String> onlinePlayers) {
        if (!enabled) {
            return message;
        }
        
        String formatted = config.chatFormat()
            .replace("{player}", sender)
            .replace("{prefix}", getPlayerPrefix(sender))
            .replace("{message}", message);
        
        for (Map.Entry<String, String> entry : config.customPlaceholders().entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        if (config.enableColorCodes()) {
            formatted = translateColorCodes(formatted);
        }
        
        if (config.enableMentions() && onlinePlayers != null) {
            formatted = highlightMentions(formatted, onlinePlayers);
        }
        
        if (config.enableLinks()) {
            formatted = highlightLinks(formatted);
        }
        
        return formatted;
    }
    
    public String translateColorCodes(String text) {
        Matcher matcher = COLOR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            char code = matcher.group(1).charAt(0);
            Optional<ColorCode> colorCode = ColorCode.fromCode(code);
            String replacement = colorCode.map(ColorCode::getAnsi).orElse("");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    public String stripColorCodes(String text) {
        return COLOR_PATTERN.matcher(text).replaceAll("");
    }
    
    private String highlightMentions(String text, Set<String> onlinePlayers) {
        Matcher matcher = MENTION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String mentioned = matcher.group(1);
            if (onlinePlayers.contains(mentioned)) {
                String highlight = translateColorCodes(config.mentionColor()) + "@" + mentioned + ColorCode.RESET.getAnsi();
                matcher.appendReplacement(result, Matcher.quoteReplacement(highlight));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String highlightLinks(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String url = matcher.group(1);
            String highlight = translateColorCodes(config.linkColor()) + url + ColorCode.RESET.getAnsi();
            matcher.appendReplacement(result, Matcher.quoteReplacement(highlight));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    public Set<String> extractMentions(String message) {
        Set<String> mentions = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(message);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}
