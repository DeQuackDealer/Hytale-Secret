package rubidium.test;

import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import rubidium.RubidiumHytaleEntry;
import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RubidiumTestHarness {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Rubidium Framework Test Harness");
        System.out.println("========================================");
        System.out.println();
        
        try {
            System.out.println("[Test] Creating JavaPluginInit...");
            JavaPluginInit init = new JavaPluginInit() {
                @Override
                public Path getDataFolder() {
                    return Paths.get("plugins", "Rubidium");
                }
                
                @Override
                public String getName() {
                    return "Rubidium";
                }
                
                @Override
                public String getVersion() {
                    return "1.0.0";
                }
            };
            
            System.out.println("[Test] Creating RubidiumHytaleEntry...");
            RubidiumHytaleEntry plugin = new RubidiumHytaleEntry(init);
            
            System.out.println("[Test] Calling onEnable()...");
            System.out.println();
            plugin.onEnable();
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("  Test Results");
            System.out.println("========================================");
            
            ProductTier tier = FeatureRegistry.getCurrentTier();
            System.out.println("Current Tier: " + tier.getDisplayName());
            System.out.println("Is Premium: " + tier.isPremium());
            
            System.out.println();
            System.out.println("Enabled Features:");
            FeatureRegistry.getAllFeatures().stream()
                .filter(FeatureRegistry.Feature::isEnabled)
                .forEach(f -> System.out.println("  + " + f.name() + " (" + f.id() + ")"));
            
            System.out.println();
            System.out.println("Disabled Features:");
            FeatureRegistry.getAllFeatures().stream()
                .filter(f -> !f.isEnabled())
                .forEach(f -> System.out.println("  - " + f.name() + " (" + f.id() + ") [Requires: " + f.tier().getDisplayName() + "]"));
            
            System.out.println();
            System.out.println("[Test] Plugin initialized: " + RubidiumHytaleEntry.isInitialized());
            System.out.println("[Test] Version: " + RubidiumHytaleEntry.getVersion());
            
            System.out.println();
            System.out.println("[Test] Calling onDisable()...");
            plugin.onDisable();
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("  TEST PASSED - Plugin loads correctly!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println();
            System.err.println("========================================");
            System.err.println("  TEST FAILED!");
            System.err.println("========================================");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
