package rubidium.chat;

import rubidium.hytale.api.player.Player;
import rubidium.placeholder.PlaceholderService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/**
 * Advanced chat system with channels, formatting, gradients, mentions, and filters.
 */
public class ChatManager {
    
    private static ChatManager instance;
    
    private final Map<String, ChatChannel> channels;
    private final Map<UUID, PlayerChatState> playerStates;
    private final List<ChatFilter> filters;
    private final Map<String, String> emojis;
    private String defaultFormat = "{prefix}{player}{suffix}: {message}";
    
    private ChatManager() {
        this.channels = new ConcurrentHashMap<>();
        this.playerStates = new ConcurrentHashMap<>();
        this.filters = Collections.synchronizedList(new ArrayList<>());
        this.emojis = new ConcurrentHashMap<>();
        
        createDefaultChannels();
        registerDefaultEmojis();
    }
    
    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }
    
    private void createDefaultChannels() {
        registerChannel(new ChatChannel("global", "Global", "§7[§aG§7]", 0, true));
        registerChannel(new ChatChannel("local", "Local", "§7[§eL§7]", 100, false));
        registerChannel(new ChatChannel("staff", "Staff", "§7[§cS§7]", -1, false).setPermission("rubidium.chat.staff"));
    }
    
    private void registerDefaultEmojis() {
        emojis.put(":heart:", "❤");
        emojis.put(":star:", "★");
        emojis.put(":smile:", "☺");
        emojis.put(":fire:", "🔥");
        emojis.put(":check:", "✓");
        emojis.put(":x:", "✗");
        emojis.put(":arrow:", "→");
        emojis.put(":diamond:", "◆");
        emojis.put(":circle:", "●");
        emojis.put(":square:", "■");
    }
    
    public void registerChannel(ChatChannel channel) {
        channels.put(channel.getId(), channel);
    }
    
    public Optional<ChatChannel> getChannel(String id) {
        return Optional.ofNullable(channels.get(id));
    }
    
    public Collection<ChatChannel> getChannels() {
        return channels.values();
    }
    
    public void addFilter(ChatFilter filter) {
        filters.add(filter);
    }
    
    public void removeFilter(ChatFilter filter) {
        filters.remove(filter);
    }
    
    public void registerEmoji(String code, String replacement) {
        emojis.put(code, replacement);
    }
    
    public PlayerChatState getPlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerChatState(player.getUuid()));
    }
    
    public ChatResult processMessage(Player sender, String message) {
        PlayerChatState state = getPlayerState(sender);
        ChatChannel channel = channels.getOrDefault(state.getCurrentChannel(), channels.get("global"));
        
        for (ChatFilter filter : filters) {
            ChatFilter.Result result = filter.filter(sender, message);
            if (result.blocked()) {
                return ChatResult.blocked(result.reason());
            }
            message = result.message();
        }
        
        message = processEmojis(message);
        message = processMentions(message);
        
        String formatted = formatMessage(sender, message, channel);
        
        if (formatted.contains("&") || formatted.contains("§")) {
            formatted = processColorCodes(formatted);
        }
        
        return ChatResult.success(formatted, channel);
    }
    
    public void broadcast(Player sender, String formattedMessage, ChatChannel channel) {
        for (Player player : rubidium.hytale.api.HytaleServer.getInstance().getOnlinePlayers()) {
            if (channel.getRange() > 0) {
                double distance = sender.getLocation().distance(player.getLocation());
                if (distance > channel.getRange()) continue;
            }
            
            if (channel.getPermission() != null && !player.hasPermission(channel.getPermission())) {
                continue;
            }
            
            if (getPlayerState(player).isChannelMuted(channel.getId())) {
                continue;
            }
            
            player.sendMessage(formattedMessage);
        }
    }
    
    private String formatMessage(Player sender, String message, ChatChannel channel) {
        String format = channel.getFormat() != null ? channel.getFormat() : defaultFormat;
        
        format = format.replace("{channel}", channel.getPrefix());
        format = format.replace("{player}", sender.getDisplayName());
        format = format.replace("{message}", message);
        
        format = PlaceholderService.getInstance().parse(sender, format);
        
        return format;
    }
    
    private String processEmojis(String message) {
        for (Map.Entry<String, String> entry : emojis.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }
    
    private String processMentions(String message) {
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String name = matcher.group(1);
            matcher.appendReplacement(result, "§b@" + name + "§r");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processColorCodes(String message) {
        return message.replace("&", "§");
    }
    
    public String applyGradient(String text, String startColor, String endColor) {
        int[] startRGB = hexToRGB(startColor);
        int[] endRGB = hexToRGB(endColor);
        
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            int r = (int) (startRGB[0] + ratio * (endRGB[0] - startRGB[0]));
            int g = (int) (startRGB[1] + ratio * (endRGB[1] - startRGB[1]));
            int b = (int) (startRGB[2] + ratio * (endRGB[2] - startRGB[2]));
            
            result.append("§x§").append(toHexChar(r >> 4)).append("§").append(toHexChar(r & 0xF))
                  .append("§").append(toHexChar(g >> 4)).append("§").append(toHexChar(g & 0xF))
                  .append("§").append(toHexChar(b >> 4)).append("§").append(toHexChar(b & 0xF))
                  .append(text.charAt(i));
        }
        
        return result.toString();
    }
    
    private int[] hexToRGB(String hex) {
        hex = hex.replace("#", "");
        return new int[]{
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
    
    private char toHexChar(int value) {
        return "0123456789abcdef".charAt(value & 0xF);
    }
    
    public void setDefaultFormat(String format) {
        this.defaultFormat = format;
    }
}
