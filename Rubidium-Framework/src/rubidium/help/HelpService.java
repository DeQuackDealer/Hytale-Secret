package rubidium.help;

import rubidium.hytale.api.player.Player;
import rubidium.commands.Command;
import rubidium.commands.CommandContext;
import rubidium.commands.CommandResult;
import rubidium.core.RubidiumLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.stream.*;

public final class HelpService {
    
    public record HelpEntry(
        String name,
        String description,
        String usage,
        String permission,
        List<String> aliases,
        String category,
        List<HelpArgument> arguments,
        List<String> examples,
        String detailedDescription,
        int priority
    ) implements Comparable<HelpEntry> {
        @Override
        public int compareTo(HelpEntry other) {
            var categoryCompare = this.category.compareTo(other.category);
            if (categoryCompare != 0) return categoryCompare;
            return Integer.compare(other.priority, this.priority);
        }
    }
    
    public record HelpArgument(
        String name,
        String description,
        boolean required,
        String defaultValue,
        List<String> validValues
    ) {}
    
    public record HelpCategory(
        String id,
        String displayName,
        String description,
        String icon,
        int sortOrder
    ) {}
    
    public record SearchResult(
        HelpEntry entry,
        double relevance,
        String matchedOn
    ) implements Comparable<SearchResult> {
        @Override
        public int compareTo(SearchResult other) {
            return Double.compare(other.relevance, this.relevance);
        }
    }
    
    private final RubidiumLogger logger;
    private final Map<String, HelpEntry> entries = new ConcurrentHashMap<>();
    private final Map<String, HelpCategory> categories = new ConcurrentHashMap<>();
    private final Map<String, String> aliasToCommand = new ConcurrentHashMap<>();
    
    private BiFunction<Player, String, Boolean> permissionChecker;
    
    private volatile int entriesPerPage = 8;
    private volatile String headerFormat = "&6=== &e%title% &6===";
    private volatile String entryFormat = "&e/%command% &7- %description%";
    private volatile String footerFormat = "&7Page %page%/%total% | /help %category% <page>";
    
    public HelpService(RubidiumLogger logger) {
        this.logger = logger;
        initializeDefaultCategories();
    }
    
    private void initializeDefaultCategories() {
        registerCategory(new HelpCategory("general", "General", "General server commands", "📋", 0));
        registerCategory(new HelpCategory("chat", "Chat", "Chat and messaging commands", "💬", 1));
        registerCategory(new HelpCategory("moderation", "Moderation", "Server moderation commands", "🛡️", 2));
        registerCategory(new HelpCategory("admin", "Administration", "Server administration commands", "⚙️", 3));
        registerCategory(new HelpCategory("teleport", "Teleportation", "Teleport and warp commands", "🌍", 4));
        registerCategory(new HelpCategory("economy", "Economy", "Economy and trading commands", "💰", 5));
        registerCategory(new HelpCategory("misc", "Miscellaneous", "Other commands", "📦", 10));
    }
    
    public void setPermissionChecker(BiFunction<Player, String, Boolean> checker) {
        this.permissionChecker = checker;
    }
    
    public void registerCommand(HelpEntry entry) {
        entries.put(entry.name().toLowerCase(), entry);
        
        for (var alias : entry.aliases()) {
            aliasToCommand.put(alias.toLowerCase(), entry.name().toLowerCase());
        }
        
        logger.debug("Registered help for command: " + entry.name());
    }
    
    public void registerCategory(HelpCategory category) {
        categories.put(category.id().toLowerCase(), category);
    }
    
    public void registerFromAnnotation(String name, Command annotation, List<HelpArgument> arguments, List<String> examples, String detailedDesc) {
        var entry = new HelpEntry(
            name,
            annotation.description(),
            annotation.usage(),
            annotation.permission(),
            Arrays.asList(annotation.aliases()),
            inferCategory(name, annotation.permission()),
            arguments,
            examples,
            detailedDesc,
            50
        );
        
        registerCommand(entry);
    }
    
    private String inferCategory(String name, String permission) {
        if (permission.contains("admin")) return "admin";
        if (permission.contains("mod")) return "moderation";
        if (permission.contains("chat")) return "chat";
        if (permission.contains("teleport") || permission.contains("warp")) return "teleport";
        if (permission.contains("eco")) return "economy";
        return "general";
    }
    
    public Optional<HelpEntry> getCommand(String name) {
        var entry = entries.get(name.toLowerCase());
        if (entry != null) return Optional.of(entry);
        
        var aliased = aliasToCommand.get(name.toLowerCase());
        if (aliased != null) {
            return Optional.ofNullable(entries.get(aliased));
        }
        
        return Optional.empty();
    }
    
