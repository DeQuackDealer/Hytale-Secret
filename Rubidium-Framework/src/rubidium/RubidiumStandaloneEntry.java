package rubidium;

import rubidium.core.RubidiumBootstrap;
import rubidium.core.tier.FeatureRegistry;
import rubidium.hytale.api.JavaPlugin;

public class RubidiumStandaloneEntry extends JavaPlugin {
    
    private static RubidiumStandaloneEntry instance;
    
    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("[Rubidium] Standalone mode - preparing to initialize...");
    }
    
    @Override
    public void onEnable() {
        RubidiumBootstrap.initialize(getClass(), true);
        getLogger().info("[Rubidium] Standalone mode enabled!");
    }
    
    @Override
    public void onDisable() {
        RubidiumBootstrap.shutdown();
    }
    
    public static RubidiumStandaloneEntry getInstance() {
        return instance;
    }
    
    public static String getRubidiumVersion() {
        return RubidiumBootstrap.getVersion();
    }
    
    public static boolean isRubidiumInitialized() {
        return RubidiumBootstrap.isInitialized();
    }
}
