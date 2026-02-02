package rubidium.test;

import com.hypixel.hytale.server.core.command.*;
import com.hypixel.hytale.server.core.plugin.*;
import rubidium.*;
import rubidium.core.tier.FeatureRegistry;
import java.nio.file.*;
import java.util.*;

public class CommandTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  Rubidium Command & Feature Test");
        System.out.println("========================================\n");
        
        // Initialize framework
        JavaPluginInit init = new JavaPluginInit();
        
        RubidiumHytaleEntry plugin = new RubidiumHytaleEntry(init);
        
        // Full lifecycle
        java.lang.reflect.Method setupMethod = plugin.getClass().getDeclaredMethod("setup");
        setupMethod.setAccessible(true);
        setupMethod.invoke(plugin);
        
        java.lang.reflect.Method startMethod = plugin.getClass().getDeclaredMethod("start");
        startMethod.setAccessible(true);
        startMethod.invoke(plugin);
        
        System.out.println("\n--- Testing Commands ---\n");
        
        // Create test sender
        List<String> output = new ArrayList<>();
        CommandSender sender = new CommandSender() {
            public void sendMessage(String msg) {
                output.add(msg);
            }
            public boolean hasPermission(String perm) { return true; }
            public String getName() { return "TestPlayer"; }
            public boolean isPlayer() { return true; }
            public boolean isConsole() { return false; }
            public UUID getUniqueId() { return UUID.randomUUID(); }
        };
        
        // Test /rubidium
        output.clear();
        boolean result = CommandManager.get().handleCommand(sender, "/rubidium");
        test("/rubidium executes", result);
        test("/rubidium outputs framework info", output.stream().anyMatch(s -> s.contains("Rubidium Framework")));
        test("/rubidium shows edition", output.stream().anyMatch(s -> s.contains("Edition")));
        test("/rubidium shows features", output.stream().anyMatch(s -> s.contains("Features")));
        
        // Test /toggle
        output.clear();
        result = CommandManager.get().handleCommand(sender, "/toggle");
        test("/toggle executes without args", result);
        test("/toggle shows usage", output.stream().anyMatch(s -> s.contains("Usage") || s.contains("feature")));
        
        // Test /toggle minimap
        output.clear();
        result = CommandManager.get().handleCommand(sender, "/toggle minimap");
        test("/toggle minimap executes", result);
        test("/toggle minimap shows state change", output.stream().anyMatch(s -> s.contains("Minimap") || s.contains("map")));
        
        // Test /hud
        output.clear();
        result = CommandManager.get().handleCommand(sender, "/hud");
        test("/hud executes", result);
        
        // Test /waypoint
        output.clear();
        result = CommandManager.get().handleCommand(sender, "/waypoint home");
        test("/waypoint home executes", result);
        
        // Test /admin
        output.clear();
        result = CommandManager.get().handleCommand(sender, "/admin");
        test("/admin executes", result);
        
        System.out.println("\n--- Testing Features ---\n");
        
        test("FeatureRegistry initialized", FeatureRegistry.getCurrentTier() != null);
        test("Edition detected", !FeatureRegistry.getCurrentTier().getDisplayName().isEmpty());
        test("Features registered", FeatureRegistry.getAllFeatures().size() > 0);
        test("Features enabled", FeatureRegistry.getAllFeatures().stream()
            .filter(FeatureRegistry.Feature::isEnabled).count() > 0);
        
        int totalFeatures = FeatureRegistry.getAllFeatures().size();
        long enabledFeatures = FeatureRegistry.getAllFeatures().stream()
            .filter(FeatureRegistry.Feature::isEnabled).count();
        
        System.out.println("\n--- Feature Summary ---");
        System.out.println("Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
        System.out.println("Total Features: " + totalFeatures);
        System.out.println("Enabled Features: " + enabledFeatures);
        
        System.out.println("\n========================================");
        System.out.println("  TEST RESULTS: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
        
        if (failed == 0) {
            System.out.println("  ALL COMMAND TESTS PASSED!");
        } else {
            System.out.println("  SOME TESTS FAILED!");
            System.exit(1);
        }
    }
    
    private static void test(String name, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + name);
            passed++;
        } else {
            System.out.println("  [FAIL] " + name);
            failed++;
        }
    }
}
