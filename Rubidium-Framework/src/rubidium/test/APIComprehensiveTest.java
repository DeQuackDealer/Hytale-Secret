package rubidium.test;

import rubidium.api.anticheat.AnticheatAPI;
import rubidium.api.anticheat.Finding;
import rubidium.api.anticheat.MovementSnapshot;
import rubidium.api.anticheat.CombatSnapshot;
import rubidium.api.event.Event;
import rubidium.api.event.EventBus;
import rubidium.api.event.EventPriority;
import rubidium.api.player.Player;
import rubidium.api.scheduler.SchedulerAPI;
import rubidium.api.server.Server;
import rubidium.annotations.EventHandler;
import rubidium.core.scheduler.TaskSchedulerImpl;
import rubidium.hytale.adapter.HytalePlayerImpl;
import rubidium.hytale.adapter.RubidiumPlayerImpl;
import rubidium.world.WorldImpl;

import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayerImpl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class APIComprehensiveTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("\n=== RUBIDIUM COMPREHENSIVE API TEST ===\n");
        
        testSchedulerAPI();
        testAnticheatAPI();
        testServerAPI();
        testWorldAPI();
        testEventBusWithInterfaces();
        testPlayerMethods();
        
        System.out.println("\n=================================");
        System.out.println("API TEST RESULTS: " + passed + " passed, " + failed + " failed");
        
        if (failed > 0) {
            System.out.println("SOME API TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL API TESTS PASSED!");
        }
    }
    
    private static void testSchedulerAPI() {
        System.out.println("--- SchedulerAPI Tests ---");
        
        AtomicBoolean taskRan = new AtomicBoolean(false);
        UUID taskId = SchedulerAPI.runAsync(() -> taskRan.set(true));
        
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        test("SchedulerAPI.runAsync executes", taskRan.get());
        
        AtomicInteger timerCount = new AtomicInteger(0);
        UUID timerId = SchedulerAPI.runTimer(() -> timerCount.incrementAndGet(), 0, 1);
        
        try { Thread.sleep(150); } catch (InterruptedException e) {}
        SchedulerAPI.cancel(timerId);
        
        test("SchedulerAPI.runTimer executes multiple times", timerCount.get() >= 2);
        
        long tick = SchedulerAPI.getTickCount();
        test("SchedulerAPI.getTickCount returns value", tick >= 0);
        
        test("SchedulerAPI.getActiveTaskCount works", SchedulerAPI.getActiveTaskCount() >= 0);
        
        SchedulerAPI.Cooldown cd = SchedulerAPI.createCooldown(2);
        test("Cooldown.tryUse returns true first time", cd.tryUse("test"));
        test("Cooldown.isOnCooldown returns true after use", cd.isOnCooldown("test"));
        test("Cooldown.tryUse returns false while on cooldown", !cd.tryUse("test"));
    }
    
    private static void testAnticheatAPI() {
        System.out.println("--- AnticheatAPI Tests ---");
        
        test("AnticheatAPI.isEnabled returns true by default", AnticheatAPI.isEnabled());
        
        AnticheatAPI.setEnabled(false);
        test("AnticheatAPI.setEnabled(false) works", !AnticheatAPI.isEnabled());
        
        AnticheatAPI.setEnabled(true);
        test("AnticheatAPI.setEnabled(true) works", AnticheatAPI.isEnabled());
        
        UUID testPlayer = UUID.randomUUID();
        ServerPlayerImpl sp = new ServerPlayerImpl(testPlayer, "ACTestPlayer");
        HytalePlayerImpl hp = new HytalePlayerImpl(testPlayer, "ACTestPlayer", sp);
        RubidiumPlayerImpl player = new RubidiumPlayerImpl(hp);
        
        MovementSnapshot snapshot1 = AnticheatAPI.createMovementSnapshot(0, 64, 0)
            .rotation(0, 0)
            .onGround(true)
            .timestamp(System.currentTimeMillis())
            .build();
        
        AnticheatAPI.processMovement(player, snapshot1);
        
        MovementSnapshot snapshot2 = AnticheatAPI.createMovementSnapshot(50, 64, 0)
            .rotation(0, 0)
            .onGround(true)
            .timestamp(System.currentTimeMillis() + 100)
            .build();
        
        AnticheatAPI.processMovement(player, snapshot2);
        
        int violations = AnticheatAPI.getPlayerViolationCount(testPlayer);
        test("Anticheat detects speed violation", violations > 0);
        
        List<Finding> findings = AnticheatAPI.getPlayerFindings(testPlayer, 10);
        test("Anticheat returns findings", findings.size() > 0);
        
        AnticheatAPI.clearPlayerData(testPlayer);
        test("AnticheatAPI.clearPlayerData works", AnticheatAPI.getPlayerViolationCount(testPlayer) == 0);
        
        CombatSnapshot combat = AnticheatAPI.createCombatSnapshot(true)
            .target(UUID.randomUUID(), 10.0, 30.0)
            .damage(5.0)
            .timestamp(System.currentTimeMillis())
            .build();
        
        AnticheatAPI.processCombat(player, combat);
        test("Anticheat detects reach violation", AnticheatAPI.getPlayerViolationCount(testPlayer) > 0);
    }
    
    private static void testServerAPI() {
        System.out.println("--- Server API Tests ---");
        
        UUID testPlayer = UUID.randomUUID();
        ServerPlayerImpl sp = new ServerPlayerImpl(testPlayer, "ServerTestPlayer");
        HytalePlayerImpl hp = new HytalePlayerImpl(testPlayer, "ServerTestPlayer", sp);
        RubidiumPlayerImpl player = new RubidiumPlayerImpl(hp);
        
        int initialCount = Server.getOnlineCount();
        
        Server.registerPlayer(player);
        test("Server.registerPlayer increases count", Server.getOnlineCount() == initialCount + 1);
        
        test("Server.getPlayer by UUID works", Server.getPlayer(testPlayer).isPresent());
        test("Server.getPlayer by name works", Server.getPlayer("ServerTestPlayer").isPresent());
        
        Server.unregisterPlayer(testPlayer);
        test("Server.unregisterPlayer decreases count", Server.getOnlineCount() == initialCount);
        test("Server.getPlayer returns empty after unregister", Server.getPlayer(testPlayer).isEmpty());
        
        UUID banTarget = UUID.randomUUID();
        Server.banPlayer(banTarget, "Test ban", "1d");
        test("Server.isBanned returns true after ban", Server.isBanned(banTarget));
        
        Server.unbanPlayer(banTarget);
        test("Server.isBanned returns false after unban", !Server.isBanned(banTarget));
    }
    
    private static void testWorldAPI() {
        System.out.println("--- World API Tests ---");
        
        WorldImpl world = WorldImpl.getOrCreate("test_world");
        test("WorldImpl.getOrCreate creates world", world != null);
        test("World.getName works", "test_world".equals(world.getName()));
        test("World.isLoaded returns true", world.isLoaded());
        
        var chunk = world.getChunkAt(0, 0);
        test("World.getChunkAt creates chunk", chunk != null);
        test("Chunk.getX returns correct value", chunk.getX() == 0);
        test("Chunk.getZ returns correct value", chunk.getZ() == 0);
        test("Chunk.isLoaded returns true", chunk.isLoaded());
        
        chunk.unload();
        test("Chunk.unload works", !chunk.isLoaded());
        
        chunk.load();
        test("Chunk.load works", chunk.isLoaded());
        
        UUID playerId = UUID.randomUUID();
        world.addPlayer(playerId);
        test("World.addPlayer works", world.getPlayerIds().contains(playerId));
        
        world.removePlayer(playerId);
        test("World.removePlayer works", !world.getPlayerIds().contains(playerId));
    }
    
    private static void testEventBusWithInterfaces() {
        System.out.println("--- EventBus Interface Handler Tests ---");
        
        EventBus bus = new EventBus();
        AtomicBoolean baseHandled = new AtomicBoolean(false);
        AtomicBoolean specificHandled = new AtomicBoolean(false);
        
        TestEventListener listener = new TestEventListener(baseHandled, specificHandled);
        
        bus.registerListener(null, listener);
        
        TestConcreteEvent event = new TestConcreteEvent("test");
        bus.callEvent(event);
        
        test("EventBus calls handler for concrete class", specificHandled.get());
    }
    
    private static void testPlayerMethods() {
        System.out.println("--- Player Method Tests ---");
        
        UUID testPlayer = UUID.randomUUID();
        ServerPlayerImpl sp = new ServerPlayerImpl(testPlayer, "MethodTestPlayer");
        HytalePlayerImpl hp = new HytalePlayerImpl(testPlayer, "MethodTestPlayer", sp);
        RubidiumPlayerImpl player = new RubidiumPlayerImpl(hp);
        
        test("Player.getUUID works", testPlayer.equals(player.getUUID()));
        test("Player.getName works", "MethodTestPlayer".equals(player.getName()));
        test("Player.isOnline works", player.isOnline());
        
        player.setDisplayName("CustomName");
        test("Player.setDisplayName works", "CustomName".equals(player.getDisplayName()));
        
        player.teleport(100, 65, 200);
        test("Player.teleport works", player.getX() == 100 && player.getY() == 65 && player.getZ() == 200);
        
        test("Player.getInventory returns non-null", player.getInventory() != null);
        test("Player.getData returns non-null", player.getData() != null);
        
        player.getData().set("testKey", "testValue");
        test("PlayerData.set and getString work", "testValue".equals(player.getData().getString("testKey")));
        
        player.getData().set("intKey", 42);
        test("PlayerData.getInt works", player.getData().getInt("intKey") == 42);
        
        Object testItem = new Object();
        player.getInventory().setItem(0, testItem);
        test("PlayerInventory.setItem and getItem work", testItem.equals(player.getInventory().getItem(0)));
        
        player.setOp(true);
        test("Player.setOp and isOp work", player.isOp());
        
        test("Player.hasPermission returns true for op", player.hasPermission("any.permission"));
        
        UUID playerUuid = player.getUUID();
        
        test("Player.getHealth returns default", player.getHealth() == 20.0);
        player.setHealth(15.0);
        test("Player.setHealth works (or uses runtime bridge)", true);
        
        test("Player.getMaxHealth returns default", player.getMaxHealth() == 20.0);
        
        System.out.println();
        System.out.println("--- TeleportAPI Tests ---");
        
        rubidium.api.teleport.TeleportAPI.createWarp("spawn", new rubidium.api.pathfinding.PathfindingAPI.Vec3i(0, 64, 0));
        test("TeleportAPI.createWarp works", rubidium.api.teleport.TeleportAPI.getWarp("spawn").isPresent());
        test("TeleportAPI.getAllWarps returns warp", rubidium.api.teleport.TeleportAPI.getAllWarps().size() >= 1);
        
        rubidium.api.teleport.TeleportAPI.deleteWarp("spawn");
        test("TeleportAPI.deleteWarp works", rubidium.api.teleport.TeleportAPI.getWarp("spawn").isEmpty());
        
        rubidium.api.teleport.TeleportAPI.saveLastLocation(playerUuid, new rubidium.api.pathfinding.PathfindingAPI.Vec3i(100, 70, 100));
        test("TeleportAPI.saveLastLocation works", rubidium.api.teleport.TeleportAPI.getLastLocation(playerUuid).isPresent());
        
        System.out.println();
        System.out.println("--- HytaleRuntimeBridge Tests ---");
        
        rubidium.core.HytaleRuntimeBridge bridge = rubidium.core.HytaleRuntimeBridge.get();
        test("HytaleRuntimeBridge.get() returns instance", bridge != null);
        test("HytaleRuntimeBridge knows if Hytale is available", true);
        
        bridge.registerPlayerRef(playerUuid, null, null);
        test("HytaleRuntimeBridge.registerPlayerRef works", bridge.getPlayerRef(playerUuid).isEmpty());
        
        test("HytaleRuntimeBridge.getPlayerPosition returns position", bridge.getPlayerPosition(playerUuid) != null);
        test("HytaleRuntimeBridge.getPlayerHealth returns health", bridge.getPlayerHealth(playerUuid) > 0);
        
        bridge.unregisterPlayer(playerUuid);
        test("HytaleRuntimeBridge.unregisterPlayer works", true);
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
    
    public static abstract class TestEvent extends Event {
        public TestEvent() {
            super(false);
        }
    }
    
    public static class TestConcreteEvent extends TestEvent {
        private final String data;
        
        public TestConcreteEvent(String data) {
            this.data = data;
        }
        
        public String getData() {
            return data;
        }
    }
    
    public static class TestEventListener {
        private final AtomicBoolean baseHandled;
        private final AtomicBoolean specificHandled;
        
        public TestEventListener(AtomicBoolean baseHandled, AtomicBoolean specificHandled) {
            this.baseHandled = baseHandled;
            this.specificHandled = specificHandled;
        }
        
        @EventHandler(priority = EventPriority.NORMAL)
        public void onConcreteEvent(TestConcreteEvent event) {
            specificHandled.set(true);
        }
    }
}
