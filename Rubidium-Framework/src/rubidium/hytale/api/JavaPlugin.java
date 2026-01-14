package rubidium.hytale.api;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Base class for all Rubidium plugins.
 */
public abstract class JavaPlugin {
    
    private PluginMetadata metadata;
    private Logger logger;
    private Path dataFolder;
    private boolean enabled = false;
    
    public final void initializeMetadata(PluginMetadata metadata) {
        this.metadata = metadata;
        this.logger = Logger.getLogger(metadata.name());
        this.dataFolder = Paths.get("plugins", metadata.name());
        
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger.severe("Failed to create data folder: " + e.getMessage());
        }
    }
    
    @Deprecated
    public final void initialize(JavaPluginInit init) {
        initializeMetadata(PluginMetadata.from(init, getClass().getName()));
    }
    
    public abstract void onEnable();
    
    public abstract void onDisable();
    
    public void onLoad() {}
    
    public void onReload() {
        onDisable();
        onEnable();
    }
    
    public String getName() {
        return metadata != null ? metadata.name() : getClass().getSimpleName();
    }
    
    public String getVersion() {
        return metadata != null ? metadata.version() : "1.0.0";
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Path getDataFolder() {
        return dataFolder;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public HytaleServer getServer() {
        return HytaleServer.getInstance();
    }
    
    public void saveDefaultConfig() {
        Path configPath = dataFolder.resolve("config.yml");
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                }
            } catch (IOException e) {
                logger.warning("Failed to save default config: " + e.getMessage());
            }
        }
    }
}
