package rubidium.integration.modtale;

import rubidium.api.player.CommandSender;

import java.util.concurrent.CompletableFuture;

public class MarketplaceCommands {
    
    private final PluginMarketplace marketplace;
    
    public MarketplaceCommands(PluginMarketplace marketplace) {
        this.marketplace = marketplace;
    }
    
    public void handlePlugins(CommandSender sender, String[] args) {
        if (args.length < 1) {
            showHelp(sender);
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "search" -> handleSearch(sender, args);
            case "browse" -> handleBrowse(sender, args);
            case "info" -> handleInfo(sender, args);
            case "install" -> handleInstall(sender, args);
            case "update" -> handleUpdate(sender, args);
            case "uninstall" -> handleUninstall(sender, args);
            case "installed" -> handleInstalled(sender);
            case "tags" -> handleTags(sender);
            default -> showHelp(sender);
        }
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage("=== Modtale Plugin Marketplace ===");
        sender.sendMessage("/plugins search <query> - Search for plugins");
        sender.sendMessage("/plugins browse [popular|newest|updated] - Browse plugins");
        sender.sendMessage("/plugins info <project-id> - View plugin details");
        sender.sendMessage("/plugins install <project-id> [version] - Install a plugin");
        sender.sendMessage("/plugins update <project-id> - Update a plugin");
        sender.sendMessage("/plugins uninstall <project-id> - Uninstall a plugin");
        sender.sendMessage("/plugins installed - List installed plugins");
        sender.sendMessage("/plugins tags - Show available tags");
    }
    
