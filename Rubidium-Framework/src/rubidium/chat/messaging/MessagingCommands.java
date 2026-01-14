package rubidium.chat.messaging;

import rubidium.commands.Command;
import rubidium.commands.CommandContext;
import rubidium.commands.CommandResult;
import rubidium.hytale.api.player.Player;

import java.util.Arrays;

public final class MessagingCommands {
    
    private final ReplyManager replyManager;
    private final MentionService mentionService;
    
    public MessagingCommands(ReplyManager replyManager, MentionService mentionService) {
        this.replyManager = replyManager;
        this.mentionService = mentionService;
    }
    
    @Command(
        name = "msg",
        aliases = {"message", "tell", "whisper", "w", "pm"},
        description = "Send a private message to a player",
        usage = "/msg <player> <message>",
        permission = "rubidium.chat.msg"
    )
    public CommandResult msgCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        if (ctx.args().length < 2) {
            ctx.sender().sendMessage("&cUsage: /msg <player> <message>");
            return CommandResult.failure("Invalid arguments");
        }
        
        var targetName = ctx.args()[0];
        var message = String.join(" ", Arrays.copyOfRange(ctx.args(), 1, ctx.args().length));
        
        var result = replyManager.sendPrivateMessage(player, targetName, message);
        
        if (!result.success()) {
            ctx.sender().sendMessage("&c" + result.errorMessage());
            return CommandResult.failure(result.errorMessage());
        }
        
        return CommandResult.success();
    }
    
    @Command(
        name = "r",
        aliases = {"reply"},
        description = "Reply to the last player who messaged you",
        usage = "/r <message>",
        permission = "rubidium.chat.reply"
    )
    public CommandResult replyCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        if (ctx.args().length == 0) {
            var target = mentionService.getReplyTarget(player.getUniqueId());
            if (target.isPresent()) {
                ctx.sender().sendMessage("&7Your reply target is: &f" + target.get().targetName());
            } else {
                ctx.sender().sendMessage("&cYou have no one to reply to.");
            }
            return CommandResult.success();
        }
        
        var message = String.join(" ", ctx.args());
        var result = replyManager.reply(player, message);
        
        if (!result.success()) {
            ctx.sender().sendMessage("&c" + result.errorMessage());
            return CommandResult.failure(result.errorMessage());
        }
        
        return CommandResult.success();
    }
    
    @Command(
        name = "socialspy",
        aliases = {"spy"},
        description = "Toggle viewing of private messages between other players",
        usage = "/socialspy [on|off]",
        permission = "rubidium.chat.socialspy"
    )
    public CommandResult socialSpyCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        boolean enable;
        
        if (ctx.args().length > 0) {
            var arg = ctx.args()[0].toLowerCase();
            enable = switch (arg) {
                case "on", "true", "enable", "1" -> true;
                case "off", "false", "disable", "0" -> false;
                default -> !replyManager.isSocialSpyEnabled(player.getUniqueId());
            };
        } else {
            enable = !replyManager.isSocialSpyEnabled(player.getUniqueId());
        }
        
        if (enable) {
            replyManager.enableSocialSpy(player.getUniqueId());
            ctx.sender().sendMessage("&aSocial Spy enabled. You will now see private messages between other players.");
        } else {
            replyManager.disableSocialSpy(player.getUniqueId());
            ctx.sender().sendMessage("&cSocial Spy disabled.");
        }
        
        return CommandResult.success();
    }
    
    @Command(
        name = "ignore",
        aliases = {"block"},
        description = "Block a player from messaging you",
        usage = "/ignore <player>",
        permission = "rubidium.chat.ignore"
    )
    public CommandResult ignoreCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        if (ctx.args().length == 0) {
            var settings = mentionService.getSettings(player.getUniqueId());
            if (settings.blockedUsers().isEmpty()) {
                ctx.sender().sendMessage("&7You are not ignoring anyone.");
            } else {
                ctx.sender().sendMessage("&7Ignored players: &f" + settings.blockedUsers().size());
            }
            return CommandResult.success();
        }
        
        ctx.sender().sendMessage("&7To block a player, use their UUID. Feature coming soon.");
        return CommandResult.success();
    }
    
    @Command(
        name = "togglemsg",
        aliases = {"togglepm", "dnd"},
        description = "Toggle receiving private messages",
        usage = "/togglemsg",
        permission = "rubidium.chat.togglemsg"
    )
    public CommandResult toggleMsgCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        var currentSettings = mentionService.getSettings(player.getUniqueId());
        var newSettings = new MentionService.MentionSettings(
            currentSettings.soundEnabled(),
            currentSettings.highlightEnabled(),
            currentSettings.highlightColor(),
            !currentSettings.privateMessagesEnabled(),
            currentSettings.blockedUsers()
        );
        
        mentionService.updateSettings(player.getUniqueId(), newSettings);
        
        if (newSettings.privateMessagesEnabled()) {
            ctx.sender().sendMessage("&aPrivate messages are now &lenabled&a.");
        } else {
            ctx.sender().sendMessage("&cPrivate messages are now &ldisabled&c.");
        }
        
        return CommandResult.success();
    }
    
    @Command(
        name = "mentionsettings",
        aliases = {"msettings"},
        description = "Configure mention and notification settings",
        usage = "/mentionsettings <sound|highlight> <on|off>",
        permission = "rubidium.chat.mentions"
    )
    public CommandResult mentionSettingsCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        var currentSettings = mentionService.getSettings(player.getUniqueId());
        
        if (ctx.args().length == 0) {
            ctx.sender().sendMessage("&6=== Mention Settings ===");
            ctx.sender().sendMessage("&7Sound: " + (currentSettings.soundEnabled() ? "&aEnabled" : "&cDisabled"));
            ctx.sender().sendMessage("&7Highlight: " + (currentSettings.highlightEnabled() ? "&aEnabled" : "&cDisabled"));
            ctx.sender().sendMessage("&7Private Messages: " + (currentSettings.privateMessagesEnabled() ? "&aEnabled" : "&cDisabled"));
            ctx.sender().sendMessage("");
            ctx.sender().sendMessage("&7Use: /mentionsettings <sound|highlight> <on|off>");
            return CommandResult.success();
        }
        
        if (ctx.args().length < 2) {
            ctx.sender().sendMessage("&cUsage: /mentionsettings <sound|highlight> <on|off>");
            return CommandResult.failure("Invalid arguments");
        }
        
        var setting = ctx.args()[0].toLowerCase();
        var value = ctx.args()[1].toLowerCase();
        var enabled = value.equals("on") || value.equals("true") || value.equals("enable");
        
        var newSettings = switch (setting) {
            case "sound" -> new MentionService.MentionSettings(
                enabled,
                currentSettings.highlightEnabled(),
                currentSettings.highlightColor(),
                currentSettings.privateMessagesEnabled(),
                currentSettings.blockedUsers()
            );
            case "highlight" -> new MentionService.MentionSettings(
                currentSettings.soundEnabled(),
                enabled,
                currentSettings.highlightColor(),
                currentSettings.privateMessagesEnabled(),
                currentSettings.blockedUsers()
            );
            default -> {
                ctx.sender().sendMessage("&cUnknown setting: " + setting);
                yield null;
            }
        };
        
        if (newSettings != null) {
            mentionService.updateSettings(player.getUniqueId(), newSettings);
            ctx.sender().sendMessage("&aMention " + setting + " " + (enabled ? "enabled" : "disabled") + ".");
        }
        
        return CommandResult.success();
    }
}
