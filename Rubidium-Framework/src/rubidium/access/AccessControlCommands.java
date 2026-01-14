package rubidium.access;

import rubidium.access.AccessControlService.*;
import rubidium.commands.Command;
import rubidium.commands.CommandContext;
import rubidium.commands.CommandResult;
import rubidium.hytale.api.player.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class AccessControlCommands {
    
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");
    
    private final AccessControlService accessControl;
    
    public AccessControlCommands(AccessControlService accessControl) {
        this.accessControl = accessControl;
    }
    
    @Command(
        name = "whitelist",
        description = "Manage the server whitelist",
        usage = "/whitelist <add|remove|list|on|off|import> [player] [duration] [reason]",
        permission = "rubidium.access.whitelist"
    )
    public CommandResult whitelistCommand(CommandContext ctx) {
        if (ctx.args().length == 0) {
            return showWhitelistHelp(ctx);
        }
        
        var subcommand = ctx.args()[0].toLowerCase();
        
        return switch (subcommand) {
            case "add" -> addToWhitelist(ctx);
            case "remove" -> removeFromWhitelist(ctx);
            case "list" -> listWhitelist(ctx);
            case "on" -> setWhitelistMode(ctx, true);
            case "off" -> setWhitelistMode(ctx, false);
            case "import" -> importWhitelist(ctx);
            default -> showWhitelistHelp(ctx);
        };
    }
    
    private CommandResult addToWhitelist(CommandContext ctx) {
        if (ctx.args().length < 2) {
            ctx.sender().sendMessage("&cUsage: /whitelist add <player> [duration] [reason]");
            return CommandResult.failure("Missing player argument");
        }
        
        var target = ctx.args()[1];
        var duration = ctx.args().length > 2 ? parseDuration(ctx.args()[2]) : null;
        var reason = ctx.args().length > 3 ? String.join(" ", Arrays.copyOfRange(ctx.args(), 3, ctx.args().length)) : null;
        
        ctx.sender().sendMessage("&7Adding &f" + target + "&7 to whitelist...");
        
        accessControl.addToWhitelist(target, ctx.sender().getName(), reason, duration)
            .thenAccept(success -> {
                if (success) {
                    ctx.sender().sendMessage("&a" + target + " has been added to the whitelist.");
                } else {
                    ctx.sender().sendMessage("&cFailed to add " + target + " to whitelist. Could not resolve player.");
                }
            });
        
        return CommandResult.success();
    }
    
    private CommandResult removeFromWhitelist(CommandContext ctx) {
        if (ctx.args().length < 2) {
            ctx.sender().sendMessage("&cUsage: /whitelist remove <player>");
            return CommandResult.failure("Missing player argument");
        }
        
        var target = ctx.args()[1];
        
        accessControl.scrapeUUIDs(List.of(target)).thenAccept(resolved -> {
            if (resolved.isEmpty()) {
                ctx.sender().sendMessage("&cCould not resolve player: " + target);
                return;
            }
            
            var player = resolved.getFirst();
            if (accessControl.removeFromWhitelist(player.uuid(), ctx.sender().getName())) {
                ctx.sender().sendMessage("&a" + player.username() + " has been removed from the whitelist.");
            } else {
                ctx.sender().sendMessage("&c" + target + " was not on the whitelist.");
            }
        });
        
        return CommandResult.success();
    }
    
    private CommandResult listWhitelist(CommandContext ctx) {
        var entries = accessControl.getWhitelist();
        
        if (entries.isEmpty()) {
            ctx.sender().sendMessage("&7The whitelist is empty.");
            return CommandResult.success();
        }
        
        ctx.sender().sendMessage("&6=== Whitelist (" + entries.size() + " entries) ===");
        for (var entry : entries) {
            var expiry = entry.isPermanent() ? "&7permanent" : "&e" + formatDuration(entry.expiresAt());
            ctx.sender().sendMessage("&f" + entry.username() + " &7(" + expiry + "&7)");
        }
        
        return CommandResult.success();
    }
    
    private CommandResult setWhitelistMode(CommandContext ctx, boolean enabled) {
        if (enabled) {
            accessControl.setMode(AccessMode.WHITELIST_ONLY);
            ctx.sender().sendMessage("&aWhitelist has been enabled.");
        } else {
            accessControl.setMode(AccessMode.DISABLED);
            ctx.sender().sendMessage("&cWhitelist has been disabled.");
        }
        return CommandResult.success();
    }
    
    private CommandResult importWhitelist(CommandContext ctx) {
        if (ctx.args().length < 2) {
            ctx.sender().sendMessage("&cUsage: /whitelist import <url>");
            return CommandResult.failure("Missing URL argument");
        }
        
        var url = ctx.args()[1];
        ctx.sender().sendMessage("&7Importing from URL...");
        
        accessControl.importFromUrl(url, AccessType.WHITELIST, ctx.sender().getName())
            .thenAccept(count -> {
                ctx.sender().sendMessage("&aImported " + count + " players to the whitelist.");
            })
            .exceptionally(e -> {
                ctx.sender().sendMessage("&cFailed to import: " + e.getMessage());
                return null;
            });
        
        return CommandResult.success();
    }
    
    private CommandResult showWhitelistHelp(CommandContext ctx) {
        ctx.sender().sendMessage("&6=== Whitelist Commands ===");
        ctx.sender().sendMessage("&e/whitelist add <player> [duration] [reason] &7- Add player");
        ctx.sender().sendMessage("&e/whitelist remove <player> &7- Remove player");
        ctx.sender().sendMessage("&e/whitelist list &7- Show all whitelisted players");
        ctx.sender().sendMessage("&e/whitelist on|off &7- Enable/disable whitelist");
        ctx.sender().sendMessage("&e/whitelist import <url> &7- Import UUIDs from URL");
        ctx.sender().sendMessage("");
        ctx.sender().sendMessage("&7Duration format: 1h, 7d, 30d, 1M, 1y");
        return CommandResult.success();
    }
    
    @Command(
        name = "ban",
        aliases = {"blacklist"},
        description = "Ban a player from the server",
        usage = "/ban <player> [duration] [reason]",
        permission = "rubidium.access.ban"
    )
    public CommandResult banCommand(CommandContext ctx) {
        if (ctx.args().length == 0) {
            return showBanHelp(ctx);
        }
        
        var subcommand = ctx.args()[0].toLowerCase();
        
        if (subcommand.equals("list")) {
            return listBans(ctx);
        }
        
        var target = ctx.args()[0];
        var duration = ctx.args().length > 1 ? parseDuration(ctx.args()[1]) : null;
        var reason = ctx.args().length > 2 ? String.join(" ", Arrays.copyOfRange(ctx.args(), 2, ctx.args().length)) : "Banned by operator";
        
        ctx.sender().sendMessage("&7Banning &f" + target + "&7...");
        
        accessControl.addToBlacklist(target, ctx.sender().getName(), reason, duration)
            .thenAccept(success -> {
                if (success) {
                    var durationStr = duration != null ? " for " + formatDuration(duration) : " permanently";
                    ctx.sender().sendMessage("&c" + target + " has been banned" + durationStr + ".");
                } else {
                    ctx.sender().sendMessage("&cFailed to ban " + target + ". Could not resolve player.");
                }
            });
        
        return CommandResult.success();
    }
    
    @Command(
        name = "unban",
        aliases = {"pardon"},
        description = "Unban a player from the server",
        usage = "/unban <player>",
        permission = "rubidium.access.ban"
    )
    public CommandResult unbanCommand(CommandContext ctx) {
        if (ctx.args().length == 0) {
            ctx.sender().sendMessage("&cUsage: /unban <player>");
            return CommandResult.failure("Missing player argument");
        }
        
        var target = ctx.args()[0];
        
        accessControl.scrapeUUIDs(List.of(target)).thenAccept(resolved -> {
            if (resolved.isEmpty()) {
                ctx.sender().sendMessage("&cCould not resolve player: " + target);
                return;
            }
            
            var player = resolved.getFirst();
            if (accessControl.removeFromBlacklist(player.uuid(), ctx.sender().getName())) {
                ctx.sender().sendMessage("&a" + player.username() + " has been unbanned.");
            } else {
                ctx.sender().sendMessage("&c" + target + " was not banned.");
            }
        });
        
        return CommandResult.success();
    }
    
    private CommandResult listBans(CommandContext ctx) {
        var entries = accessControl.getBlacklist();
        
        if (entries.isEmpty()) {
            ctx.sender().sendMessage("&7No players are currently banned.");
            return CommandResult.success();
        }
        
        ctx.sender().sendMessage("&6=== Banned Players (" + entries.size() + ") ===");
        for (var entry : entries) {
            var expiry = entry.isPermanent() ? "&cpermanent" : "&e" + formatDuration(entry.expiresAt());
            ctx.sender().sendMessage("&f" + entry.username() + " &7(" + expiry + "&7) - &c" + 
                (entry.reason() != null ? entry.reason() : "No reason"));
        }
        
        return CommandResult.success();
    }
    
    private CommandResult showBanHelp(CommandContext ctx) {
        ctx.sender().sendMessage("&6=== Ban Commands ===");
        ctx.sender().sendMessage("&e/ban <player> [duration] [reason] &7- Ban a player");
        ctx.sender().sendMessage("&e/ban list &7- Show all banned players");
        ctx.sender().sendMessage("&e/unban <player> &7- Unban a player");
        ctx.sender().sendMessage("");
        ctx.sender().sendMessage("&7Duration format: 1h, 7d, 30d, 1M, 1y");
        ctx.sender().sendMessage("&7Leave duration empty for permanent ban");
        return CommandResult.success();
    }
    
    private Duration parseDuration(String input) {
        if (input == null) return null;
        
        var matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.matches()) return null;
        
        long amount = Long.parseLong(matcher.group(1));
        var unit = matcher.group(2);
        
        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            case "w" -> Duration.ofDays(amount * 7);
            case "M" -> Duration.ofDays(amount * 30);
            case "y" -> Duration.ofDays(amount * 365);
            default -> null;
        };
    }
    
    private String formatDuration(java.time.Instant expiry) {
        var remaining = Duration.between(java.time.Instant.now(), expiry);
        if (remaining.isNegative()) return "expired";
        
        if (remaining.toDays() > 0) {
            return remaining.toDays() + " days";
        } else if (remaining.toHours() > 0) {
            return remaining.toHours() + " hours";
        } else if (remaining.toMinutes() > 0) {
            return remaining.toMinutes() + " minutes";
        } else {
            return remaining.toSeconds() + " seconds";
        }
    }
    
    private String formatDuration(Duration duration) {
        if (duration.toDays() > 0) {
            return duration.toDays() + " days";
        } else if (duration.toHours() > 0) {
            return duration.toHours() + " hours";
        } else if (duration.toMinutes() > 0) {
            return duration.toMinutes() + " minutes";
        } else {
            return duration.toSeconds() + " seconds";
        }
    }
}
