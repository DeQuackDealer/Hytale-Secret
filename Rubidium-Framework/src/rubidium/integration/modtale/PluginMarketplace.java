package rubidium.integration.modtale;

import rubidium.core.logging.RubidiumLogger;
import rubidium.core.module.ModuleManager;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PluginMarketplace {
    
    private static final String API_KEY_ENV = "MODTALE_API_KEY";
    
    private final ModtaleClient client;
    private final RubidiumLogger logger;
    private final ModuleManager moduleManager;
    private final Path pluginsDirectory;
    
    private final Map<String, InstalledPlugin> installedPlugins = new ConcurrentHashMap<>();
    private List<String> cachedTags = null;
    private List<String> cachedGameVersions = null;
    
    public PluginMarketplace(RubidiumLogger logger, ModuleManager moduleManager, Path pluginsDirectory) {
        this.logger = logger;
        this.moduleManager = moduleManager;
        this.pluginsDirectory = pluginsDirectory;
        
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("MODTALE_API_KEY not set. Modtale API will be rate-limited.");
        }
        this.client = new ModtaleClient(logger, apiKey);
    }
    
    public PluginMarketplace(RubidiumLogger logger, ModuleManager moduleManager, Path pluginsDirectory, String apiKey) {
        this.logger = logger;
        this.moduleManager = moduleManager;
        this.pluginsDirectory = pluginsDirectory;
        this.client = new ModtaleClient(logger, apiKey);
    }
    
    public void initialize() {
        logger.info("Initializing Modtale plugin marketplace");
        
        client.getTags().thenAccept(tags -> {
            cachedTags = tags;
            logger.debug("Loaded {} plugin tags", tags.size());
        }).exceptionally(e -> {
            logger.warn("Failed to load tags from Modtale");
            return null;
        });
        
        client.getGameVersions().thenAccept(versions -> {
            cachedGameVersions = versions;
            logger.debug("Loaded {} game versions", versions.size());
        }).exceptionally(e -> {
            logger.warn("Failed to load game versions from Modtale");
            return null;
        });
        
        scanInstalledPlugins();
    }
    
    public CompletableFuture<ModtaleClient.SearchResult> search(String query) {
        return client.searchPlugins(
            ModtaleClient.SearchQuery.defaults().withSearch(query)
        );
    }
    
    public CompletableFuture<ModtaleClient.SearchResult> search(ModtaleClient.SearchQuery query) {
        return client.searchPlugins(query);
    }
    
    public CompletableFuture<ModtaleClient.SearchResult> browsePopular(int page) {
        return client.searchPlugins(
            new ModtaleClient.SearchQuery(null, page, 20, ModtaleClient.SortOrder.DOWNLOADS, null, null, null)
        );
    }
    
    public CompletableFuture<ModtaleClient.SearchResult> browseNewest(int page) {
        return client.searchPlugins(
            new ModtaleClient.SearchQuery(null, page, 20, ModtaleClient.SortOrder.NEWEST, null, null, null)
        );
    }
    
    public CompletableFuture<ModtaleClient.SearchResult> browseUpdated(int page) {
        return client.searchPlugins(
            new ModtaleClient.SearchQuery(null, page, 20, ModtaleClient.SortOrder.UPDATED, null, null, null)
        );
    }
    
    public CompletableFuture<ModtaleClient.SearchResult> searchByTag(String tag, int page) {
        return client.searchPlugins(
            new ModtaleClient.SearchQuery(null, page, 20, ModtaleClient.SortOrder.DOWNLOADS, List.of(tag), null, null)
        );
    }
    
    public CompletableFuture<ModtaleClient.ProjectDetails> getPluginDetails(String projectId) {
        return client.getProject(projectId);
    }
    
    public CompletableFuture<InstallResult> installPlugin(String projectId) {
        logger.info("Installing plugin from Modtale: {}", projectId);
        
        return client.getProject(projectId)
            .thenCompose(details -> {
                if (details.versions().isEmpty()) {
                    return CompletableFuture.completedFuture(
                        new InstallResult(false, "No versions available for this plugin", null)
                    );
                }
                
                ModtaleClient.ProjectVersion latest = details.versions().get(0);
                
                return client.downloadLatestVersion(projectId, pluginsDirectory)
                    .thenApply(downloadedPath -> {
                        InstalledPlugin installed = new InstalledPlugin(
                            projectId,
                            details.title(),
                            latest.versionNumber(),
                            downloadedPath,
                            details.author()
                        );
                        
                        installedPlugins.put(projectId, installed);
                        
                        logger.info("Plugin {} v{} installed successfully", details.title(), latest.versionNumber());
                        
                        return new InstallResult(true, "Plugin installed successfully. Restart to activate.", downloadedPath);
                    });
            })
            .exceptionally(e -> {
                logger.error("Failed to install plugin {}", projectId, e);
                return new InstallResult(false, "Installation failed: " + e.getMessage(), null);
            });
    }
    
    public CompletableFuture<InstallResult> installPlugin(String projectId, String versionId) {
        logger.info("Installing plugin {} version {}", projectId, versionId);
        
        return client.getProject(projectId)
            .thenCompose(details -> {
                Optional<ModtaleClient.ProjectVersion> version = details.versions().stream()
                    .filter(v -> v.id().equals(versionId) || v.versionNumber().equals(versionId))
                    .findFirst();
                
                if (version.isEmpty()) {
                    return CompletableFuture.completedFuture(
                        new InstallResult(false, "Version not found: " + versionId, null)
                    );
                }
                
                return client.downloadPlugin(projectId, version.get().id(), pluginsDirectory)
                    .thenApply(downloadedPath -> {
                        InstalledPlugin installed = new InstalledPlugin(
                            projectId,
                            details.title(),
                            version.get().versionNumber(),
                            downloadedPath,
                            details.author()
                        );
                        
                        installedPlugins.put(projectId, installed);
                        
                        return new InstallResult(true, "Plugin installed successfully", downloadedPath);
                    });
            })
            .exceptionally(e -> {
                logger.error("Failed to install plugin", e);
                return new InstallResult(false, "Installation failed: " + e.getMessage(), null);
            });
    }
    
    public CompletableFuture<UpdateResult> updatePlugin(String projectId) {
        InstalledPlugin installed = installedPlugins.get(projectId);
        if (installed == null) {
            return CompletableFuture.completedFuture(
                new UpdateResult(false, "Plugin not installed", null, null)
            );
        }
        
        return client.getProject(projectId)
            .thenCompose(details -> {
                if (details.versions().isEmpty()) {
                    return CompletableFuture.completedFuture(
                        new UpdateResult(false, "No versions available", installed.version(), null)
                    );
                }
                
                String latestVersion = details.versions().get(0).versionNumber();
                
                if (latestVersion.equals(installed.version())) {
                    return CompletableFuture.completedFuture(
                        new UpdateResult(false, "Already on latest version", installed.version(), latestVersion)
                    );
                }
                
                return installPlugin(projectId)
                    .thenApply(result -> new UpdateResult(
                        result.success(),
                        result.message(),
                        installed.version(),
                        latestVersion
                    ));
            });
    }
    
    public boolean uninstallPlugin(String projectId) {
        InstalledPlugin installed = installedPlugins.remove(projectId);
        if (installed == null) {
            return false;
        }
        
        try {
            java.nio.file.Files.deleteIfExists(installed.path());
            logger.info("Uninstalled plugin: {}", installed.name());
            return true;
        } catch (java.io.IOException e) {
            logger.error("Failed to delete plugin file", e);
            return false;
        }
    }
    
    public Collection<InstalledPlugin> getInstalledPlugins() {
        return Collections.unmodifiableCollection(installedPlugins.values());
    }
    
    public Optional<InstalledPlugin> getInstalledPlugin(String projectId) {
        return Optional.ofNullable(installedPlugins.get(projectId));
    }
    
    public boolean isInstalled(String projectId) {
        return installedPlugins.containsKey(projectId);
    }
    
    public List<String> getAvailableTags() {
        return cachedTags != null ? cachedTags : List.of();
    }
    
    public List<String> getGameVersions() {
        return cachedGameVersions != null ? cachedGameVersions : List.of();
    }
    
    private void scanInstalledPlugins() {
        try {
            if (!java.nio.file.Files.exists(pluginsDirectory)) {
                java.nio.file.Files.createDirectories(pluginsDirectory);
            }
            
            try (var stream = java.nio.file.Files.list(pluginsDirectory)) {
                stream.filter(p -> p.toString().endsWith(".jar"))
                    .forEach(jarPath -> {
                        String fileName = jarPath.getFileName().toString();
                        logger.debug("Found installed plugin: {}", fileName);
                    });
            }
        } catch (java.io.IOException e) {
            logger.error("Failed to scan plugins directory", e);
        }
    }
    
    public record InstalledPlugin(
        String projectId,
        String name,
        String version,
        Path path,
        String author
    ) {}
    
    public record InstallResult(
        boolean success,
        String message,
        Path installedPath
    ) {}
    
    public record UpdateResult(
        boolean success,
        String message,
        String previousVersion,
        String newVersion
    ) {}
}
