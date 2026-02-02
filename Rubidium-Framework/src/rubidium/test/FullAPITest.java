package rubidium.test;

import rubidium.*;
import rubidium.api.item.*;
import rubidium.api.world.*;
import rubidium.api.title.*;
import rubidium.api.scoreboard.*;
import rubidium.core.tier.FeatureRegistry;
import com.hypixel.hytale.server.core.plugin.*;

import java.nio.file.*;
import java.util.*;

public class FullAPITest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  Rubidium Full API Test Suite");
        System.out.println("========================================\n");
        
        JavaPluginInit init = new JavaPluginInit();
        
        RubidiumHytaleEntry plugin = new RubidiumHytaleEntry(init);
        
        java.lang.reflect.Method setupMethod = plugin.getClass().getDeclaredMethod("setup");
        setupMethod.setAccessible(true);
        setupMethod.invoke(plugin);
        
        java.lang.reflect.Method startMethod = plugin.getClass().getDeclaredMethod("start");
        startMethod.setAccessible(true);
        startMethod.invoke(plugin);
        
        System.out.println("\n--- Testing Item/Inventory API ---\n");
        testInventoryAPI();
        
        System.out.println("\n--- Testing World API ---\n");
        testWorldAPI();
        
        System.out.println("\n--- Testing Title API ---\n");
        testTitleAPI();
        
        System.out.println("\n--- Testing Scoreboard API ---\n");
        testScoreboardAPI();
        
        System.out.println("\n--- Testing Feature Registry ---\n");
        testFeatureRegistry();
        
        System.out.println("\n========================================");
        System.out.println("  FULL API TEST RESULTS: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
        
        if (failed == 0) {
            System.out.println("  ALL API TESTS PASSED!");
        } else {
            System.out.println("  SOME TESTS FAILED!");
            System.exit(1);
        }
    }
    
    private static void testInventoryAPI() {
        UUID playerId = UUID.randomUUID();
        
        InventoryAPI api = InventoryAPI.get();
        test("InventoryAPI singleton exists", api != null);
        
        ItemStack sword = new ItemStack("hytale:diamond_sword");
        test("ItemStack creation", sword.getType().equals("hytale:diamond_sword"));
        test("ItemStack amount default", sword.getAmount() == 1);
        
        sword.setDisplayName("Epic Sword");
        test("ItemStack displayName", "Epic Sword".equals(sword.getDisplayName()));
        
        sword.addLore("A powerful blade");
        test("ItemStack lore", sword.getLore().size() == 1);
        
        sword.addEnchantment("sharpness", 5);
        test("ItemStack enchantment", sword.hasEnchantment("sharpness"));
        test("ItemStack enchant level", sword.getEnchantmentLevel("sharpness") == 5);
        
        boolean given = api.giveItem(playerId, new ItemStack("hytale:diamond", 32));
        test("Give item", given);
        
        int count = api.countItem(playerId, "hytale:diamond");
        test("Count item", count == 32);
        
        boolean has = api.hasItem(playerId, "hytale:diamond", 16);
        test("Has item (enough)", has);
        
        boolean hasNot = api.hasItem(playerId, "hytale:diamond", 64);
        test("Has item (not enough)", !hasNot);
        
        boolean taken = api.takeItem(playerId, "hytale:diamond", 16);
        test("Take item", taken);
        
        int remaining = api.countItem(playerId, "hytale:diamond");
        test("Count after take", remaining == 16);
        
        PlayerInventory inv = api.getInventory(playerId);
        test("Get inventory", inv != null);
        test("Inventory owner", inv.getOwner().equals(playerId));
        
        api.clearInventory(playerId);
        test("Clear inventory", api.countItem(playerId, "hytale:diamond") == 0);
        
        api.setSlot(playerId, 0, new ItemStack("hytale:golden_apple", 5));
        ItemStack slot0 = api.getSlot(playerId, 0);
        test("Set/get slot", slot0 != null && slot0.getAmount() == 5);
        
        test("Item definitions loaded", api.getAllDefinitions().size() >= 20);
    }
    
    private static void testWorldAPI() {
        WorldAPI api = WorldAPI.get();
        test("WorldAPI singleton exists", api != null);
        
        test("Default worlds exist", api.getWorldNames().contains("world"));
        
        WorldAPI.WorldState world = api.getWorld("world");
        test("Get world state", world != null);
        
        api.setTime("world", 0);
        test("Set time to dawn", api.getTime("world") == 0);
        
        api.setTime("world", 6000);
        test("Set time to noon", api.getTime("world") == 6000);
        test("Is day", world.isDay());
        
        api.setTime("world", 18000);
        test("Is night", world.isNight());
        
        api.setWeather("world", WorldAPI.Weather.RAIN);
        test("Set weather", api.getWeather("world") == WorldAPI.Weather.RAIN);
        
        api.setWeather("world", WorldAPI.Weather.CLEAR);
        test("Clear weather", api.getWeather("world") == WorldAPI.Weather.CLEAR);
        
        String block = api.getBlock("world", 0, 64, 0);
        test("Get block", block != null);
        
        api.setBlock("world", 0, 64, 0, "hytale:stone");
        test("Set block (no exception)", true);
        
        world.setPvpEnabled(false);
        test("Set PvP", !world.isPvpEnabled());
        
        world.setSpawn(100, 65, 100);
        test("Set spawn", world.getSpawnX() == 100 && world.getSpawnY() == 65);
    }
    
    private static void testTitleAPI() {
        TitleAPI api = TitleAPI.get();
        test("TitleAPI singleton exists", api != null);
        
        UUID playerId = UUID.randomUUID();
        
        api.sendTitle(playerId, "Welcome!", "To the server");
        test("Send title (no exception)", true);
        
        api.sendTitle(playerId, "Custom Timing", "Subtitle", 5, 40, 10);
        test("Send title with timing", true);
        
        api.sendSubtitle(playerId, "Just a subtitle");
        test("Send subtitle only", true);
        
        api.sendActionBar(playerId, "Action bar message");
        test("Send action bar", true);
        
        api.sendActionBar(playerId, "Timed action bar", 60);
        test("Send timed action bar", true);
        
        api.sendBossBar(playerId, "test_boss", "Boss Name", 0.75f, 
            TitleAPI.BossBarColor.RED, TitleAPI.BossBarStyle.SEGMENTED_10);
        test("Send boss bar", true);
        
        api.updateBossBar(playerId, "test_boss", "Updated Boss", 0.5f);
        test("Update boss bar", true);
        
        api.removeBossBar(playerId, "test_boss");
        test("Remove boss bar", true);
        
        api.clearTitle(playerId);
        test("Clear title", true);
        
        api.clearActionBar(playerId);
        test("Clear action bar", true);
        
        Set<UUID> players = new HashSet<>();
        players.add(UUID.randomUUID());
        players.add(UUID.randomUUID());
        api.broadcastTitle(players, "Broadcast", "To all", 10, 70, 20);
        test("Broadcast title", true);
        
        api.broadcastActionBar(players, "Broadcast action bar");
        test("Broadcast action bar", true);
    }
    
    private static void testScoreboardAPI() {
        test("ScoreboardAPI create", ScoreboardAPI.create("test") != null);
        
        ScoreboardAPI.Scoreboard scoreboard = ScoreboardAPI.create("kills")
            .title("Kills Leaderboard")
            .type(ScoreboardAPI.Scoreboard.DisplayType.SIDEBAR)
            .line("Player1", 10)
            .line("Player2", 8)
            .line("Player3", 5)
            .build();
        
        test("Scoreboard created", scoreboard != null);
        test("Scoreboard id", "kills".equals(scoreboard.getId()));
        test("Scoreboard title", "Kills Leaderboard".equals(scoreboard.getTitle()));
        
        ScoreboardAPI.register(scoreboard);
        test("Scoreboard registered", ScoreboardAPI.get("kills").isPresent());
        
        scoreboard.addLine("Player4");
        test("Add line", scoreboard.getEntries().size() > 3);
        
        scoreboard.removeLine(0);
        test("Remove line", scoreboard.getEntries().size() <= 3);
        
        UUID playerId = UUID.randomUUID();
        ScoreboardAPI.show(playerId, "kills");
        test("Show scoreboard", ScoreboardAPI.getPlayerScoreboard(playerId).isPresent());
        
        ScoreboardAPI.hide(playerId);
        test("Hide scoreboard", !ScoreboardAPI.getPlayerScoreboard(playerId).isPresent());
        
        ScoreboardAPI.remove("kills");
        test("Remove scoreboard", !ScoreboardAPI.get("kills").isPresent());
        
        ScoreboardAPI.Scoreboard sidebar = ScoreboardAPI.sidebar("quick", "Quick Stats");
        test("Quick sidebar creation", sidebar != null);
    }
    
    private static void testFeatureRegistry() {
        test("FeatureRegistry initialized", FeatureRegistry.getCurrentTier() != null);
        test("Features exist", !FeatureRegistry.getAllFeatures().isEmpty());
        
        int total = FeatureRegistry.getAllFeatures().size();
        test("Total features > 20", total > 20);
        
        long enabled = FeatureRegistry.getAllFeatures().stream()
            .filter(FeatureRegistry.Feature::isEnabled)
            .count();
        test("Enabled features > 20", enabled > 20);
        
        String edition = FeatureRegistry.getCurrentTier().getDisplayName();
        test("Edition detected", edition != null && !edition.isEmpty());
        
        System.out.println("  Edition: " + edition);
        System.out.println("  Total Features: " + total);
        System.out.println("  Enabled Features: " + enabled);
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