    public List<HelpEntry> getCommandsByCategory(String category, Player player) {
        return entries.values().stream()
            .filter(e -> e.category().equalsIgnoreCase(category))
            .filter(e -> hasPermission(player, e.permission()))
            .sorted()
            .toList();
    }
    
    public List<HelpEntry> getAllCommands(Player player) {
        return entries.values().stream()
            .filter(e -> hasPermission(player, e.permission()))
            .sorted()
            .toList();
    }
    
    public List<SearchResult> search(String query, Player player) {
        var lowerQuery = query.toLowerCase();
        var results = new ArrayList<SearchResult>();
        
        for (var entry : entries.values()) {
            if (!hasPermission(player, entry.permission())) continue;
            
            double relevance = 0;
            String matchedOn = null;
            
            if (entry.name().toLowerCase().equals(lowerQuery)) {
                relevance = 1.0;
                matchedOn = "exact name match";
            } else if (entry.name().toLowerCase().startsWith(lowerQuery)) {
                relevance = 0.9;
                matchedOn = "name prefix";
            } else if (entry.name().toLowerCase().contains(lowerQuery)) {
                relevance = 0.7;
                matchedOn = "name contains";
            }
            
            for (var alias : entry.aliases()) {
                if (alias.toLowerCase().equals(lowerQuery)) {
                    relevance = Math.max(relevance, 0.95);
                    matchedOn = "alias: " + alias;
                } else if (alias.toLowerCase().contains(lowerQuery)) {
                    relevance = Math.max(relevance, 0.6);
                    matchedOn = "alias contains: " + alias;
                }
            }
            
            if (entry.description().toLowerCase().contains(lowerQuery)) {
                relevance = Math.max(relevance, 0.5);
                matchedOn = matchedOn != null ? matchedOn : "description";
            }
            
            if (entry.detailedDescription() != null && 
                entry.detailedDescription().toLowerCase().contains(lowerQuery)) {
                relevance = Math.max(relevance, 0.3);
                matchedOn = matchedOn != null ? matchedOn : "detailed description";
            }
            
            if (relevance > 0) {
                results.add(new SearchResult(entry, relevance, matchedOn));
            }
        }
        
        return results.stream().sorted().toList();
    }
    
