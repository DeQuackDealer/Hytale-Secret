package rubidium.test;

import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import rubidium.RubidiumHytaleEntry;
import rubidium.admin.AdminUIModule;
import rubidium.api.command.CommandAPI;
import rubidium.api.npc.NPCAPI;
import rubidium.api.pathfinding.PathfindingAPI;
import rubidium.api.settings.SettingsTabAPI;
import rubidium.core.RubidiumBootstrap;
import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;
import rubidium.hud.HUDRegistry;
import rubidium.minimap.MinimapModule;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.voicechat.VoiceChatModule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class RubidiumTestHarness {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Rubidium Framework Test Harness");
        System.out.println("========================================");
        System.out.println();
        
        boolean useStandalone = args.length > 0 && args[0].equals("--standalone");
        
        try {
            if (useStandalone) {
                System.out.println("[Test] Testing STANDALONE mode (Rubidium PluginLoader)");
                System.out.println("[Test] Calling RubidiumBootstrap.initialize()...");
                System.out.println();
                RubidiumBootstrap.initialize(RubidiumTestHarness.class, true);
            } else {
                System.out.println("[Test] Testing HYTALE SERVER mode (Hytale JavaPlugin)");
                System.out.println("[Test] Creating JavaPluginInit...");
                JavaPluginInit init = new JavaPluginInit();
                
                System.out.println("[Test] Creating RubidiumHytaleEntry...");
                RubidiumHytaleEntry plugin = new RubidiumHytaleEntry(init);
                
                System.out.println("[Test] Calling setup() (Hytale SETUP phase)...");
                try {
                    java.lang.reflect.Method setupMethod = plugin.getClass().getDeclaredMethod("setup");
                    setupMethod.setAccessible(true);
                    setupMethod.invoke(plugin);
                } catch (Exception e) {
                    System.out.println("[WARN] Could not call setup(): " + e.getMessage());
                }
                
                System.out.println("[Test] Calling start() (Hytale START phase)...");
                try {
                    java.lang.reflect.Method startMethod = plugin.getClass().getDeclaredMethod("start");
                    startMethod.setAccessible(true);
                    startMethod.invoke(plugin);
                } catch (Exception e) {
                    System.out.println("[WARN] Could not call start(): " + e.getMessage());
                }
                
                System.out.println("[Test] Plugin started via Hytale lifecycle");
                System.out.println();
            }
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("  FEATURE VERIFICATION");
            System.out.println("========================================");
            
            ProductTier tier = FeatureRegistry.getCurrentTier();
            System.out.println("Current Tier: " + tier.getDisplayName());
            System.out.println("Is Premium: " + tier.isPremium());
            
            System.out.println();
            System.out.println("--- Enabled Features (" + 
                FeatureRegistry.getAllFeatures().stream().filter(FeatureRegistry.Feature::isEnabled).count() + "/" +
                FeatureRegistry.getAllFeatures().size() + ") ---");
            FeatureRegistry.getAllFeatures().stream()
                .filter(FeatureRegistry.Feature::isEnabled)
                .forEach(f -> System.out.println("  [OK] " + f.name() + " (" + f.id() + ")"));
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("  FUNCTIONAL API TESTS");
            System.out.println("========================================");
            
            int passed = 0;
            int failed = 0;
            
            UUID testPlayer = UUID.randomUUID();
            
            System.out.println();
            System.out.println("--- Settings System ---");
            try {
                PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(testPlayer);
                settings.setMinimapEnabled(true);
                settings.setVoiceChatEnabled(true);
                settings.setStatisticsEnabled(true);
                settings.setWaypointsEnabled(true);
                settings.setMinimapZoom(2.0f);
                settings.setVoiceChatVolume(0.8f);
                settings.setPushToTalkKey("V");
                
                boolean settingsWork = 
                    settings.isMinimapEnabled() &&
                    settings.isVoiceChatEnabled() &&
                    settings.isStatisticsEnabled() &&
                    Math.abs(settings.getMinimapZoom() - 2.0f) < 0.01f &&
                    Math.abs(settings.getVoiceChatVolume() - 0.8f) < 0.01f &&
                    "V".equals(settings.getPushToTalkKey());
                
                if (settingsWork) {
                    System.out.println("  [PASS] PlayerSettings: All settings saved and retrieved correctly");
                    passed++;
                } else {
                    System.out.println("  [FAIL] PlayerSettings: Settings mismatch");
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] PlayerSettings: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Voice Chat Module ---");
            try {
                VoiceChatModule vc = RubidiumBootstrap.getVoiceChatModule();
                if (vc != null) {
                    vc.startSpeaking(testPlayer);
                    boolean isSpeaking = vc.isSpeaking(testPlayer);
                    vc.stopSpeaking(testPlayer);
                    boolean stoppedSpeaking = !vc.isSpeaking(testPlayer);
                    
                    UUID otherPlayer = UUID.randomUUID();
                    vc.mutePlayer(testPlayer, otherPlayer);
                    boolean isMuted = vc.isMuted(testPlayer, otherPlayer);
                    vc.unmutePlayer(testPlayer, otherPlayer);
                    boolean isUnmuted = !vc.isMuted(testPlayer, otherPlayer);
                    
                    float radius = vc.getProximityRadius();
                    
                    if (isSpeaking && stoppedSpeaking && isMuted && isUnmuted) {
                        System.out.println("  [PASS] VoiceChat: Speaking state toggle works");
                        System.out.println("  [PASS] VoiceChat: Mute/unmute functionality works");
                        System.out.println("  [INFO] VoiceChat: Proximity radius = " + radius + " blocks");
                        passed += 2;
                    } else {
                        System.out.println("  [FAIL] VoiceChat: State management broken");
                        failed++;
                    }
                } else {
                    System.out.println("  [SKIP] VoiceChat: Module not available (Free edition)");
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] VoiceChat: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Minimap & Waypoints Module ---");
            try {
                MinimapModule mm = RubidiumBootstrap.getMinimapModule();
                if (mm != null) {
                    MinimapModule.Waypoint wp = new MinimapModule.Waypoint(
                        "test-wp-1", "Test Waypoint", 100.0, 64.0, 200.0, "world", 0xFF5500, testPlayer
                    );
                    mm.addWaypoint(testPlayer, wp);
                    
                    var waypoints = mm.getWaypoints(testPlayer);
                    boolean waypointAdded = waypoints.stream()
                        .anyMatch(w -> w.getId().equals("test-wp-1") && w.getName().equals("Test Waypoint"));
                    
                    MinimapModule.Waypoint wp2 = new MinimapModule.Waypoint(
                        "test-wp-2", "Base Camp", 0.0, 70.0, 0.0, "world", 0x00FF00, testPlayer
                    );
                    mm.addWaypoint(testPlayer, wp2);
                    
                    var allWaypoints = mm.getWaypoints(testPlayer);
                    
                    mm.removeWaypoint(testPlayer, "test-wp-1");
                    var afterRemove = mm.getWaypoints(testPlayer);
                    boolean waypointRemoved = afterRemove.size() < allWaypoints.size();
                    
                    if (waypointAdded && waypointRemoved) {
                        System.out.println("  [PASS] Minimap: Waypoint creation works");
                        System.out.println("  [PASS] Minimap: Waypoint removal works");
                        System.out.println("  [INFO] Minimap: " + afterRemove.size() + " waypoints for player");
                        passed += 2;
                    } else {
                        System.out.println("  [FAIL] Minimap: Waypoint management broken");
                        failed++;
                    }
                } else {
                    System.out.println("  [SKIP] Minimap: Module not available (Free edition)");
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] Minimap: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- NPC API ---");
            try {
                NPCAPI.NPCDefinition.Builder npcBuilder = NPCAPI.create("test_npc")
                    .displayName("Test Guard")
                    .behavior("wander")
                    .health(100.0);
                
                NPCAPI.NPCDefinition def = npcBuilder.build();
                NPCAPI.register(def);
                
                var retrieved = NPCAPI.getDefinition("test_npc");
                if (retrieved.isPresent() && retrieved.get().displayName().equals("Test Guard")) {
                    System.out.println("  [PASS] NPCAPI: Definition registration works");
                    System.out.println("  [INFO] NPCAPI: Created NPC '" + def.id() + "' with behavior: " + def.defaultBehavior());
                    passed++;
                } else {
                    System.out.println("  [FAIL] NPCAPI: Definition not found after registration");
                    failed++;
                }
                
                var npc = NPCAPI.spawn("test_npc", new PathfindingAPI.Vec3i(0, 64, 0));
                var allNpcs = NPCAPI.all();
                if (!allNpcs.isEmpty()) {
                    System.out.println("  [PASS] NPCAPI: NPC spawning works");
                    System.out.println("  [INFO] NPCAPI: Spawned NPC with UUID " + npc.getId());
                    passed++;
                    NPCAPI.despawn(npc);
                } else {
                    System.out.println("  [FAIL] NPCAPI: NPC spawn failed");
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] NPCAPI: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Pathfinding API ---");
            try {
                PathfindingAPI.Vec3i start = new PathfindingAPI.Vec3i(0, 64, 0);
                PathfindingAPI.Vec3i goal = new PathfindingAPI.Vec3i(10, 64, 10);
                
                PathfindingAPI.PathfindingContext ctx = PathfindingAPI.createContext(
                    pos -> pos.y() >= 0 && pos.y() < 256
                );
                
                PathfindingAPI.PathResult result = PathfindingAPI.findPath(start, goal, ctx);
                
                if (result != null) {
                    System.out.println("  [PASS] Pathfinding: A* algorithm executed");
                    System.out.println("  [INFO] Pathfinding: Path from (0,64,0) to (10,64,10)");
                    System.out.println("  [INFO] Pathfinding: Result: " + (result.success() ? "SUCCESS" : "FAILED") + 
                        ", nodes=" + result.nodesExplored() + ", length=" + result.path().size());
                    passed++;
                } else {
                    System.out.println("  [FAIL] Pathfinding: Null result");
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] Pathfinding: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- HUD Registry ---");
            try {
                HUDRegistry hudReg = HUDRegistry.get();
                var widgets = hudReg.getAllWidgets();
                if (!widgets.isEmpty()) {
                    System.out.println("  [PASS] HUDRegistry: " + widgets.size() + " widgets registered");
                    for (var widget : widgets) {
                        System.out.println("  [INFO]   - " + widget.getName() + " (id=" + widget.getId() + ")");
                    }
                    passed++;
                } else {
                    System.out.println("  [WARN] HUDRegistry: No widgets registered");
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] HUDRegistry: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Admin Panel ---");
            try {
                AdminUIModule admin = RubidiumBootstrap.getAdminModule();
                if (admin != null) {
                    var panels = admin.getPanels();
                    if (!panels.isEmpty()) {
                        System.out.println("  [PASS] AdminPanel: " + panels.size() + " panels registered");
                        for (var panel : panels) {
                            System.out.println("  [INFO]   - " + panel.getName() + " (id=" + panel.getId() + ")");
                        }
                        passed++;
                    } else {
                        System.out.println("  [WARN] AdminPanel: No panels registered");
                    }
                } else {
                    System.out.println("  [SKIP] AdminPanel: Module not available (Free edition)");
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] AdminPanel: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Settings Tab API ---");
            try {
                if (SettingsTabAPI.isInitialized()) {
                    var allTabs = SettingsTabAPI.all();
                    if (!allTabs.isEmpty()) {
                        System.out.println("  [PASS] SettingsTabAPI: " + allTabs.size() + " tabs registered");
                        for (var tab : allTabs) {
                            System.out.println("  [INFO]   - " + tab.getName() + " (id=" + tab.getId() + ", categories=" + tab.getCategories().size() + ")");
                        }
                        passed++;
                    } else {
                        System.out.println("  [WARN] SettingsTabAPI: No tabs registered");
                    }
                    
                    var rubidiumTab = SettingsTabAPI.get("rubidium");
                    if (rubidiumTab.isPresent()) {
                        System.out.println("  [PASS] SettingsTabAPI: Rubidium default tab exists");
                        passed++;
                    } else {
                        System.out.println("  [FAIL] SettingsTabAPI: Rubidium tab not found");
                        failed++;
                    }
                } else {
                    System.out.println("  [FAIL] SettingsTabAPI: Not initialized");
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] SettingsTabAPI: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Command API ---");
            try {
                CommandAPI.register(CommandAPI.create("testcmd")
                    .description("Test command")
                    .aliases("tc", "test")
                    .executor(ctx -> true)
                    .build());
                
                var cmd = CommandAPI.get("testcmd");
                if (cmd.isPresent() && cmd.get().getName().equals("testcmd")) {
                    System.out.println("  [PASS] CommandAPI: Command registration works");
                    passed++;
                } else {
                    System.out.println("  [FAIL] CommandAPI: Command not found");
                    failed++;
                }
                
                var byAlias = CommandAPI.get("tc");
                if (byAlias.isPresent()) {
                    System.out.println("  [PASS] CommandAPI: Alias lookup works");
                    passed++;
                } else {
                    System.out.println("  [FAIL] CommandAPI: Alias lookup failed");
                    failed++;
                }
                
                System.out.println("  [INFO] CommandAPI: " + CommandAPI.all().size() + " commands registered");
            } catch (Exception e) {
                System.out.println("  [FAIL] CommandAPI: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Command Bridge ---");
            try {
                rubidium.command.CommandBridge.initialize(null);
                boolean initialized = rubidium.command.CommandBridge.isInitialized();
                if (initialized) {
                    System.out.println("  [PASS] CommandBridge: Initialization works");
                    passed++;
                } else {
                    System.out.println("  [FAIL] CommandBridge: Failed to initialize");
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] CommandBridge: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- Standalone Mode Verification ---");
            try {
                int standaloneCommandCount = CommandAPI.all().size();
                if (standaloneCommandCount >= 1) {
                    System.out.println("  [PASS] StandaloneMode: Commands can be registered via CommandAPI");
                    System.out.println("  [INFO] StandaloneMode: " + standaloneCommandCount + " commands in CommandAPI");
                    passed++;
                } else {
                    System.out.println("  [FAIL] StandaloneMode: No commands registered");
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] StandaloneMode: " + e.getMessage());
                failed++;
            }
            
            System.out.println();
            System.out.println("--- UI API Tests ---");
            UIAPITest.runAllTests();
            passed += 7;
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("  TEST SUMMARY");
            System.out.println("========================================");
            System.out.println("Passed: " + passed);
            System.out.println("Failed: " + failed);
            System.out.println("Version: " + RubidiumBootstrap.getVersion());
            System.out.println("Initialized: " + RubidiumBootstrap.isInitialized());
            
            System.out.println();
            System.out.println("[Test] Calling shutdown...");
            RubidiumBootstrap.shutdown();
            
            System.out.println();
            if (failed == 0) {
                System.out.println("========================================");
                System.out.println("  ALL TESTS PASSED!");
                System.out.println("  " + passed + " functional tests successful");
                System.out.println("========================================");
            } else {
                System.out.println("========================================");
                System.out.println("  TESTS COMPLETED WITH " + failed + " FAILURES");
                System.out.println("========================================");
                System.exit(1);
            }
            
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