    private void handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /plugins search <query>");
            return;
        }
        
        String query = joinArgs(args, 1);
        sender.sendMessage("Searching for: " + query + "...");
        
        marketplace.search(query)
            .thenAccept(result -> {
                if (result.projects().isEmpty()) {
                    sender.sendMessage("No plugins found matching: " + query);
                    return;
                }
                
                sender.sendMessage("=== Search Results (" + result.totalElements() + " total) ===");
                for (var project : result.projects()) {
                    sender.sendMessage(formatProjectSummary(project));
                }
                
                if (result.totalPages() > 1) {
                    sender.sendMessage("Page 1 of " + result.totalPages());
                }
            })
            .exceptionally(e -> {
                sender.sendMessage("Search failed: " + e.getMessage());
                return null;
            });
    }
    
    private void handleBrowse(CommandSender sender, String[] args) {
        String category = args.length > 1 ? args[1].toLowerCase() : "popular";
        int parsedPage = 0;
        
        if (args.length > 2) {
            try {
                parsedPage = Integer.parseInt(args[2]) - 1;
            } catch (NumberFormatException e) {
                parsedPage = 0;
            }
        }
        
        final int page = parsedPage;
        
        CompletableFuture<ModtaleClient.SearchResult> future = switch (category) {
            case "newest" -> marketplace.browseNewest(page);
            case "updated" -> marketplace.browseUpdated(page);
            default -> marketplace.browsePopular(page);
        };
        
        sender.sendMessage("Loading " + category + " plugins...");
        
        future.thenAccept(result -> {
            if (result.projects().isEmpty()) {
                sender.sendMessage("No plugins found.");
                return;
            }
            
            sender.sendMessage("=== " + capitalize(category) + " Plugins ===");
            for (var project : result.projects()) {
                sender.sendMessage(formatProjectSummary(project));
            }
            
            sender.sendMessage("Page " + (page + 1) + " of " + result.totalPages());
        }).exceptionally(e -> {
            sender.sendMessage("Failed to browse plugins: " + e.getMessage());
            return null;
        });
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /plugins info <project-id>");
            return;
        }
        
        String projectId = args[1];
        sender.sendMessage("Loading plugin details...");
        
        marketplace.getPluginDetails(projectId)
            .thenAccept(details -> {
                sender.sendMessage("=== " + details.title() + " ===");
                sender.sendMessage("Author: " + details.author());
                sender.sendMessage("License: " + (details.license() != null ? details.license() : "Not specified"));
                sender.sendMessage("Status: " + details.status());
                sender.sendMessage("");
                sender.sendMessage(details.description());
                sender.sendMessage("");
                
                if (!details.versions().isEmpty()) {
                    sender.sendMessage("Available Versions:");
                    for (var version : details.versions()) {
                        sender.sendMessage("  - " + version.versionNumber() + " (" + version.downloadCount() + " downloads)");
                    }
                }
                
                if (details.repositoryUrl() != null) {
                    sender.sendMessage("Repository: " + details.repositoryUrl());
                }
                
                boolean installed = marketplace.isInstalled(projectId);
                sender.sendMessage("");
                sender.sendMessage(installed ? "[INSTALLED]" : "Use /plugins install " + projectId + " to install");
            })
            .exceptionally(e -> {
                sender.sendMessage("Failed to load plugin details: " + e.getMessage());
                return null;
            });
    }
    
    private void handleInstall(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /plugins install <project-id> [version]");
            return;
        }
        
        String projectId = args[1];
        String version = args.length > 2 ? args[2] : null;
        
        sender.sendMessage("Installing plugin " + projectId + "...");
        
        CompletableFuture<PluginMarketplace.InstallResult> future;
        if (version != null) {
            future = marketplace.installPlugin(projectId, version);
        } else {
            future = marketplace.installPlugin(projectId);
        }
        
        future.thenAccept(result -> {
            if (result.success()) {
                sender.sendMessage("Successfully installed plugin!");
                sender.sendMessage(result.message());
            } else {
                sender.sendMessage("Installation failed: " + result.message());
            }
        }).exceptionally(e -> {
            sender.sendMessage("Installation failed: " + e.getMessage());
            return null;
        });
    }
    
    private void handleUpdate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /plugins update <project-id>");
            return;
        }
        
        String projectId = args[1];
        sender.sendMessage("Checking for updates...");
        
        marketplace.updatePlugin(projectId)
            .thenAccept(result -> {
                if (result.success()) {
                    sender.sendMessage("Updated from " + result.previousVersion() + " to " + result.newVersion());
                } else {
                    sender.sendMessage(result.message());
                }
            })
            .exceptionally(e -> {
                sender.sendMessage("Update failed: " + e.getMessage());
                return null;
            });
    }
    
    private void handleUninstall(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /plugins uninstall <project-id>");
            return;
        }
        
        String projectId = args[1];
        
        if (marketplace.uninstallPlugin(projectId)) {
            sender.sendMessage("Plugin uninstalled. Restart to apply changes.");
        } else {
            sender.sendMessage("Plugin not found or could not be uninstalled.");
        }
    }
    
    private void handleInstalled(CommandSender sender) {
        var installed = marketplace.getInstalledPlugins();
        
        if (installed.isEmpty()) {
            sender.sendMessage("No plugins installed from Modtale.");
            return;
        }
        
        sender.sendMessage("=== Installed Plugins (" + installed.size() + ") ===");
        for (var plugin : installed) {
            sender.sendMessage(" - " + plugin.name() + " v" + plugin.version() + " by " + plugin.author());
            sender.sendMessage("   ID: " + plugin.projectId());
        }
    }
    
    private void handleTags(CommandSender sender) {
        var tags = marketplace.getAvailableTags();
        
        if (tags.isEmpty()) {
            sender.sendMessage("Tags not loaded. Try again later.");
            return;
        }
        
        sender.sendMessage("=== Available Tags ===");
        sender.sendMessage(String.join(", ", tags));
    }
    
    private String formatProjectSummary(ModtaleClient.ProjectSummary project) {
        String rating = String.format("%.1f", project.rating());
        String installed = marketplace.isInstalled(project.id()) ? " [INSTALLED]" : "";
        
        return " - " + project.title() + " by " + project.author() + installed +
               "\n   " + project.downloads() + " downloads | " + rating + " rating" +
               "\n   ID: " + project.id();
    }
    
    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
