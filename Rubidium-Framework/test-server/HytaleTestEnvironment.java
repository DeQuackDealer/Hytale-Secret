package rubidium.testserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

import rubidium.api.item.InventoryAPI;
import rubidium.api.item.ItemStack;
import rubidium.api.world.WorldAPI;
import rubidium.api.title.TitleAPI;
import rubidium.api.scoreboard.ScoreboardAPI;
import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;
import rubidium.core.HytaleRuntimeBridge;

public class HytaleTestEnvironment {
    
    private static final Map<UUID, TestPlayer> players = new ConcurrentHashMap<>();
    private static final List<String> eventLog = new CopyOnWriteArrayList<>();
    private static final List<String> serverLog = new CopyOnWriteArrayList<>();
    private static WorldAPI.WorldState worldState;
    private static boolean hytaleServerLoaded = false;
    private static Object hytaleServerInstance = null;
    private static long serverStartTime;
    private static int tickCount = 0;
    private static ScheduledExecutorService tickScheduler;
    
    public static void main(String[] args) throws Exception {
        int port = 5000;
        serverStartTime = System.currentTimeMillis();
        
        printBanner();
        
        initializeHytaleServer();
        initializeRubidium();
        startTickScheduler();
        
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/hytale", new HytaleAPIHandler());
        server.createContext("/api/player", new PlayerAPIHandler());
        server.createContext("/api/inventory", new InventoryAPIHandler());
        server.createContext("/api/world", new WorldAPIHandler());
        server.createContext("/api/title", new TitleAPIHandler());
        server.createContext("/api/scoreboard", new ScoreboardAPIHandler());
        server.createContext("/api/events", new EventsAPIHandler());
        server.createContext("/api/logs", new LogsAPIHandler());
        server.createContext("/api/command", new CommandHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        log("Server", "Test environment running on http://0.0.0.0:" + port);
        log("Server", "Ready for Rubidium API testing!");
    }
    
    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════════════════════╗");
        System.out.println("  ║       RUBIDIUM + HYTALE TEST ENVIRONMENT                  ║");
        System.out.println("  ║         Production-Ready Framework Testing                ║");
        System.out.println("  ╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private static void initializeHytaleServer() {
        log("Hytale", "Attempting to initialize HytaleServer integration...");
        
        try {
            Class<?> hytaleServerClass = Class.forName("com.hypixel.hytale.server.core.HytaleServer");
            log("Hytale", "HytaleServer class found: " + hytaleServerClass.getName());
            
            Method getMethod = null;
            for (Method m : hytaleServerClass.getDeclaredMethods()) {
                if (m.getName().equals("get") && m.getParameterCount() == 0) {
                    getMethod = m;
                    break;
                }
            }
            
            if (getMethod != null) {
                log("Hytale", "Found HytaleServer.get() method");
                hytaleServerLoaded = true;
            }
            
            log("Hytale", "HytaleServer classes available:");
            for (Method m : hytaleServerClass.getDeclaredMethods()) {
                log("Hytale", "  - " + m.getName() + "(" + m.getParameterCount() + " params)");
            }
            
        } catch (ClassNotFoundException e) {
            log("Hytale", "HytaleServer class not found - running in simulation mode");
        } catch (Exception e) {
            log("Hytale", "Error initializing HytaleServer: " + e.getMessage());
        }
        
        if (!hytaleServerLoaded) {
            log("Hytale", "Running in SIMULATION MODE - APIs will work but not connect to real Hytale");
        }
        
        log("Hytale", "HytaleServer integration initialized");
    }
    
    private static void initializeRubidium() {
        log("Rubidium", "Initializing Rubidium Framework...");
        
        FeatureRegistry.initialize(ProductTier.PLUS);
        log("Rubidium", "Feature Registry: " + FeatureRegistry.getAllFeatures().size() + " features loaded");
        log("Rubidium", "Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
        
        log("Rubidium", "Inventory API: " + InventoryAPI.get().getAllDefinitions().size() + " item types");
        
        worldState = WorldAPI.get().getWorld("test_world");
        WorldAPI.get().setTime("test_world", 6000);
        WorldAPI.get().setWeather("test_world", WorldAPI.Weather.CLEAR);
        log("Rubidium", "World API: test_world created");
        
        ScoreboardAPI.Scoreboard sb = ScoreboardAPI.sidebar("server_stats", "Rubidium Server");
        sb.setLine(0, "Players: 0", 0);
        sb.setLine(1, "Uptime: 0s", 1);
        sb.setLine(2, "TPS: 20.0", 2);
        sb.setLine(3, "Memory: 0MB", 3);
        ScoreboardAPI.register(sb);
        log("Rubidium", "Scoreboard API: initialized");
        
        createTestPlayer("Steve", 100, 64, 100);
        createTestPlayer("Alex", -50, 64, 50);
        createTestPlayer("Notch", 0, 70, 0);
        log("Rubidium", "Created 3 test players");
        
        HytaleRuntimeBridge bridge = HytaleRuntimeBridge.get();
        log("Rubidium", "Runtime Bridge: " + (bridge.isHytaleAvailable() ? "CONNECTED" : "SIMULATION"));
        
        log("Rubidium", "Framework initialization complete!");
    }
    
    private static void startTickScheduler() {
        tickScheduler = Executors.newSingleThreadScheduledExecutor();
        tickScheduler.scheduleAtFixedRate(() -> {
            tickCount++;
            
            if (tickCount % 20 == 0) {
                updateScoreboard();
            }
            
            if (tickCount % 100 == 0) {
                long time = WorldAPI.get().getTime("test_world");
                WorldAPI.get().setTime("test_world", (time + 1) % 24000);
            }
            
        }, 50, 50, TimeUnit.MILLISECONDS);
        
        log("Server", "Tick scheduler started (20 TPS)");
    }
    
    private static void updateScoreboard() {
        Optional<ScoreboardAPI.Scoreboard> sb = ScoreboardAPI.get("server_stats");
        if (sb.isPresent()) {
            long uptime = (System.currentTimeMillis() - serverStartTime) / 1000;
            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            
            sb.get().setLine(0, "Players: " + players.size(), 0);
            sb.get().setLine(1, "Uptime: " + uptime + "s", 1);
            sb.get().setLine(2, "Ticks: " + tickCount, 2);
            sb.get().setLine(3, "Memory: " + usedMB + "MB", 3);
        }
    }
    
    private static TestPlayer createTestPlayer(String name, double x, double y, double z) {
        UUID id = UUID.randomUUID();
        TestPlayer player = new TestPlayer(id, name, x, y, z);
        players.put(id, player);
        
        rubidium.api.item.PlayerInventory inv = InventoryAPI.get().getInventory(id);
        inv.addItem(new ItemStack("hytale:iron_sword", 1));
        inv.addItem(new ItemStack("hytale:bread", 16));
        inv.addItem(new ItemStack("hytale:torch", 32));
        
        logEvent("Player '" + name + "' joined at (" + (int)x + ", " + (int)y + ", " + (int)z + ")");
        return player;
    }
    
    private static void log(String category, String message) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        String entry = "[" + timestamp + "] [" + category + "] " + message;
        serverLog.add(0, entry);
        if (serverLog.size() > 100) serverLog.remove(serverLog.size() - 1);
        System.out.println(entry);
    }
    
    private static void logEvent(String event) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        String entry = "[" + timestamp + "] " + event;
        eventLog.add(0, entry);
        if (eventLog.size() > 50) eventLog.remove(eventLog.size() - 1);
    }
    
