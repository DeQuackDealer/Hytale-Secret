package jurassictale;

import rubidium.api.RubidiumPlugin;
import rubidium.api.PluginInfo;
import rubidium.core.logging.RubidiumLogger;

import jurassictale.dino.DinoManager;
import jurassictale.dino.DinoRegistry;
import jurassictale.territory.TerritoryManager;
import jurassictale.compound.CompoundManager;
import jurassictale.compound.power.PowerGridManager;
import jurassictale.items.ItemRegistry;
import jurassictale.capture.CaptureManager;
import jurassictale.events.ChaosStormManager;

import java.nio.file.Path;

@PluginInfo(
    id = "jurassictale",
    name = "JurassicTale",
    version = "0.1.0",
    description = "Fantasy adventure survival MMO with dinosaurs in the Exclusion Zone",
    authors = {"Yellow Tale Team"}
)
public class JurassicTalePlugin implements RubidiumPlugin {
    
    private RubidiumLogger logger;
    private Path dataDir;
    private JurassicTaleConfig config;
    
    private DinoRegistry dinoRegistry;
    private DinoManager dinoManager;
    private TerritoryManager territoryManager;
    private CompoundManager compoundManager;
    private PowerGridManager powerGridManager;
    private ItemRegistry itemRegistry;
    private CaptureManager captureManager;
    private ChaosStormManager chaosStormManager;
    
    @Override
    public void onEnable(RubidiumLogger logger, Path dataDir) {
        this.logger = logger;
        this.dataDir = dataDir;
        
        logger.info("=================================");
        logger.info("  JurassicTale v0.1.0");
        logger.info("  Project Extinction");
        logger.info("=================================");
        
        loadConfig();
        initializeManagers();
        registerContent();
        
        logger.info("JurassicTale enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        logger.info("Shutting down JurassicTale...");
        
        if (chaosStormManager != null) chaosStormManager.shutdown();
        if (dinoManager != null) dinoManager.shutdown();
        if (compoundManager != null) compoundManager.saveAll();
        
        logger.info("JurassicTale disabled.");
    }
    
    private void loadConfig() {
        this.config = JurassicTaleConfig.load(dataDir.resolve("config.yml"));
        logger.info("Configuration loaded");
    }
    
    private void initializeManagers() {
        this.dinoRegistry = new DinoRegistry(logger);
        this.territoryManager = new TerritoryManager(logger, config);
        this.dinoManager = new DinoManager(logger, dinoRegistry, territoryManager, config);
        this.powerGridManager = new PowerGridManager(logger);
        this.compoundManager = new CompoundManager(logger, powerGridManager, dataDir);
        this.itemRegistry = new ItemRegistry(logger);
        this.captureManager = new CaptureManager(logger, dinoManager);
        this.chaosStormManager = new ChaosStormManager(logger, config, territoryManager);
        
        logger.info("All managers initialized");
    }
    
    private void registerContent() {
        dinoRegistry.registerDefaults();
        itemRegistry.registerDefaults();
        
        logger.info("Content registered: {} dino types, {} items", 
            dinoRegistry.getRegisteredCount(), 
            itemRegistry.getRegisteredCount());
    }
    
    public DinoRegistry getDinoRegistry() { return dinoRegistry; }
    public DinoManager getDinoManager() { return dinoManager; }
    public TerritoryManager getTerritoryManager() { return territoryManager; }
    public CompoundManager getCompoundManager() { return compoundManager; }
    public PowerGridManager getPowerGridManager() { return powerGridManager; }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public CaptureManager getCaptureManager() { return captureManager; }
    public ChaosStormManager getChaosStormManager() { return chaosStormManager; }
    public JurassicTaleConfig getConfig() { return config; }
}
