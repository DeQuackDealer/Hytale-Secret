package rubidium.hytale.api;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Base class for all Rubidium plugins.
 */
public abstract class JavaPlugin {
    
    private JavaPluginInit init;
    private Logger logger;
    private Path dataFolder;
    private boolean enabled = false;
    
    public final void initialize(JavaPluginInit init) {
        this.init = init;
        this.logger = Logger.getLogger(init.name());
        this.dataFolder = Paths.get("plugins", init.name());
        
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger.error("Failed to create data folder: " + e.getMessage());
        }
    }
    
    public abstract void onEnable();
    
    public abstract void onDisable();
    
    public void onLoad() {}
    
    public void onReload() {
        onDisable();
        onEnable();
    }
    
    public String getName() {
        return init != null ? init.name() : getClass().getSimpleName();
    }
    
    public String getVersion() {
        return init != null ? init.version() : "1.0.0";
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
                logger.warn("Failed to save default config: " + e.getMessage());
            }
        }
    }
}
