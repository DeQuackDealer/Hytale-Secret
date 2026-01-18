package rubidium.api.chat;

import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.api.npc.NPCAPI;
import rubidium.api.event.EventAPI;

import java.util.UUID;
import java.util.function.Predicate;

public final class ChatAPI {
    
    private static final String DEFAULT_BOT_COLOR = "&7";
    private static final String DEFAULT_NPC_COLOR = "&e";
    
    private ChatAPI() {}
    
    public static void broadcast(String message) {
        for (Player player : Server.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        System.out.println("[Chat] " + stripColors(message));
    }
    
    public static void broadcast(String message, Predicate<Player> filter) {
        for (Player player : Server.getOnlinePlayers()) {
            if (filter.test(player)) {
                player.sendMessage(message);
            }
        }
    }
    
    public static void broadcastWorld(String worldId, String message) {
        for (Player player : Server.getOnlinePlayers()) {
            if (worldId.equals(player.getWorld())) {
                player.sendMessage(message);
            }
        }
    }
    
    public static void sendAsPlayer(Player sender, String message) {
        String formatted = formatPlayerMessage(sender, message);
        broadcast(formatted);
        EventAPI.fire(new PlayerChatEvent(sender, message, formatted));
    }
    
    public static void sendAsBot(String botName, String message) {
        String formatted = formatBotMessage(botName, message);
        broadcast(formatted);
        EventAPI.fire(new BotChatEvent(botName, message, formatted));
    }
    
    public static void sendAsBot(String botName, String message, String color) {
        String formatted = color + "[" + botName + "] &f" + message;
        broadcast(formatted);
        EventAPI.fire(new BotChatEvent(botName, message, formatted));
    }
    
    public static void sendAsNPC(NPCAPI.NPC npc, String message) {
        String npcName = npc.getDefinition().displayName();
        String formatted = formatNPCMessage(npcName, message);
        broadcast(formatted);
        EventAPI.fire(new NPCChatEvent(npc, message, formatted));
    }
    
    public static void sendAsNPC(String npcName, String message) {
        String formatted = formatNPCMessage(npcName, message);
        broadcast(formatted);
    }
    
    public static void whisper(Player from, Player to, String message) {
        String outgoing = "&7[You -> " + to.getName() + "] " + message;
        String incoming = "&7[" + from.getName() + " -> You] " + message;
        from.sendMessage(outgoing);
        to.sendMessage(incoming);
    }
    
    public static void sendTo(Player player, String message) {
        player.sendMessage(message);
    }
    
    public static void sendTo(UUID playerId, String message) {
        Server.getPlayer(playerId).ifPresent(p -> p.sendMessage(message));
    }
    
    public static void announce(String message) {
        broadcast("&6[Announcement] &f" + message);
    }
    
    public static void announceServer(String message) {
        broadcast("&c[Server] &f" + message);
    }
    
    public static void tip(Player player, String message) {
        player.sendMessage("&aTip: &7" + message);
    }
    
    public static void error(Player player, String message) {
        player.sendMessage("&cError: &7" + message);
    }
    
    public static void success(Player player, String message) {
        player.sendMessage("&aSuccess: &7" + message);
    }
    
    public static void info(Player player, String message) {
        player.sendMessage("&bInfo: &7" + message);
    }
    
    public static void warning(Player player, String message) {
        player.sendMessage("&eWarning: &7" + message);
    }
    
    private static String formatPlayerMessage(Player player, String message) {
        return "&f<" + player.getName() + "> " + message;
    }
    
    private static String formatBotMessage(String botName, String message) {
        return DEFAULT_BOT_COLOR + "[" + botName + "] &f" + message;
    }
    
    private static String formatNPCMessage(String npcName, String message) {
        return DEFAULT_NPC_COLOR + "[" + npcName + "] &f" + message;
    }
    
    private static String stripColors(String message) {
        return message.replaceAll("&[0-9a-fk-or]", "");
    }
    
    public static class PlayerChatEvent extends EventAPI.Event {
        private final Player player;
        private final String message;
        private final String formattedMessage;
        
        public PlayerChatEvent(Player player, String message, String formattedMessage) {
            this.player = player;
            this.message = message;
            this.formattedMessage = formattedMessage;
        }
        
        public Player getPlayer() { return player; }
        public String getMessage() { return message; }
        public String getFormattedMessage() { return formattedMessage; }
    }
    
    public static class BotChatEvent extends EventAPI.Event {
        private final String botName;
        private final String message;
        private final String formattedMessage;
        
        public BotChatEvent(String botName, String message, String formattedMessage) {
            this.botName = botName;
            this.message = message;
            this.formattedMessage = formattedMessage;
        }
        
        public String getBotName() { return botName; }
        public String getMessage() { return message; }
        public String getFormattedMessage() { return formattedMessage; }
    }
    
    public static class NPCChatEvent extends EventAPI.Event {
        private final NPCAPI.NPC npc;
        private final String message;
        private final String formattedMessage;
        
        public NPCChatEvent(NPCAPI.NPC npc, String message, String formattedMessage) {
            this.npc = npc;
            this.message = message;
            this.formattedMessage = formattedMessage;
        }
        
        public NPCAPI.NPC getNpc() { return npc; }
        public String getMessage() { return message; }
        public String getFormattedMessage() { return formattedMessage; }
    }
}
