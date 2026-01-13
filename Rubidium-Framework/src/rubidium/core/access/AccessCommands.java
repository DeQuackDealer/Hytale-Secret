package rubidium.core.access;

import rubidium.api.player.CommandSender;
import rubidium.api.player.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessCommands {
    
    private final AccessControlManager accessManager;
    
    public AccessCommands(AccessControlManager accessManager) {
        this.accessManager = accessManager;
    }
    
    public void handleBan(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /ban <player> [reason]");
            return;
        }
        
        String targetPlayer = args[0];
        String reason = args.length > 1 ? joinArgs(args, 1) : "No reason specified";
        String bannedBy = sender instanceof Player ? ((Player) sender).getName() : "Console";
        
        AccessControlManager.BanResult result = accessManager.ban(
            targetPlayer, targetPlayer, reason, bannedBy, null
        );
        
        sender.sendMessage(result.message());
    }
    
    public void handleTempBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /tempban <player> <duration> [reason]");
            sender.sendMessage("Duration format: 1d, 12h, 30m, 1w (days, hours, minutes, weeks)");
            return;
        }
        
        String targetPlayer = args[0];
        String durationStr = args[1];
        String reason = args.length > 2 ? joinArgs(args, 2) : "No reason specified";
        String bannedBy = sender instanceof Player ? ((Player) sender).getName() : "Console";
        
        long durationSeconds = parseDuration(durationStr);
        if (durationSeconds <= 0) {
            sender.sendMessage("Invalid duration format. Use: 1d, 12h, 30m, 1w");
            return;
        }
        
        AccessControlManager.BanResult result = accessManager.tempBan(
            targetPlayer, targetPlayer, reason, bannedBy, durationSeconds
        );
        
        sender.sendMessage(result.message());
    }
    
    public void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /unban <player>");
            return;
        }
        
        String targetPlayer = args[0];
        boolean unbanned = accessManager.unban(targetPlayer);
        
        if (unbanned) {
            sender.sendMessage("Player " + targetPlayer + " has been unbanned.");
        } else {
            sender.sendMessage("Player " + targetPlayer + " is not banned.");
        }
    }
    
    public void handleBanList(CommandSender sender) {
        var bans = accessManager.getBanList();
        
        if (bans.isEmpty()) {
            sender.sendMessage("No players are currently banned.");
            return;
        }
        
        sender.sendMessage("=== Banned Players (" + bans.size() + ") ===");
        for (var entry : bans) {
            String expiry = entry.isPermanent() ? "permanent" : "expires " + entry.expiresAt();
            sender.sendMessage(" - " + entry.playerName() + " (" + entry.playerId() + ")");
            sender.sendMessage("   Reason: " + entry.reason() + " | " + expiry);
        }
    }
    
    public void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /whitelist <add|remove|list|on|off|clear> [player]");
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /whitelist add <player>");
                    return;
                }
                AccessControlManager.WhitelistResult result = accessManager.addToWhitelist(args[1]);
                sender.sendMessage(result.message());
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /whitelist remove <player>");
                    return;
                }
                boolean removed = accessManager.removeFromWhitelist(args[1]);
                sender.sendMessage(removed ? "Player removed from whitelist." : "Player not on whitelist.");
            }
            case "list" -> {
                Set<String> whitelist = accessManager.getWhitelist();
                if (whitelist.isEmpty()) {
                    sender.sendMessage("Whitelist is empty.");
                } else {
                    sender.sendMessage("=== Whitelisted Players (" + whitelist.size() + ") ===");
                    for (String player : whitelist) {
                        sender.sendMessage(" - " + player);
                    }
                }
            }
            case "on" -> {
                accessManager.setAccessMode(AccessMode.PRIVATE);
                sender.sendMessage("Whitelist enabled. Only whitelisted players can join.");
            }
            case "off" -> {
                accessManager.setAccessMode(AccessMode.PUBLIC);
                sender.sendMessage("Whitelist disabled. Server is now public.");
            }
            case "clear" -> {
                accessManager.clearWhitelist();
                sender.sendMessage("Whitelist cleared.");
            }
            default -> sender.sendMessage("Unknown action. Use: add, remove, list, on, off, clear");
        }
    }
    
    public void handleAccessMode(CommandSender sender, String[] args) {
        if (args.length < 1) {
            AccessMode current = accessManager.getAccessMode();
            sender.sendMessage("Current access mode: " + current);
            sender.sendMessage("Usage: /accessmode <public|private>");
            return;
        }
        
        String mode = args[0].toUpperCase();
        try {
            AccessMode newMode = AccessMode.valueOf(mode);
            accessManager.setAccessMode(newMode);
            sender.sendMessage("Access mode set to " + newMode);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid mode. Use: public or private");
        }
    }
    
    private long parseDuration(String duration) {
        Pattern pattern = Pattern.compile("(\\d+)([smhdw])");
        Matcher matcher = pattern.matcher(duration.toLowerCase());
        
        if (!matcher.matches()) {
            return -1;
        }
        
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        
        return switch (unit) {
            case "s" -> value;
            case "m" -> value * 60;
            case "h" -> value * 3600;
            case "d" -> value * 86400;
            case "w" -> value * 604800;
            default -> -1;
        };
    }
    
    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