    static class TestPlayer {
        UUID id;
        String name;
        double health = 20.0;
        double maxHealth = 20.0;
        double x, y, z;
        float yaw = 0, pitch = 0;
        boolean online = true;
        int level = 1;
        int xp = 0;
        String gameMode = "SURVIVAL";
        
        TestPlayer(UUID id, String name, double x, double y, double z) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        String toJson() {
            return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"health\":%.1f,\"maxHealth\":%.1f,\"x\":%.1f,\"y\":%.1f,\"z\":%.1f,\"yaw\":%.1f,\"pitch\":%.1f,\"online\":%s,\"level\":%d,\"xp\":%d,\"gameMode\":\"%s\"}",
                id, name, health, maxHealth, x, y, z, yaw, pitch, online, level, xp, gameMode
            );
        }
    }
    
    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = generateDashboardHTML();
            sendResponse(exchange, 200, "text/html", html);
        }
    }
    
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long uptime = (System.currentTimeMillis() - serverStartTime) / 1000;
            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long maxMB = rt.maxMemory() / 1024 / 1024;
            
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"hytaleConnected\":").append(hytaleServerLoaded).append(",");
            json.append("\"rubidiumEdition\":\"").append(FeatureRegistry.getCurrentTier().getDisplayName()).append("\",");
            json.append("\"features\":").append(FeatureRegistry.getAllFeatures().size()).append(",");
            json.append("\"items\":").append(InventoryAPI.get().getAllDefinitions().size()).append(",");
            json.append("\"players\":").append(players.size()).append(",");
            json.append("\"worldTime\":").append(worldState.getTime()).append(",");
            json.append("\"weather\":\"").append(worldState.getWeather()).append("\",");
            json.append("\"uptime\":").append(uptime).append(",");
            json.append("\"ticks\":").append(tickCount).append(",");
            json.append("\"memoryUsed\":").append(usedMB).append(",");
            json.append("\"memoryMax\":").append(maxMB).append(",");
            json.append("\"tps\":20.0");
            json.append("}");
            sendResponse(exchange, 200, "application/json", json.toString());
        }
    }
    
    static class HytaleAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/api/hytale/info")) {
                StringBuilder json = new StringBuilder("{");
                json.append("\"loaded\":").append(hytaleServerLoaded).append(",");
                json.append("\"mode\":\"").append(hytaleServerLoaded ? "CONNECTED" : "SIMULATION").append("\",");
                json.append("\"version\":\"1.0.0\",");
                json.append("\"serverType\":\"Dedicated\",");
                
                json.append("\"availableAPIs\":[");
                json.append("\"InventoryAPI\",\"WorldAPI\",\"TitleAPI\",\"ScoreboardAPI\",");
                json.append("\"PlayerAPI\",\"EventAPI\",\"CommandAPI\",\"NPCAPI\",");
                json.append("\"VoiceChatAPI\",\"MinimapAPI\",\"PartyAPI\"");
                json.append("],");
                
                json.append("\"bridgeStatus\":\"").append(HytaleRuntimeBridge.get().isHytaleAvailable() ? "ACTIVE" : "FALLBACK").append("\"");
                json.append("}");
                sendResponse(exchange, 200, "application/json", json.toString());
            } else if (path.equals("/api/hytale/test")) {
                runHytaleTests(exchange);
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            }
        }
        
        private void runHytaleTests(HttpExchange exchange) throws IOException {
            StringBuilder results = new StringBuilder();
            results.append("{\"tests\":[");
            
            int passed = 0;
            int total = 0;
            
            total++; if (testFeatureRegistry()) { passed++; results.append("{\"name\":\"FeatureRegistry\",\"passed\":true},"); }
            else results.append("{\"name\":\"FeatureRegistry\",\"passed\":false},");
            
            total++; if (testInventoryAPI()) { passed++; results.append("{\"name\":\"InventoryAPI\",\"passed\":true},"); }
            else results.append("{\"name\":\"InventoryAPI\",\"passed\":false},");
            
            total++; if (testWorldAPI()) { passed++; results.append("{\"name\":\"WorldAPI\",\"passed\":true},"); }
            else results.append("{\"name\":\"WorldAPI\",\"passed\":false},");
            
            total++; if (testTitleAPI()) { passed++; results.append("{\"name\":\"TitleAPI\",\"passed\":true},"); }
            else results.append("{\"name\":\"TitleAPI\",\"passed\":false},");
            
            total++; if (testScoreboardAPI()) { passed++; results.append("{\"name\":\"ScoreboardAPI\",\"passed\":true},"); }
            else results.append("{\"name\":\"ScoreboardAPI\",\"passed\":false},");
            
            total++; if (testPlayerManagement()) { passed++; results.append("{\"name\":\"PlayerManagement\",\"passed\":true},"); }
            else results.append("{\"name\":\"PlayerManagement\",\"passed\":false},");
            
            total++; if (testRuntimeBridge()) { passed++; results.append("{\"name\":\"RuntimeBridge\",\"passed\":true}"); }
            else results.append("{\"name\":\"RuntimeBridge\",\"passed\":false}");
            
            results.append("],\"passed\":").append(passed).append(",\"total\":").append(total).append("}");
            
            logEvent("Ran " + total + " API tests: " + passed + "/" + total + " passed");
            sendResponse(exchange, 200, "application/json", results.toString());
        }
        
        private boolean testFeatureRegistry() {
            try {
                return FeatureRegistry.getAllFeatures().size() > 0 && FeatureRegistry.getCurrentTier() != null;
            } catch (Exception e) { return false; }
        }
        
        private boolean testInventoryAPI() {
            try {
                UUID testId = UUID.randomUUID();
                rubidium.api.item.PlayerInventory inv = InventoryAPI.get().getInventory(testId);
                inv.addItem(new ItemStack("hytale:diamond", 64));
                return inv.getSlot(0) != null;
            } catch (Exception e) { return false; }
        }
        
        private boolean testWorldAPI() {
            try {
                WorldAPI.get().setTime("test_world", 12000);
                return WorldAPI.get().getTime("test_world") == 12000;
            } catch (Exception e) { return false; }
        }
        
        private boolean testTitleAPI() {
            try {
                UUID testId = UUID.randomUUID();
                TitleAPI.get().sendTitle(testId, "Test", "Subtitle");
                return true;
            } catch (Exception e) { return false; }
        }
        
        private boolean testScoreboardAPI() {
            try {
                return ScoreboardAPI.get("server_stats").isPresent();
            } catch (Exception e) { return false; }
        }
        
        private boolean testPlayerManagement() {
            try {
                return players.size() > 0;
            } catch (Exception e) { return false; }
        }
        
        private boolean testRuntimeBridge() {
            try {
                HytaleRuntimeBridge bridge = HytaleRuntimeBridge.get();
                return bridge != null;
            } catch (Exception e) { return false; }
        }
    }
    
    static class PlayerAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            if (path.equals("/api/player/list")) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                for (TestPlayer p : players.values()) {
                    if (!first) json.append(",");
                    json.append(p.toJson());
                    first = false;
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } else if (path.equals("/api/player/create")) {
                String name = getQueryParam(query, "name", "Player" + (players.size() + 1));
                double x = Double.parseDouble(getQueryParam(query, "x", "0"));
                double y = Double.parseDouble(getQueryParam(query, "y", "64"));
                double z = Double.parseDouble(getQueryParam(query, "z", "0"));
                TestPlayer player = createTestPlayer(name, x, y, z);
                sendResponse(exchange, 200, "application/json", player.toJson());
            } else if (path.equals("/api/player/teleport")) {
                String idStr = getQueryParam(query, "id", null);
                double x = Double.parseDouble(getQueryParam(query, "x", "0"));
                double y = Double.parseDouble(getQueryParam(query, "y", "64"));
                double z = Double.parseDouble(getQueryParam(query, "z", "0"));
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    TestPlayer player = players.get(id);
                    if (player != null) {
                        player.x = x; player.y = y; player.z = z;
                        logEvent("Teleported " + player.name + " to (" + (int)x + ", " + (int)y + ", " + (int)z + ")");
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Player not found\"}");
            } else if (path.equals("/api/player/health")) {
                String idStr = getQueryParam(query, "id", null);
                double health = Double.parseDouble(getQueryParam(query, "health", "20"));
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    TestPlayer player = players.get(id);
                    if (player != null) {
                        player.health = Math.max(0, Math.min(20, health));
                        logEvent("Set " + player.name + "'s health to " + (int)player.health);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Player not found\"}");
            } else if (path.equals("/api/player/gamemode")) {
                String idStr = getQueryParam(query, "id", null);
                String mode = getQueryParam(query, "mode", "SURVIVAL");
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    TestPlayer player = players.get(id);
                    if (player != null) {
                        player.gameMode = mode.toUpperCase();
                        logEvent("Set " + player.name + "'s gamemode to " + player.gameMode);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Player not found\"}");
            } else if (path.equals("/api/player/kick")) {
                String idStr = getQueryParam(query, "id", null);
                String reason = getQueryParam(query, "reason", "Kicked by admin");
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    TestPlayer player = players.remove(id);
                    if (player != null) {
                        logEvent("Kicked " + player.name + ": " + reason);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Player not found\"}");
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            }
        }
    }
    
    static class InventoryAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            if (path.equals("/api/inventory/items")) {
                Collection<InventoryAPI.ItemDefinition> defs = InventoryAPI.get().getAllDefinitions();
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                for (InventoryAPI.ItemDefinition def : defs) {
                    if (!first) json.append(",");
                    json.append("{\"id\":\"").append(def.getId())
                        .append("\",\"name\":\"").append(def.getName())
                        .append("\",\"maxStack\":").append(def.getMaxStack()).append("}");
                    first = false;
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } else if (path.equals("/api/inventory/get")) {
                String idStr = getQueryParam(query, "player", null);
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    rubidium.api.item.PlayerInventory inv = InventoryAPI.get().getInventory(id);
                    if (inv != null) {
                        StringBuilder json = new StringBuilder("{\"slots\":[");
                        boolean first = true;
                        for (int i = 0; i < 9; i++) {
                            if (!first) json.append(",");
                            ItemStack item = inv.getSlot(i);
                            if (item != null) {
                                json.append("{\"slot\":").append(i)
                                    .append(",\"type\":\"").append(item.getType())
                                    .append("\",\"amount\":").append(item.getAmount()).append("}");
                            } else {
                                json.append("null");
                            }
                            first = false;
                        }
                        json.append("]}");
                        sendResponse(exchange, 200, "application/json", json.toString());
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Inventory not found\"}");
            } else if (path.equals("/api/inventory/give")) {
                String idStr = getQueryParam(query, "player", null);
                String itemType = getQueryParam(query, "item", "hytale:diamond");
                int amount = Integer.parseInt(getQueryParam(query, "amount", "1"));
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    rubidium.api.item.PlayerInventory inv = InventoryAPI.get().getInventory(id);
                    TestPlayer player = players.get(id);
                    if (inv != null && player != null) {
                        inv.addItem(new ItemStack(itemType, amount));
                        logEvent("Gave " + amount + "x " + itemType.replace("hytale:", "") + " to " + player.name);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Failed to give item\"}");
            } else if (path.equals("/api/inventory/clear")) {
                String idStr = getQueryParam(query, "player", null);
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    rubidium.api.item.PlayerInventory inv = InventoryAPI.get().getInventory(id);
                    TestPlayer player = players.get(id);
                    if (inv != null && player != null) {
                        inv.clear();
                        logEvent("Cleared " + player.name + "'s inventory");
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Failed to clear inventory\"}");
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            }
        }
    }
    
    static class WorldAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            if (path.equals("/api/world/status")) {
                boolean isDay = WorldAPI.get().getTime("test_world") < 12000;
                String json = String.format(
                    "{\"time\":%d,\"weather\":\"%s\",\"isDay\":%s,\"isNight\":%s,\"pvpEnabled\":%s}",
                    worldState.getTime(), worldState.getWeather(), isDay, !isDay, worldState.isPvpEnabled()
                );
                sendResponse(exchange, 200, "application/json", json);
            } else if (path.equals("/api/world/time")) {
                long time = Long.parseLong(getQueryParam(query, "value", "6000"));
                WorldAPI.get().setTime("test_world", time);
                boolean isDay = time < 12000;
                logEvent("Set world time to " + time + " (" + (isDay ? "Day" : "Night") + ")");
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"time\":" + time + "}");
            } else if (path.equals("/api/world/weather")) {
                String weather = getQueryParam(query, "value", "clear");
                WorldAPI.Weather w = WorldAPI.Weather.valueOf(weather.toUpperCase());
                WorldAPI.get().setWeather("test_world", w);
                logEvent("Set weather to " + weather);
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"weather\":\"" + weather + "\"}");
            } else if (path.equals("/api/world/explosion")) {
                double x = Double.parseDouble(getQueryParam(query, "x", "0"));
                double y = Double.parseDouble(getQueryParam(query, "y", "64"));
                double z = Double.parseDouble(getQueryParam(query, "z", "0"));
                float power = Float.parseFloat(getQueryParam(query, "power", "4"));
                WorldAPI.get().createExplosion("test_world", x, y, z, power, false, true);
                logEvent("Explosion at (" + (int)x + ", " + (int)y + ", " + (int)z + ") power " + power);
                sendResponse(exchange, 200, "application/json", "{\"success\":true}");
            } else if (path.equals("/api/world/lightning")) {
                double x = Double.parseDouble(getQueryParam(query, "x", "0"));
                double y = Double.parseDouble(getQueryParam(query, "y", "64"));
                double z = Double.parseDouble(getQueryParam(query, "z", "0"));
                WorldAPI.get().strikeLightning("test_world", x, y, z, false);
                logEvent("Lightning at (" + (int)x + ", " + (int)y + ", " + (int)z + ")");
                sendResponse(exchange, 200, "application/json", "{\"success\":true}");
            } else if (path.equals("/api/world/pvp")) {
                boolean enabled = Boolean.parseBoolean(getQueryParam(query, "enabled", "true"));
                worldState.setPvpEnabled(enabled);
                logEvent("PvP " + (enabled ? "enabled" : "disabled"));
                sendResponse(exchange, 200, "application/json", "{\"success\":true}");
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            }
        }
    }
    
    static class TitleAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            if (path.equals("/api/title/send")) {
                String idStr = getQueryParam(query, "player", null);
                String title = getQueryParam(query, "title", "Hello");
                String subtitle = getQueryParam(query, "subtitle", "");
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    TestPlayer player = players.get(id);
                    if (player != null) {
                        TitleAPI.get().sendTitle(id, title, subtitle);
                        logEvent("Title to " + player.name + ": " + title);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Player not found\"}");
            } else if (path.equals("/api/title/actionbar")) {
                String idStr = getQueryParam(query, "player", null);
                String message = getQueryParam(query, "message", "Action Bar");
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    TestPlayer player = players.get(id);
                    if (player != null) {
                        TitleAPI.get().sendActionBar(id, message);
                        logEvent("ActionBar to " + player.name + ": " + message);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Player not found\"}");
            } else if (path.equals("/api/title/bossbar")) {
                String idStr = getQueryParam(query, "player", null);
                String name = getQueryParam(query, "name", "Boss");
                float progress = Float.parseFloat(getQueryParam(query, "progress", "1.0"));
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    TestPlayer player = players.get(id);
                    if (player != null) {
                        TitleAPI.get().sendBossBar(id, "boss_" + id, name, progress, TitleAPI.BossBarColor.RED, TitleAPI.BossBarStyle.SOLID);
                        logEvent("BossBar to " + player.name + ": " + name + " (" + (int)(progress*100) + "%)");
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Player not found\"}");
            } else if (path.equals("/api/title/broadcast")) {
                String title = getQueryParam(query, "title", "Broadcast");
                String subtitle = getQueryParam(query, "subtitle", "");
                Set<UUID> allIds = new HashSet<>(players.keySet());
                TitleAPI.get().broadcastTitle(allIds, title, subtitle, 10, 70, 20);
                logEvent("Broadcast title: " + title + " to " + allIds.size() + " players");
                sendResponse(exchange, 200, "application/json", "{\"success\":true}");
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            }
        }
    }
    
    static class ScoreboardAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            if (path.equals("/api/scoreboard/get")) {
                Optional<ScoreboardAPI.Scoreboard> sb = ScoreboardAPI.get("server_stats");
                if (sb.isPresent()) {
                    StringBuilder json = new StringBuilder("{\"id\":\"server_stats\",\"title\":\"")
                        .append(sb.get().getTitle()).append("\",\"lines\":[");
                    boolean first = true;
                    for (ScoreboardAPI.ScoreEntry entry : sb.get().getEntries()) {
                        if (!first) json.append(",");
                        json.append("{\"text\":\"").append(entry.text())
                            .append("\",\"score\":").append(entry.score()).append("}");
                        first = false;
                    }
                    json.append("]}");
                    sendResponse(exchange, 200, "application/json", json.toString());
                } else {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Scoreboard not found\"}");
                }
            } else if (path.equals("/api/scoreboard/update")) {
                int line = Integer.parseInt(getQueryParam(query, "line", "0"));
                String text = getQueryParam(query, "text", "Updated");
                
                Optional<ScoreboardAPI.Scoreboard> sb = ScoreboardAPI.get("server_stats");
                if (sb.isPresent()) {
                    sb.get().setLine(line, text, line);
                    logEvent("Scoreboard line " + line + ": " + text);
                    sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                } else {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Scoreboard not found\"}");
                }
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            }
        }
    }
    
    static class CommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String cmd = getQueryParam(query, "cmd", "help");
            
            String result = executeCommand(cmd);
            logEvent("Command: /" + cmd);
            sendResponse(exchange, 200, "application/json", "{\"result\":\"" + result.replace("\"", "\\\"") + "\"}");
        }
        
        private String executeCommand(String cmd) {
            String[] parts = cmd.split(" ");
            String command = parts[0].toLowerCase();
            
            switch (command) {
                case "help":
                    return "Commands: /rubidium, /help, /time, /weather, /day, /night, /tp, /give, /heal, /kill, /gamemode, /spawn, /kick, /say, /broadcast, /clear, /list, /tps, /memory, /gc, /uptime, /version, /plugins, /world, /pvp, /difficulty, /seed, /save, /reload, /stop";
                
                case "rubidium":
                    StringBuilder rb = new StringBuilder();
                    rb.append("=== Rubidium Framework ===\\n");
                    rb.append("Edition: ").append(FeatureRegistry.getCurrentTier().getDisplayName()).append("\\n");
                    rb.append("Features: ").append(FeatureRegistry.getAllFeatures().size()).append(" loaded\\n");
                    rb.append("Items: ").append(InventoryAPI.get().getAllDefinitions().size()).append(" registered\\n");
                    rb.append("Players: ").append(players.size()).append(" online\\n");
                    rb.append("Runtime: ").append(HytaleRuntimeBridge.get().isHytaleAvailable() ? "Connected" : "Simulation").append("\\n");
                    rb.append("Uptime: ").append((System.currentTimeMillis() - serverStartTime) / 1000).append("s\\n");
                    rb.append("Sub-commands: /rubidium version, /rubidium features, /rubidium reload, /rubidium status");
                    if (parts.length > 1) {
                        switch (parts[1].toLowerCase()) {
                            case "version": return "Rubidium " + FeatureRegistry.getCurrentTier().getDisplayName() + " v1.0.0";
                            case "features": return "Features: " + String.join(", ", FeatureRegistry.getAllFeatures().stream().map(f -> f.id()).toArray(String[]::new));
                            case "reload": return "Rubidium configuration reloaded.";
                            case "status": return "Status: RUNNING | Mode: " + (hytaleServerLoaded ? "CONNECTED" : "SIMULATION") + " | TPS: 20.0";
                            default: return rb.toString();
                        }
                    }
                    return rb.toString();
                
                case "time":
                    if (parts.length > 1) {
                        String timeArg = parts[1].toLowerCase();
                        long time;
                        switch (timeArg) {
                            case "day": time = 1000; break;
                            case "noon": time = 6000; break;
                            case "sunset": time = 12000; break;
                            case "night": time = 13000; break;
                            case "midnight": time = 18000; break;
                            case "sunrise": time = 23000; break;
                            default: time = Long.parseLong(timeArg);
                        }
                        WorldAPI.get().setTime("test_world", time);
                        return "Time set to " + time + " (" + timeArg + ")";
                    }
                    return "Usage: /time <day|noon|sunset|night|midnight|sunrise|value>";
                
                case "day":
                    WorldAPI.get().setTime("test_world", 1000);
                    return "Time set to day (1000)";
                
                case "night":
                    WorldAPI.get().setTime("test_world", 13000);
                    return "Time set to night (13000)";
                
                case "weather":
                    if (parts.length > 1) {
                        try {
                            WorldAPI.Weather w = WorldAPI.Weather.valueOf(parts[1].toUpperCase());
                            WorldAPI.get().setWeather("test_world", w);
                            return "Weather set to " + parts[1];
                        } catch (Exception e) {
                            return "Invalid weather. Use: clear, rain, thunder, snow";
                        }
                    }
                    return "Usage: /weather <clear|rain|thunder|snow>";
                
                case "tp":
                case "teleport":
                    if (parts.length >= 4) {
                        String playerName = parts[1];
                        double x = Double.parseDouble(parts[2]);
                        double y = Double.parseDouble(parts[3]);
                        double z = parts.length > 4 ? Double.parseDouble(parts[4]) : 0;
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                p.x = x; p.y = y; p.z = z;
                                return "Teleported " + p.name + " to (" + (int)x + ", " + (int)y + ", " + (int)z + ")";
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /tp <player> <x> <y> [z]";
                
                case "give":
                    if (parts.length >= 3) {
                        String playerName = parts[1];
                        String item = parts[2].contains(":") ? parts[2] : "hytale:" + parts[2];
                        int amount = parts.length > 3 ? Integer.parseInt(parts[3]) : 1;
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                InventoryAPI.get().getInventory(p.id).addItem(new ItemStack(item, amount));
                                return "Gave " + amount + "x " + item + " to " + p.name;
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /give <player> <item> [amount]";
                
                case "heal":
                    if (parts.length >= 2) {
                        String playerName = parts[1];
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                p.health = p.maxHealth;
                                return "Healed " + p.name + " to full health";
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    for (TestPlayer p : players.values()) { p.health = p.maxHealth; }
                    return "Healed all players";
                
                case "kill":
                    if (parts.length >= 2) {
                        String playerName = parts[1];
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                p.health = 0;
                                return "Killed " + p.name;
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /kill <player>";
                
                case "gamemode":
                case "gm":
                    if (parts.length >= 3) {
                        String playerName = parts[1];
                        String mode = parts[2].toUpperCase();
                        if (mode.equals("0")) mode = "SURVIVAL";
                        else if (mode.equals("1")) mode = "CREATIVE";
                        else if (mode.equals("2")) mode = "ADVENTURE";
                        else if (mode.equals("3")) mode = "SPECTATOR";
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                p.gameMode = mode;
                                return "Set " + p.name + "'s gamemode to " + mode;
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /gamemode <player> <survival|creative|adventure|spectator|0-3>";
                
                case "spawn":
                    if (parts.length >= 2) {
                        String playerName = parts[1];
                        double x = Math.random() * 200 - 100;
                        double z = Math.random() * 200 - 100;
                        TestPlayer newPlayer = createTestPlayer(playerName, x, 64, z);
                        return "Spawned " + playerName + " at (" + (int)x + ", 64, " + (int)z + ")";
                    }
                    return "Usage: /spawn <playername>";
                
                case "kick":
                    if (parts.length >= 2) {
                        String playerName = parts[1];
                        String reason = parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)) : "Kicked by admin";
                        for (UUID id : new HashSet<>(players.keySet())) {
                            TestPlayer p = players.get(id);
                            if (p.name.equalsIgnoreCase(playerName)) {
                                players.remove(id);
                                return "Kicked " + p.name + ": " + reason;
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /kick <player> [reason]";
                
                case "say":
                case "broadcast":
                    if (parts.length >= 2) {
                        String message = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        logEvent("[BROADCAST] " + message);
                        return "Broadcast: " + message;
                    }
                    return "Usage: /say <message>";
                
                case "clear":
                    if (parts.length >= 2) {
                        String playerName = parts[1];
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                InventoryAPI.get().getInventory(p.id).clear();
                                return "Cleared " + p.name + "'s inventory";
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /clear <player>";
                
                case "list":
                case "online":
                case "players":
                    StringBuilder list = new StringBuilder("Online players (" + players.size() + "):\\n");
                    for (TestPlayer p : players.values()) {
                        list.append(" - ").append(p.name).append(" [").append(p.gameMode).append("] HP: ").append((int)p.health).append("/").append((int)p.maxHealth).append("\\n");
                    }
                    return list.toString().trim();
                
                case "tps":
                case "lag":
                    double tps = 20.0;
                    return "TPS: " + tps + " | MSPT: 50ms | Ticks: " + tickCount;
                
                case "memory":
                case "mem":
                    Runtime rt = Runtime.getRuntime();
                    long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                    long total = rt.totalMemory() / 1024 / 1024;
                    long max = rt.maxMemory() / 1024 / 1024;
                    long free = rt.freeMemory() / 1024 / 1024;
                    return "Memory: " + used + "MB / " + max + "MB (Allocated: " + total + "MB, Free: " + free + "MB)";
                
                case "gc":
                    System.gc();
                    return "Garbage collection triggered";
                
                case "uptime":
                    long uptimeSec = (System.currentTimeMillis() - serverStartTime) / 1000;
                    long hours = uptimeSec / 3600;
                    long minutes = (uptimeSec % 3600) / 60;
                    long seconds = uptimeSec % 60;
                    return "Uptime: " + hours + "h " + minutes + "m " + seconds + "s (" + uptimeSec + " seconds)";
                
                case "version":
                case "ver":
                    return "Rubidium " + FeatureRegistry.getCurrentTier().getDisplayName() + " v1.0.0 | Hytale Server Integration | Java " + System.getProperty("java.version");
                
                case "plugins":
                case "pl":
                    return "Plugins (1): Rubidium " + FeatureRegistry.getCurrentTier().getDisplayName();
                
                case "world":
                case "worlds":
                    return "Worlds: test_world | Time: " + WorldAPI.get().getTime("test_world") + " | Weather: " + worldState.getWeather() + " | PvP: " + (worldState.isPvpEnabled() ? "ON" : "OFF");
                
                case "pvp":
                    if (parts.length >= 2) {
                        boolean enabled = parts[1].equalsIgnoreCase("on") || parts[1].equalsIgnoreCase("true");
                        worldState.setPvpEnabled(enabled);
                        return "PvP " + (enabled ? "enabled" : "disabled");
                    }
                    return "PvP is " + (worldState.isPvpEnabled() ? "enabled" : "disabled") + ". Usage: /pvp <on|off>";
                
                case "difficulty":
                case "diff":
                    return "Difficulty: NORMAL (Simulation mode)";
                
                case "seed":
                    return "World seed: " + Math.abs("test_world".hashCode());
                
                case "save":
                case "save-all":
                    return "World saved successfully.";
                
                case "reload":
                    return "Server configuration reloaded.";
                
                case "stop":
                case "shutdown":
                    return "Server shutdown initiated... (simulation only - server continues running)";
                
                case "xp":
                case "experience":
                    if (parts.length >= 3) {
                        String playerName = parts[1];
                        int xp = Integer.parseInt(parts[2]);
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                p.xp += xp;
                                p.level = p.xp / 100;
                                return "Gave " + xp + " XP to " + p.name + " (Level: " + p.level + ")";
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /xp <player> <amount>";
                
                case "effect":
                    if (parts.length >= 3) {
                        String playerName = parts[1];
                        String effect = parts[2];
                        int duration = parts.length > 3 ? Integer.parseInt(parts[3]) : 30;
                        return "Applied " + effect + " to " + playerName + " for " + duration + "s";
                    }
                    return "Usage: /effect <player> <effect> [duration]";
                
                case "summon":
                    if (parts.length >= 2) {
                        String entity = parts[1];
                        return "Summoned " + entity + " at spawn location";
                    }
                    return "Usage: /summon <entity>";
                
                case "setblock":
                    if (parts.length >= 5) {
                        return "Set block " + parts[4] + " at (" + parts[1] + ", " + parts[2] + ", " + parts[3] + ")";
                    }
                    return "Usage: /setblock <x> <y> <z> <block>";
                
                case "fill":
                    return "Usage: /fill <x1> <y1> <z1> <x2> <y2> <z2> <block>";
                
                case "enchant":
                    if (parts.length >= 3) {
                        return "Enchanted " + parts[1] + "'s held item with " + parts[2];
                    }
                    return "Usage: /enchant <player> <enchantment> [level]";
                
                case "title":
                    if (parts.length >= 3) {
                        String playerName = parts[1];
                        String titleText = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                        for (TestPlayer p : players.values()) {
                            if (p.name.equalsIgnoreCase(playerName)) {
                                TitleAPI.get().sendTitle(p.id, titleText, "");
                                return "Sent title '" + titleText + "' to " + p.name;
                            }
                        }
                        return "Player not found: " + playerName;
                    }
                    return "Usage: /title <player> <text>";
                
                case "scoreboard":
                    return "Scoreboard: server_stats | Lines: 4 | Use dashboard to modify";
                
                case "whitelist":
                    return "Whitelist is OFF (simulation mode)";
                
                case "ban":
                case "pardon":
                case "op":
                case "deop":
                    return "Permission commands are disabled in simulation mode";
                
                case "?":
                    return "Type /help for a list of commands or /rubidium for framework info";
                
                default:
                    return "Unknown command: /" + command + ". Type /help for a list of commands or /rubidium for Rubidium info.";
            }
        }
    }
    
    static class EventsAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (String event : eventLog) {
                if (!first) json.append(",");
                json.append("\"").append(event.replace("\"", "\\\"")).append("\"");
                first = false;
            }
            json.append("]");
            sendResponse(exchange, 200, "application/json", json.toString());
        }
    }
    
    static class LogsAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (String log : serverLog) {
                if (!first) json.append(",");
                json.append("\"").append(log.replace("\"", "\\\"")).append("\"");
                first = false;
            }
            json.append("]");
            sendResponse(exchange, 200, "application/json", json.toString());
        }
    }
    
    private static String getQueryParam(String query, String key, String defaultVal) {
        if (query == null) return defaultVal;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && pair[0].equals(key)) {
                try {
                    return java.net.URLDecoder.decode(pair[1], "UTF-8");
                } catch (Exception e) {
                    return pair[1];
                }
            }
        }
        return defaultVal;
    }
    
    private static void sendResponse(HttpExchange exchange, int code, String contentType, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        byte[] bytes = body.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private static String generateDashboardHTML() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rubidium + Hytale Test Environment</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', sans-serif; background: linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0d1b2a 100%); min-height: 100vh; color: #fff; }
        .header { background: linear-gradient(90deg, #667eea, #764ba2, #f093fb); padding: 25px; text-align: center; box-shadow: 0 4px 30px rgba(102,126,234,0.4); }
        .header h1 { font-size: 32px; margin-bottom: 8px; text-shadow: 0 2px 10px rgba(0,0,0,0.3); }
        .header .subtitle { opacity: 0.9; font-size: 14px; }
        .status-bar { display: flex; justify-content: center; gap: 30px; padding: 15px; background: rgba(0,0,0,0.3); }
        .status-item { display: flex; align-items: center; gap: 8px; }
        .status-dot { width: 10px; height: 10px; border-radius: 50%; }
        .status-dot.connected { background: #4ade80; box-shadow: 0 0 10px #4ade80; }
        .status-dot.simulation { background: #fbbf24; box-shadow: 0 0 10px #fbbf24; }
        .container { max-width: 1600px; margin: 0 auto; padding: 20px; display: grid; grid-template-columns: repeat(auto-fit, minmax(380px, 1fr)); gap: 20px; }
        .card { background: rgba(255,255,255,0.03); border-radius: 16px; padding: 20px; backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.08); transition: transform 0.2s, box-shadow 0.2s; }
        .card:hover { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(102,126,234,0.2); }
        .card h2 { color: #a78bfa; margin-bottom: 15px; font-size: 18px; display: flex; align-items: center; gap: 10px; }
        .card h2 .icon { font-size: 24px; }
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 15px; }
        .stat-box { background: rgba(0,0,0,0.25); padding: 15px; border-radius: 12px; text-align: center; }
        .stat-box .value { font-size: 28px; font-weight: bold; background: linear-gradient(90deg, #667eea, #764ba2); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .stat-box .label { font-size: 11px; opacity: 0.6; margin-top: 4px; text-transform: uppercase; }
        .player-list { max-height: 220px; overflow-y: auto; }
        .player-item { display: flex; justify-content: space-between; align-items: center; padding: 12px; background: rgba(0,0,0,0.2); border-radius: 10px; margin-bottom: 8px; }
        .player-info { display: flex; flex-direction: column; }
        .player-name { font-weight: 600; font-size: 15px; }
        .player-details { font-size: 12px; opacity: 0.6; }
        .player-health { display: flex; align-items: center; gap: 5px; color: #f87171; }
        .btn { padding: 10px 18px; border: none; border-radius: 10px; cursor: pointer; font-size: 13px; font-weight: 500; transition: all 0.2s; }
        .btn-primary { background: linear-gradient(90deg, #667eea, #764ba2); color: white; }
        .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(102,126,234,0.4); }
        .btn-secondary { background: rgba(255,255,255,0.08); color: white; }
        .btn-secondary:hover { background: rgba(255,255,255,0.15); }
        .btn-danger { background: linear-gradient(90deg, #ef4444, #dc2626); color: white; }
        .btn-success { background: linear-gradient(90deg, #22c55e, #16a34a); color: white; }
        .btn-group { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 15px; }
        .input-group { display: flex; gap: 8px; margin-top: 12px; }
        .input-group input, .input-group select { flex: 1; padding: 10px 14px; border: 1px solid rgba(255,255,255,0.15); border-radius: 10px; background: rgba(0,0,0,0.25); color: white; font-size: 13px; }
        .input-group input::placeholder { color: rgba(255,255,255,0.4); }
        .log-container { max-height: 250px; overflow-y: auto; font-family: 'Consolas', monospace; font-size: 11px; background: rgba(0,0,0,0.35); padding: 15px; border-radius: 12px; }
        .log-entry { padding: 4px 0; border-bottom: 1px solid rgba(255,255,255,0.03); }
        .log-entry:last-child { border-bottom: none; }
        .time-display { font-size: 56px; text-align: center; font-weight: 700; background: linear-gradient(90deg, #fbbf24, #f59e0b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 15px 0; }
        .weather-icon { font-size: 40px; text-align: center; margin: 10px 0; }
        .controls-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
        .test-results { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 8px; }
        .test-item { padding: 10px; border-radius: 8px; text-align: center; font-size: 12px; }
        .test-item.pass { background: rgba(34,197,94,0.2); border: 1px solid #22c55e; }
        .test-item.fail { background: rgba(239,68,68,0.2); border: 1px solid #ef4444; }
        .command-input { width: 100%; padding: 12px; border: 1px solid rgba(255,255,255,0.15); border-radius: 10px; background: rgba(0,0,0,0.25); color: #22c55e; font-family: 'Consolas', monospace; font-size: 14px; }
        .command-output { margin-top: 10px; padding: 12px; background: rgba(0,0,0,0.35); border-radius: 10px; font-family: 'Consolas', monospace; font-size: 12px; color: #a3e635; min-height: 40px; }
        .inventory-grid { display: grid; grid-template-columns: repeat(9, 1fr); gap: 4px; margin-top: 12px; }
        .inv-slot { aspect-ratio: 1; background: rgba(0,0,0,0.35); border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 9px; text-align: center; border: 1px solid rgba(255,255,255,0.08); }
        .inv-slot.filled { background: rgba(102,126,234,0.2); border-color: #667eea; }
        @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
        .live { animation: pulse 2s infinite; color: #4ade80; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Rubidium + Hytale Test Environment</h1>
        <div class="subtitle">Production-Ready Framework Testing Dashboard</div>
    </div>
    
    <div class="status-bar">
        <div class="status-item">
            <span class="status-dot" id="hytale-status"></span>
            <span id="hytale-mode">Loading...</span>
        </div>
        <div class="status-item">
            <span class="status-dot connected"></span>
            <span id="rubidium-edition">Rubidium</span>
        </div>
        <div class="status-item">
            <span class="live">LIVE</span>
            <span id="uptime">0s</span>
        </div>
    </div>
    
    <div class="container">
        <div class="card">
            <h2><span class="icon">📊</span> Server Status</h2>
            <div class="stats-grid">
                <div class="stat-box"><div class="value" id="stat-players">0</div><div class="label">Players</div></div>
                <div class="stat-box"><div class="value" id="stat-features">0</div><div class="label">Features</div></div>
                <div class="stat-box"><div class="value" id="stat-items">0</div><div class="label">Items</div></div>
                <div class="stat-box"><div class="value" id="stat-ticks">0</div><div class="label">Ticks</div></div>
            </div>
            <div class="stats-grid">
                <div class="stat-box"><div class="value" id="stat-memory">0</div><div class="label">Memory MB</div></div>
                <div class="stat-box"><div class="value" id="stat-tps">20.0</div><div class="label">TPS</div></div>
            </div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="runTests()">Run API Tests</button>
                <button class="btn btn-secondary" onclick="refreshStatus()">Refresh</button>
            </div>
            <div id="test-results" class="test-results" style="margin-top:15px;"></div>
        </div>
        
        <div class="card">
            <h2><span class="icon">👥</span> Players</h2>
            <div id="player-list" class="player-list"></div>
            <div class="input-group">
                <input type="text" id="new-player-name" placeholder="Player name...">
                <button class="btn btn-primary" onclick="createPlayer()">Spawn</button>
            </div>
            <div class="btn-group">
                <button class="btn btn-secondary" onclick="teleportRandom()">Teleport</button>
                <button class="btn btn-secondary" onclick="damageRandom()">Damage</button>
                <button class="btn btn-danger" onclick="kickRandom()">Kick</button>
            </div>
        </div>
        
        <div class="card">
            <h2><span class="icon">🎒</span> Inventory API</h2>
            <select id="inv-player" style="width:100%;padding:10px;border-radius:10px;background:rgba(0,0,0,0.25);color:white;border:1px solid rgba(255,255,255,0.15);" onchange="loadInventory()">
                <option value="">Select player...</option>
            </select>
            <div id="inventory-display" class="inventory-grid"></div>
            <div class="input-group">
                <select id="item-type">
                    <option value="hytale:diamond_sword">Diamond Sword</option>
                    <option value="hytale:iron_sword">Iron Sword</option>
                    <option value="hytale:diamond_pickaxe">Diamond Pickaxe</option>
                    <option value="hytale:golden_apple">Golden Apple</option>
                    <option value="hytale:diamond">Diamond</option>
                    <option value="hytale:iron_ingot">Iron Ingot</option>
                    <option value="hytale:bread">Bread</option>
                    <option value="hytale:torch">Torch</option>
                </select>
                <input type="number" id="item-amount" value="1" min="1" max="64" style="width:60px;">
                <button class="btn btn-primary" onclick="giveItem()">Give</button>
            </div>
            <div class="btn-group">
                <button class="btn btn-danger" onclick="clearInventory()">Clear Inv</button>
            </div>
        </div>
        
        <div class="card">
            <h2><span class="icon">🌍</span> World API</h2>
            <div class="time-display" id="world-time">6000</div>
            <div class="weather-icon" id="weather-icon">☀️</div>
            <div class="controls-grid">
                <button class="btn btn-secondary" onclick="setTime(0)">🌙 Midnight</button>
                <button class="btn btn-secondary" onclick="setTime(6000)">🌅 Morning</button>
                <button class="btn btn-secondary" onclick="setTime(12000)">☀️ Noon</button>
                <button class="btn btn-secondary" onclick="setTime(18000)">🌆 Sunset</button>
            </div>
            <div class="btn-group">
                <button class="btn btn-secondary" onclick="setWeather('clear')">☀️ Clear</button>
                <button class="btn btn-secondary" onclick="setWeather('rain')">🌧️ Rain</button>
                <button class="btn btn-secondary" onclick="setWeather('thunder')">⛈️ Thunder</button>
                <button class="btn btn-secondary" onclick="setWeather('snow')">❄️ Snow</button>
            </div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="explosion()">💥 Explosion</button>
                <button class="btn btn-primary" onclick="lightning()">⚡ Lightning</button>
            </div>
        </div>
        
        <div class="card">
            <h2><span class="icon">📝</span> Title API</h2>
            <div class="input-group">
                <input type="text" id="title-text" placeholder="Title...">
                <input type="text" id="subtitle-text" placeholder="Subtitle...">
            </div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="sendTitle()">Send Title</button>
                <button class="btn btn-success" onclick="broadcastTitle()">Broadcast</button>
            </div>
            <div class="input-group">
                <input type="text" id="actionbar-text" placeholder="Action bar message...">
                <button class="btn btn-primary" onclick="sendActionBar()">Action Bar</button>
            </div>
            <div class="input-group">
                <input type="text" id="bossbar-name" placeholder="Boss name...">
                <input type="range" id="bossbar-progress" min="0" max="100" value="100" style="flex:1;">
                <button class="btn btn-primary" onclick="sendBossBar()">Boss Bar</button>
            </div>
        </div>
        
        <div class="card">
            <h2><span class="icon">📋</span> Scoreboard API</h2>
            <div id="scoreboard" style="background:rgba(0,0,0,0.35);padding:15px;border-radius:12px;">
                <div style="text-align:center;color:#a78bfa;font-weight:600;margin-bottom:10px;border-bottom:1px solid rgba(255,255,255,0.1);padding-bottom:10px;" id="sb-title">Server Stats</div>
                <div id="scoreboard-lines"></div>
            </div>
            <div class="input-group">
                <select id="sb-line"><option value="0">Line 0</option><option value="1">Line 1</option><option value="2">Line 2</option><option value="3">Line 3</option></select>
                <input type="text" id="sb-text" placeholder="New text...">
                <button class="btn btn-primary" onclick="updateScoreboard()">Update</button>
            </div>
        </div>
        
        <div class="card">
            <h2><span class="icon">💻</span> Command Console</h2>
            <input type="text" class="command-input" id="command-input" placeholder="Type a command... (e.g. /help)" onkeypress="if(event.key==='Enter')runCommand()">
            <div class="command-output" id="command-output">Type /help to see available commands</div>
        </div>
        
        <div class="card" style="grid-column: span 2;">
            <h2><span class="icon">📜</span> Event Log</h2>
            <div id="event-log" class="log-container"></div>
        </div>
        
        <div class="card" style="grid-column: span 2;">
            <h2><span class="icon">🔧</span> Server Log</h2>
            <div id="server-log" class="log-container"></div>
        </div>
    </div>
    
    <script>
        let players = [];
        
        async function api(path) {
            const res = await fetch(path);
            return res.json();
        }
        
        async function refreshStatus() {
            const status = await api('/api/status');
            document.getElementById('stat-players').textContent = status.players;
            document.getElementById('stat-features').textContent = status.features;
            document.getElementById('stat-items').textContent = status.items;
            document.getElementById('stat-ticks').textContent = status.ticks;
            document.getElementById('stat-memory').textContent = status.memoryUsed;
            document.getElementById('stat-tps').textContent = status.tps.toFixed(1);
            document.getElementById('world-time').textContent = status.worldTime;
            document.getElementById('uptime').textContent = status.uptime + 's';
            document.getElementById('rubidium-edition').textContent = status.rubidiumEdition;
            updateWeatherIcon(status.weather);
            
            const hytaleStatus = document.getElementById('hytale-status');
            const hytaleMode = document.getElementById('hytale-mode');
            if (status.hytaleConnected) {
                hytaleStatus.className = 'status-dot connected';
                hytaleMode.textContent = 'HytaleServer Connected';
            } else {
                hytaleStatus.className = 'status-dot simulation';
                hytaleMode.textContent = 'Simulation Mode';
            }
        }
        
        async function runTests() {
            const results = await api('/api/hytale/test');
            const container = document.getElementById('test-results');
            container.innerHTML = results.tests.map(t => 
                `<div class="test-item ${t.passed ? 'pass' : 'fail'}">${t.name}<br>${t.passed ? '✓' : '✗'}</div>`
            ).join('') + `<div class="test-item ${results.passed === results.total ? 'pass' : 'fail'}">Total: ${results.passed}/${results.total}</div>`;
            loadEvents();
        }
        
        async function loadPlayers() {
            players = await api('/api/player/list');
            const list = document.getElementById('player-list');
            const select = document.getElementById('inv-player');
            
            list.innerHTML = players.map(p => `
                <div class="player-item">
                    <div class="player-info">
                        <div class="player-name">${p.name}</div>
                        <div class="player-details">${p.gameMode} | Pos: ${p.x.toFixed(0)}, ${p.y.toFixed(0)}, ${p.z.toFixed(0)}</div>
                    </div>
                    <div class="player-health">❤️ ${p.health.toFixed(0)}/${p.maxHealth.toFixed(0)}</div>
                </div>
            `).join('');
            
            select.innerHTML = '<option value="">Select player...</option>' + 
                players.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
        }
        
        async function createPlayer() {
            const name = document.getElementById('new-player-name').value || 'Player' + (players.length + 1);
            const x = Math.floor(Math.random() * 200 - 100);
            const z = Math.floor(Math.random() * 200 - 100);
            await api(`/api/player/create?name=${encodeURIComponent(name)}&x=${x}&y=64&z=${z}`);
            document.getElementById('new-player-name').value = '';
            loadPlayers();
            loadEvents();
        }
        
        async function teleportRandom() {
            if (players.length === 0) return;
            const p = players[Math.floor(Math.random() * players.length)];
            const x = Math.floor(Math.random() * 200 - 100);
            const z = Math.floor(Math.random() * 200 - 100);
            await api(`/api/player/teleport?id=${p.id}&x=${x}&y=64&z=${z}`);
            loadPlayers();
            loadEvents();
        }
        
        async function damageRandom() {
            if (players.length === 0) return;
            const p = players[Math.floor(Math.random() * players.length)];
            const health = Math.max(1, p.health - Math.floor(Math.random() * 5 + 1));
            await api(`/api/player/health?id=${p.id}&health=${health}`);
            loadPlayers();
            loadEvents();
        }
        
        async function kickRandom() {
            if (players.length === 0) return;
            const p = players[Math.floor(Math.random() * players.length)];
            await api(`/api/player/kick?id=${p.id}&reason=Kicked%20from%20dashboard`);
            loadPlayers();
            loadEvents();
        }
        
        async function loadInventory() {
            const playerId = document.getElementById('inv-player').value;
            const display = document.getElementById('inventory-display');
            if (!playerId) {
                display.innerHTML = Array(9).fill('<div class="inv-slot">-</div>').join('');
                return;
            }
            const inv = await api(`/api/inventory/get?player=${playerId}`);
            display.innerHTML = inv.slots.map((slot, i) => {
                if (slot) {
                    const name = slot.type.replace('hytale:', '').replace('_', ' ');
                    return `<div class="inv-slot filled" title="${slot.type}">${name}\\nx${slot.amount}</div>`;
                }
                return '<div class="inv-slot">-</div>';
            }).join('');
        }
        
        async function giveItem() {
            const playerId = document.getElementById('inv-player').value;
            if (!playerId) { alert('Select a player first'); return; }
            const item = document.getElementById('item-type').value;
            const amount = document.getElementById('item-amount').value;
            await api(`/api/inventory/give?player=${playerId}&item=${item}&amount=${amount}`);
            loadInventory();
            loadEvents();
        }
        
        async function clearInventory() {
            const playerId = document.getElementById('inv-player').value;
            if (!playerId) { alert('Select a player first'); return; }
            await api(`/api/inventory/clear?player=${playerId}`);
            loadInventory();
            loadEvents();
        }
        
        async function setTime(time) {
            await api(`/api/world/time?value=${time}`);
            document.getElementById('world-time').textContent = time;
            loadScoreboard();
            loadEvents();
        }
        
        async function setWeather(weather) {
            await api(`/api/world/weather?value=${weather}`);
            updateWeatherIcon(weather);
            loadScoreboard();
            loadEvents();
        }
        
        function updateWeatherIcon(weather) {
            const icons = { CLEAR: '☀️', RAIN: '🌧️', THUNDER: '⛈️', SNOW: '❄️', clear: '☀️', rain: '🌧️', thunder: '⛈️', snow: '❄️' };
            document.getElementById('weather-icon').textContent = icons[weather] || '☀️';
        }
        
        async function explosion() {
            const x = Math.floor(Math.random() * 100 - 50);
            const z = Math.floor(Math.random() * 100 - 50);
            await api(`/api/world/explosion?x=${x}&y=64&z=${z}&power=4`);
            loadEvents();
        }
        
        async function lightning() {
            const x = Math.floor(Math.random() * 100 - 50);
            const z = Math.floor(Math.random() * 100 - 50);
            await api(`/api/world/lightning?x=${x}&y=64&z=${z}`);
            loadEvents();
        }
        
        async function sendTitle() {
            const playerId = document.getElementById('inv-player').value;
            if (!playerId) { alert('Select a player first'); return; }
            const title = document.getElementById('title-text').value || 'Hello';
            const subtitle = document.getElementById('subtitle-text').value;
            await api(`/api/title/send?player=${playerId}&title=${encodeURIComponent(title)}&subtitle=${encodeURIComponent(subtitle)}`);
            loadEvents();
        }
        
        async function broadcastTitle() {
            const title = document.getElementById('title-text').value || 'Broadcast';
            const subtitle = document.getElementById('subtitle-text').value;
            await api(`/api/title/broadcast?title=${encodeURIComponent(title)}&subtitle=${encodeURIComponent(subtitle)}`);
            loadEvents();
        }
        
        async function sendActionBar() {
            const playerId = document.getElementById('inv-player').value;
            if (!playerId) { alert('Select a player first'); return; }
            const message = document.getElementById('actionbar-text').value || 'Action Bar';
            await api(`/api/title/actionbar?player=${playerId}&message=${encodeURIComponent(message)}`);
            loadEvents();
        }
        
        async function sendBossBar() {
            const playerId = document.getElementById('inv-player').value;
            if (!playerId) { alert('Select a player first'); return; }
            const name = document.getElementById('bossbar-name').value || 'Boss';
            const progress = document.getElementById('bossbar-progress').value / 100;
            await api(`/api/title/bossbar?player=${playerId}&name=${encodeURIComponent(name)}&progress=${progress}`);
            loadEvents();
        }
        
        async function loadScoreboard() {
            const sb = await api('/api/scoreboard/get');
            document.getElementById('sb-title').textContent = sb.title;
            document.getElementById('scoreboard-lines').innerHTML = sb.lines.map(line => 
                `<div style="display:flex;justify-content:space-between;padding:4px 0;">${line.text}<span style="color:#667eea;">${line.score}</span></div>`
            ).join('');
        }
        
        async function updateScoreboard() {
            const line = document.getElementById('sb-line').value;
            const text = document.getElementById('sb-text').value || 'Updated';
            await api(`/api/scoreboard/update?line=${line}&text=${encodeURIComponent(text)}`);
            loadScoreboard();
            loadEvents();
        }
        
        async function runCommand() {
            const input = document.getElementById('command-input');
            const cmd = input.value.replace(/^\\//, '');
            if (!cmd) return;
            const result = await api(`/api/command?cmd=${encodeURIComponent(cmd)}`);
            document.getElementById('command-output').textContent = '> ' + result.result;
            input.value = '';
            loadEvents();
        }
        
        async function loadEvents() {
            const events = await api('/api/events');
            document.getElementById('event-log').innerHTML = events.map(e => `<div class="log-entry">${e}</div>`).join('');
        }
        
        async function loadServerLogs() {
            const logs = await api('/api/logs');
            document.getElementById('server-log').innerHTML = logs.map(l => `<div class="log-entry">${l}</div>`).join('');
        }
        
        refreshStatus();
        loadPlayers();
        loadScoreboard();
        loadEvents();
        loadServerLogs();
        loadInventory();
        
        setInterval(() => {
            refreshStatus();
            loadPlayers();
            loadScoreboard();
        }, 2000);
        
        setInterval(() => {
            loadEvents();
            loadServerLogs();
        }, 3000);
    </script>
</body>
</html>
""";
    }
}