    public void showMainHelp(Player player, int page) {
        var allCommands = getAllCommands(player);
        var totalPages = (int) Math.ceil((double) allCommands.size() / entriesPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        var start = (page - 1) * entriesPerPage;
        var end = Math.min(start + entriesPerPage, allCommands.size());
        
        player.sendMessage(formatHeader("Help"));
        player.sendMessage("");
        
        player.sendMessage("&7Available categories:");
        for (var category : categories.values().stream()
                .sorted(Comparator.comparingInt(HelpCategory::sortOrder))
                .toList()) {
            var count = getCommandsByCategory(category.id(), player).size();
            if (count > 0) {
                player.sendMessage("  &e/help " + category.id() + " &7- " + category.displayName() + " (" + count + " commands)");
            }
        }
        
        player.sendMessage("");
        player.sendMessage("&7Use &e/help <command>&7 for detailed help on a specific command.");
        player.sendMessage("&7Use &e/help search <query>&7 to search for commands.");
    }
    
    public void showCategoryHelp(Player player, String categoryId, int page) {
        var category = categories.get(categoryId.toLowerCase());
        if (category == null) {
            showSearchResults(player, categoryId, page);
            return;
        }
        
        var commands = getCommandsByCategory(categoryId, player);
        
        if (commands.isEmpty()) {
            player.sendMessage("&cNo commands available in this category.");
            return;
        }
        
        var totalPages = (int) Math.ceil((double) commands.size() / entriesPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        var start = (page - 1) * entriesPerPage;
        var end = Math.min(start + entriesPerPage, commands.size());
        
        player.sendMessage(formatHeader(category.displayName() + " Commands"));
        player.sendMessage("&7" + category.description());
        player.sendMessage("");
        
        for (int i = start; i < end; i++) {
            var entry = commands.get(i);
            player.sendMessage(formatEntry(entry));
        }
        
        player.sendMessage("");
        player.sendMessage(formatFooter(categoryId, page, totalPages));
    }
    
    public void showCommandHelp(Player player, String commandName) {
        var entryOpt = getCommand(commandName);
        
        if (entryOpt.isEmpty()) {
            showSearchResults(player, commandName, 1);
            return;
        }
        
        var entry = entryOpt.get();
        
        if (!hasPermission(player, entry.permission())) {
            player.sendMessage("&cYou don't have permission to view this command.");
            return;
        }
        
        player.sendMessage(formatHeader("/" + entry.name()));
        player.sendMessage("");
        
        player.sendMessage("&fDescription: &7" + entry.description());
        
        if (entry.detailedDescription() != null && !entry.detailedDescription().isEmpty()) {
            player.sendMessage("&7" + entry.detailedDescription());
        }
        
        player.sendMessage("");
        player.sendMessage("&fUsage: &e" + entry.usage());
        
        if (!entry.aliases().isEmpty()) {
            player.sendMessage("&fAliases: &7" + String.join(", ", entry.aliases()));
        }
        
        if (!entry.arguments().isEmpty()) {
            player.sendMessage("");
            player.sendMessage("&fArguments:");
            for (var arg : entry.arguments()) {
                var required = arg.required() ? "&c(required)" : "&7(optional)";
                player.sendMessage("  &e" + arg.name() + " " + required + " &7- " + arg.description());
                
                if (!arg.validValues().isEmpty()) {
                    player.sendMessage("    &7Valid values: " + String.join(", ", arg.validValues()));
                }
                if (arg.defaultValue() != null) {
                    player.sendMessage("    &7Default: " + arg.defaultValue());
                }
            }
        }
        
        if (!entry.examples().isEmpty()) {
            player.sendMessage("");
            player.sendMessage("&fExamples:");
            for (var example : entry.examples()) {
                player.sendMessage("  &e" + example);
            }
        }
        
        player.sendMessage("");
        player.sendMessage("&fPermission: &7" + entry.permission());
        player.sendMessage("&fCategory: &7" + entry.category());
    }
    
    public void showSearchResults(Player player, String query, int page) {
        var results = search(query, player);
        
        if (results.isEmpty()) {
            player.sendMessage("&cNo commands found matching '&f" + query + "&c'.");
            player.sendMessage("&7Try a different search term or use &e/help&7 to see all categories.");
            return;
        }
        
        var totalPages = (int) Math.ceil((double) results.size() / entriesPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        var start = (page - 1) * entriesPerPage;
        var end = Math.min(start + entriesPerPage, results.size());
        
        player.sendMessage(formatHeader("Search: " + query));
        player.sendMessage("&7Found " + results.size() + " result(s)");
        player.sendMessage("");
        
        for (int i = start; i < end; i++) {
            var result = results.get(i);
            player.sendMessage(formatEntry(result.entry()) + " &8(" + result.matchedOn() + ")");
        }
        
        if (totalPages > 1) {
            player.sendMessage("");
            player.sendMessage("&7Page " + page + "/" + totalPages + " | /help search " + query + " <page>");
        }
    }
    
    private String formatHeader(String title) {
        return headerFormat.replace("%title%", title);
    }
    
    private String formatEntry(HelpEntry entry) {
        return entryFormat
            .replace("%command%", entry.name())
            .replace("%description%", entry.description());
    }
    
    private String formatFooter(String category, int page, int totalPages) {
        return footerFormat
            .replace("%category%", category)
            .replace("%page%", String.valueOf(page))
            .replace("%total%", String.valueOf(totalPages));
    }
    
    private boolean hasPermission(Player player, String permission) {
        if (permission == null || permission.isEmpty()) return true;
        if (permissionChecker == null) return true;
        return permissionChecker.apply(player, permission);
    }
    
    public void setEntriesPerPage(int count) {
        this.entriesPerPage = count;
    }
    
    public void setHeaderFormat(String format) {
        this.headerFormat = format;
    }
    
    public void setEntryFormat(String format) {
        this.entryFormat = format;
    }
    
    @Command(
        name = "help",
        aliases = {"?", "commands"},
        description = "Display help information",
        usage = "/help [command|category|search] [page]",
        permission = "rubidium.help"
    )
    public CommandResult helpCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        if (ctx.args().length == 0) {
            showMainHelp(player, 1);
            return CommandResult.success();
        }
        
        var firstArg = ctx.args()[0].toLowerCase();
        
        if (firstArg.equals("search") && ctx.args().length > 1) {
            var query = String.join(" ", Arrays.copyOfRange(ctx.args(), 1, ctx.args().length));
            int page = 1;
            try {
                page = Integer.parseInt(ctx.args()[ctx.args().length - 1]);
                query = String.join(" ", Arrays.copyOfRange(ctx.args(), 1, ctx.args().length - 1));
            } catch (NumberFormatException ignored) {}
            
            showSearchResults(player, query, page);
            return CommandResult.success();
        }
        
        int page = 1;
        if (ctx.args().length > 1) {
            try {
                page = Integer.parseInt(ctx.args()[1]);
            } catch (NumberFormatException ignored) {}
        }
        
        if (categories.containsKey(firstArg)) {
            showCategoryHelp(player, firstArg, page);
        } else {
            showCommandHelp(player, firstArg);
        }
        
        return CommandResult.success();
    }
}
