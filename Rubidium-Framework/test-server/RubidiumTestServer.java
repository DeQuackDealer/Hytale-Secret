package rubidium.testserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
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

public class RubidiumTestServer {
    
    private static final Map<UUID, SimulatedPlayer> players = new ConcurrentHashMap<>();
    private static final List<String> eventLog = new CopyOnWriteArrayList<>();
    private static WorldAPI.WorldState worldState;
    
    public static void main(String[] args) throws Exception {
        int port = 5000;
        
        log("==============================================");
        log("    RUBIDIUM FRAMEWORK TEST SERVER");
        log("==============================================");
        log("");
        
        initializeFramework();
        
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/test", new TestAPIHandler());
        server.createContext("/api/player", new PlayerAPIHandler());
        server.createContext("/api/inventory", new InventoryAPIHandler());
        server.createContext("/api/world", new WorldAPIHandler());
        server.createContext("/api/title", new TitleAPIHandler());
        server.createContext("/api/scoreboard", new ScoreboardAPIHandler());
        server.createContext("/api/events", new EventsAPIHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        log("Test server running on http://0.0.0.0:" + port);
        log("Open the webview to interact with Rubidium APIs");
        log("");
    }
    
    private static void initializeFramework() {
        log("Initializing Rubidium Framework...");
        
        FeatureRegistry.initialize(ProductTier.PLUS);
        log("  - Feature Registry: " + FeatureRegistry.getAllFeatures().size() + " features loaded");
        log("  - Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
        
        log("  - Inventory API: " + InventoryAPI.get().getAllDefinitions().size() + " item types registered");
        
        worldState = WorldAPI.get().getWorld("test_world");
        WorldAPI.get().setTime("test_world", 6000);
        WorldAPI.get().setWeather("test_world", WorldAPI.Weather.CLEAR);
        log("  - World API: test_world initialized (time: 6000, weather: clear)");
        
        ScoreboardAPI.Scoreboard sb = ScoreboardAPI.sidebar("server_stats", "Server Statistics");
        sb.setLine(0, "Players Online: 0", 0);
        sb.setLine(1, "World Time: Day", 1);
        sb.setLine(2, "Weather: Clear", 2);
        ScoreboardAPI.register(sb);
        log("  - Scoreboard API: server_stats scoreboard created");
        
        createTestPlayer("TestPlayer1");
        createTestPlayer("TestPlayer2");
        log("  - Created 2 test players");
        
        log("");
        log("Framework initialization complete!");
        log("");
    }
    
    private static SimulatedPlayer createTestPlayer(String name) {
        UUID id = UUID.randomUUID();
        SimulatedPlayer player = new SimulatedPlayer(id, name);
        players.put(id, player);
        
        rubidium.api.item.PlayerInventory inv = InventoryAPI.get().getInventory(id);
        inv.addItem(new ItemStack("hytale:iron_sword", 1));
        inv.addItem(new ItemStack("hytale:bread", 16));
        inv.addItem(new ItemStack("hytale:diamond_pickaxe", 1));
        
        logEvent("Player '" + name + "' joined with starter items");
        return player;
    }
    
    private static void log(String msg) {
        System.out.println(msg);
    }
    
    private static void logEvent(String event) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        String entry = "[" + timestamp + "] " + event;
        eventLog.add(0, entry);
        if (eventLog.size() > 50) {
            eventLog.remove(eventLog.size() - 1);
        }
        System.out.println("EVENT: " + event);
    }
    
    static class SimulatedPlayer {
        UUID id;
        String name;
        double health = 20.0;
        double x = 0, y = 64, z = 0;
        boolean online = true;
        
        SimulatedPlayer(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
        
        String toJson() {
            return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"health\":%.1f,\"x\":%.1f,\"y\":%.1f,\"z\":%.1f,\"online\":%s}",
                id, name, health, x, y, z, online
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
    
    static class TestAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"running\",");
            json.append("\"edition\":\"").append(FeatureRegistry.getCurrentTier().getDisplayName()).append("\",");
            json.append("\"features\":").append(FeatureRegistry.getAllFeatures().size()).append(",");
            json.append("\"items\":").append(InventoryAPI.get().getAllDefinitions().size()).append(",");
            json.append("\"players\":").append(players.size()).append(",");
            json.append("\"worldTime\":").append(worldState.getTime()).append(",");
            json.append("\"weather\":\"").append(worldState.getWeather()).append("\"");
            json.append("}");
            sendResponse(exchange, 200, "application/json", json.toString());
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
                for (SimulatedPlayer p : players.values()) {
                    if (!first) json.append(",");
                    json.append(p.toJson());
                    first = false;
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } else if (path.equals("/api/player/create")) {
                String name = getQueryParam(query, "name", "Player" + (players.size() + 1));
                SimulatedPlayer player = createTestPlayer(name);
                sendResponse(exchange, 200, "application/json", player.toJson());
            } else if (path.equals("/api/player/teleport")) {
                String idStr = getQueryParam(query, "id", null);
                double x = Double.parseDouble(getQueryParam(query, "x", "0"));
                double y = Double.parseDouble(getQueryParam(query, "y", "64"));
                double z = Double.parseDouble(getQueryParam(query, "z", "0"));
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    SimulatedPlayer player = players.get(id);
                    if (player != null) {
                        player.x = x;
                        player.y = y;
                        player.z = z;
                        logEvent("Teleported " + player.name + " to (" + x + ", " + y + ", " + z + ")");
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
                    SimulatedPlayer player = players.get(id);
                    if (player != null) {
                        player.health = Math.max(0, Math.min(20, health));
                        logEvent("Set " + player.name + "'s health to " + player.health);
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
                    json.append("\"").append(def.getId()).append("\"");
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
                String itemType = getQueryParam(query, "item", "hytale:stone");
                int amount = Integer.parseInt(getQueryParam(query, "amount", "1"));
                
                if (idStr != null) {
                    UUID id = UUID.fromString(idStr);
                    rubidium.api.item.PlayerInventory inv = InventoryAPI.get().getInventory(id);
                    SimulatedPlayer player = players.get(id);
                    if (inv != null && player != null) {
                        ItemStack item = new ItemStack(itemType, amount);
                        inv.addItem(item);
                        logEvent("Gave " + amount + "x " + itemType + " to " + player.name);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Failed to give item\"}");
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
                    "{\"time\":%d,\"weather\":\"%s\",\"isDay\":%s,\"isNight\":%s}",
                    worldState.getTime(),
                    worldState.getWeather(),
                    isDay,
                    !isDay
                );
                sendResponse(exchange, 200, "application/json", json);
            } else if (path.equals("/api/world/time")) {
                long time = Long.parseLong(getQueryParam(query, "value", "6000"));
                WorldAPI.get().setTime("test_world", time);
                boolean isDay = time < 12000;
                logEvent("Set world time to " + time + " (" + (isDay ? "Day" : "Night") + ")");
                updateScoreboard();
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"time\":" + time + "}");
            } else if (path.equals("/api/world/weather")) {
                String weather = getQueryParam(query, "value", "clear");
                WorldAPI.Weather w = WorldAPI.Weather.valueOf(weather.toUpperCase());
                WorldAPI.get().setWeather("test_world", w);
                logEvent("Set weather to " + weather);
                updateScoreboard();
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"weather\":\"" + weather + "\"}");
            } else if (path.equals("/api/world/explosion")) {
                double x = Double.parseDouble(getQueryParam(query, "x", "0"));
                double y = Double.parseDouble(getQueryParam(query, "y", "64"));
                double z = Double.parseDouble(getQueryParam(query, "z", "0"));
                float power = Float.parseFloat(getQueryParam(query, "power", "4"));
                WorldAPI.get().createExplosion("test_world", x, y, z, power, false, true);
                logEvent("Created explosion at (" + x + ", " + y + ", " + z + ") with power " + power);
                sendResponse(exchange, 200, "application/json", "{\"success\":true}");
            } else if (path.equals("/api/world/lightning")) {
                double x = Double.parseDouble(getQueryParam(query, "x", "0"));
                double y = Double.parseDouble(getQueryParam(query, "y", "64"));
                double z = Double.parseDouble(getQueryParam(query, "z", "0"));
                WorldAPI.get().strikeLightning("test_world", x, y, z, false);
                logEvent("Lightning strike at (" + x + ", " + y + ", " + z + ")");
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
                    SimulatedPlayer player = players.get(id);
                    if (player != null) {
                        TitleAPI.get().sendTitle(id, title, subtitle);
                        logEvent("Sent title '" + title + "' to " + player.name);
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
                    SimulatedPlayer player = players.get(id);
                    if (player != null) {
                        TitleAPI.get().sendActionBar(id, message);
                        logEvent("Sent action bar '" + message + "' to " + player.name);
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
                    SimulatedPlayer player = players.get(id);
                    if (player != null) {
                        TitleAPI.get().sendBossBar(id, "boss_" + id, name, progress, TitleAPI.BossBarColor.RED, TitleAPI.BossBarStyle.SOLID);
                        logEvent("Sent boss bar '" + name + "' (" + (int)(progress*100) + "%) to " + player.name);
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
                logEvent("Broadcast title '" + title + "' to all players");
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
                    logEvent("Updated scoreboard line " + line + " to '" + text + "'");
                    sendResponse(exchange, 200, "application/json", "{\"success\":true}");
                } else {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Scoreboard not found\"}");
                }
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
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
    
    private static void updateScoreboard() {
        Optional<ScoreboardAPI.Scoreboard> sb = ScoreboardAPI.get("server_stats");
        if (sb.isPresent()) {
            boolean isDay = WorldAPI.get().getTime("test_world") < 12000;
            sb.get().setLine(0, "Players Online: " + players.size(), 0);
            sb.get().setLine(1, "World Time: " + (isDay ? "Day" : "Night"), 1);
            sb.get().setLine(2, "Weather: " + worldState.getWeather(), 2);
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
    <title>Rubidium Framework Test Dashboard</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', Tahoma, sans-serif; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); min-height: 100vh; color: #fff; }
        .header { background: linear-gradient(90deg, #e94560, #ff6b6b); padding: 20px; text-align: center; box-shadow: 0 4px 20px rgba(233,69,96,0.3); }
        .header h1 { font-size: 28px; margin-bottom: 5px; }
        .header .subtitle { opacity: 0.9; font-size: 14px; }
        .container { max-width: 1400px; margin: 0 auto; padding: 20px; display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px; }
        .card { background: rgba(255,255,255,0.05); border-radius: 16px; padding: 20px; backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.1); }
        .card h2 { color: #e94560; margin-bottom: 15px; font-size: 18px; display: flex; align-items: center; gap: 8px; }
        .card h2::before { content: ''; width: 4px; height: 20px; background: #e94560; border-radius: 2px; }
        .status-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 15px; }
        .status-item { background: rgba(0,0,0,0.2); padding: 15px; border-radius: 10px; text-align: center; }
        .status-item .value { font-size: 24px; font-weight: bold; color: #4ecca3; }
        .status-item .label { font-size: 12px; opacity: 0.7; margin-top: 5px; }
        .player-list { max-height: 200px; overflow-y: auto; }
        .player-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px; margin-bottom: 8px; }
        .player-name { font-weight: bold; }
        .player-health { color: #ff6b6b; }
        .player-pos { font-size: 12px; opacity: 0.7; }
        .btn { padding: 8px 16px; border: none; border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
        .btn-primary { background: #e94560; color: white; }
        .btn-primary:hover { background: #ff6b6b; transform: translateY(-2px); }
        .btn-secondary { background: rgba(255,255,255,0.1); color: white; }
        .btn-secondary:hover { background: rgba(255,255,255,0.2); }
        .btn-group { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 15px; }
        .input-group { display: flex; gap: 8px; margin-top: 10px; }
        .input-group input, .input-group select { flex: 1; padding: 8px 12px; border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; background: rgba(0,0,0,0.2); color: white; font-size: 13px; }
        .input-group input::placeholder { color: rgba(255,255,255,0.5); }
        .event-log { max-height: 300px; overflow-y: auto; font-family: 'Consolas', monospace; font-size: 12px; background: rgba(0,0,0,0.3); padding: 15px; border-radius: 10px; }
        .event-item { padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,0.05); }
        .event-item:last-child { border-bottom: none; }
        .inventory-grid { display: grid; grid-template-columns: repeat(9, 1fr); gap: 4px; margin-top: 10px; }
        .inv-slot { aspect-ratio: 1; background: rgba(0,0,0,0.3); border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 10px; text-align: center; border: 1px solid rgba(255,255,255,0.1); }
        .inv-slot.filled { background: rgba(78,204,163,0.2); border-color: #4ecca3; }
        .scoreboard { background: rgba(0,0,0,0.3); padding: 15px; border-radius: 10px; }
        .scoreboard-title { text-align: center; color: #e94560; font-weight: bold; margin-bottom: 10px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 10px; }
        .scoreboard-line { display: flex; justify-content: space-between; padding: 5px 0; }
        .world-controls { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
        .time-display { font-size: 48px; text-align: center; color: #4ecca3; margin: 15px 0; }
        .weather-icon { font-size: 32px; text-align: center; margin: 10px 0; }
        @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
        .live-indicator { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; }
        .live-dot { width: 8px; height: 8px; background: #4ecca3; border-radius: 50%; animation: pulse 1.5s infinite; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Rubidium Framework Test Dashboard</h1>
        <div class="subtitle">Interactive API Testing Environment <span class="live-indicator"><span class="live-dot"></span> LIVE</span></div>
    </div>
    
    <div class="container">
        <div class="card">
            <h2>Framework Status</h2>
            <div class="status-grid">
                <div class="status-item">
                    <div class="value" id="edition">-</div>
                    <div class="label">Edition</div>
                </div>
                <div class="status-item">
                    <div class="value" id="features">-</div>
                    <div class="label">Features</div>
                </div>
                <div class="status-item">
                    <div class="value" id="items">-</div>
                    <div class="label">Item Types</div>
                </div>
            </div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="runAllTests()">Run All API Tests</button>
                <button class="btn btn-secondary" onclick="refreshStatus()">Refresh Status</button>
            </div>
        </div>
        
        <div class="card">
            <h2>Players</h2>
            <div id="player-list" class="player-list"></div>
            <div class="input-group">
                <input type="text" id="new-player-name" placeholder="Player name...">
                <button class="btn btn-primary" onclick="createPlayer()">Add Player</button>
            </div>
            <div class="btn-group">
                <button class="btn btn-secondary" onclick="teleportRandomPlayer()">Teleport Random</button>
                <button class="btn btn-secondary" onclick="damageRandomPlayer()">Damage Random</button>
            </div>
        </div>
        
        <div class="card">
            <h2>Inventory API</h2>
            <select id="inv-player" class="input-group" style="width:100%;margin-bottom:10px;" onchange="loadInventory()">
                <option value="">Select a player...</option>
            </select>
            <div id="inventory-display" class="inventory-grid"></div>
            <div class="input-group">
                <select id="item-type">
                    <option value="hytale:diamond_sword">Diamond Sword</option>
                    <option value="hytale:iron_pickaxe">Iron Pickaxe</option>
                    <option value="hytale:golden_apple">Golden Apple</option>
                    <option value="hytale:diamond">Diamond</option>
                    <option value="hytale:bread">Bread</option>
                </select>
                <input type="number" id="item-amount" value="1" min="1" max="64" style="width:60px;">
                <button class="btn btn-primary" onclick="giveItem()">Give Item</button>
            </div>
        </div>
        
        <div class="card">
            <h2>World API</h2>
            <div class="time-display" id="world-time">6000</div>
            <div class="weather-icon" id="weather-icon">☀️</div>
            <div class="world-controls">
                <button class="btn btn-secondary" onclick="setTime(0)">Midnight (0)</button>
                <button class="btn btn-secondary" onclick="setTime(6000)">Morning (6000)</button>
                <button class="btn btn-secondary" onclick="setTime(12000)">Noon (12000)</button>
                <button class="btn btn-secondary" onclick="setTime(18000)">Sunset (18000)</button>
            </div>
            <div class="btn-group">
                <button class="btn btn-secondary" onclick="setWeather('clear')">☀️ Clear</button>
                <button class="btn btn-secondary" onclick="setWeather('rain')">🌧️ Rain</button>
                <button class="btn btn-secondary" onclick="setWeather('storm')">⛈️ Storm</button>
            </div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="createExplosion()">💥 Explosion</button>
                <button class="btn btn-primary" onclick="strikeLightning()">⚡ Lightning</button>
            </div>
        </div>
        
        <div class="card">
            <h2>Title API</h2>
            <div class="input-group">
                <input type="text" id="title-text" placeholder="Title text...">
                <input type="text" id="subtitle-text" placeholder="Subtitle...">
            </div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="sendTitle()">Send Title</button>
                <button class="btn btn-secondary" onclick="broadcastTitle()">Broadcast All</button>
            </div>
            <div class="input-group">
                <input type="text" id="actionbar-text" placeholder="Action bar message...">
                <button class="btn btn-primary" onclick="sendActionBar()">Send Action Bar</button>
            </div>
            <div class="input-group">
                <input type="text" id="bossbar-name" placeholder="Boss name...">
                <input type="range" id="bossbar-progress" min="0" max="100" value="100">
                <button class="btn btn-primary" onclick="sendBossBar()">Send Boss Bar</button>
            </div>
        </div>
        
        <div class="card">
            <h2>Scoreboard API</h2>
            <div id="scoreboard" class="scoreboard">
                <div class="scoreboard-title">Server Statistics</div>
                <div id="scoreboard-lines"></div>
            </div>
            <div class="input-group">
                <select id="sb-line">
                    <option value="0">Line 0</option>
                    <option value="1">Line 1</option>
                    <option value="2">Line 2</option>
                </select>
                <input type="text" id="sb-text" placeholder="New text...">
                <button class="btn btn-primary" onclick="updateScoreboard()">Update</button>
            </div>
        </div>
        
        <div class="card" style="grid-column: span 2;">
            <h2>Event Log</h2>
            <div id="event-log" class="event-log"></div>
        </div>
    </div>
    
    <script>
        let players = [];
        
        async function api(path) {
            const res = await fetch(path);
            return res.json();
        }
        
        async function refreshStatus() {
            const status = await api('/api/test');
            document.getElementById('edition').textContent = status.edition;
            document.getElementById('features').textContent = status.features;
            document.getElementById('items').textContent = status.items;
            document.getElementById('world-time').textContent = status.worldTime;
            updateWeatherIcon(status.weather);
        }
        
        async function loadPlayers() {
            players = await api('/api/player/list');
            const list = document.getElementById('player-list');
            const select = document.getElementById('inv-player');
            
            list.innerHTML = players.map(p => `
                <div class="player-item">
                    <div>
                        <div class="player-name">${p.name}</div>
                        <div class="player-pos">Pos: ${p.x.toFixed(0)}, ${p.y.toFixed(0)}, ${p.z.toFixed(0)}</div>
                    </div>
                    <div class="player-health">❤️ ${p.health.toFixed(0)}/20</div>
                </div>
            `).join('');
            
            select.innerHTML = '<option value="">Select a player...</option>' + 
                players.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
        }
        
        async function createPlayer() {
            const name = document.getElementById('new-player-name').value || 'NewPlayer';
            await api('/api/player/create?name=' + encodeURIComponent(name));
            document.getElementById('new-player-name').value = '';
            loadPlayers();
            loadEvents();
        }
        
        async function teleportRandomPlayer() {
            if (players.length === 0) return;
            const p = players[Math.floor(Math.random() * players.length)];
            const x = Math.floor(Math.random() * 200 - 100);
            const z = Math.floor(Math.random() * 200 - 100);
            await api(`/api/player/teleport?id=${p.id}&x=${x}&y=64&z=${z}`);
            loadPlayers();
            loadEvents();
        }
        
        async function damageRandomPlayer() {
            if (players.length === 0) return;
            const p = players[Math.floor(Math.random() * players.length)];
            const health = Math.max(1, p.health - Math.floor(Math.random() * 5 + 1));
            await api(`/api/player/health?id=${p.id}&health=${health}`);
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
                    return `<div class="inv-slot filled" title="${slot.type}">${name}\\n${slot.amount}</div>`;
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
            const icons = { CLEAR: '☀️', RAIN: '🌧️', STORM: '⛈️', SNOW: '❄️', clear: '☀️', rain: '🌧️', storm: '⛈️' };
            document.getElementById('weather-icon').textContent = icons[weather] || '☀️';
        }
        
        async function createExplosion() {
            const x = Math.floor(Math.random() * 100 - 50);
            const z = Math.floor(Math.random() * 100 - 50);
            await api(`/api/world/explosion?x=${x}&y=64&z=${z}&power=4`);
            loadEvents();
        }
        
        async function strikeLightning() {
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
            const lines = document.getElementById('scoreboard-lines');
            lines.innerHTML = sb.lines.map(line => `
                <div class="scoreboard-line">
                    <span>${line.text}</span>
                    <span>${line.score}</span>
                </div>
            `).join('');
        }
        
        async function updateScoreboard() {
            const line = document.getElementById('sb-line').value;
            const text = document.getElementById('sb-text').value || 'Updated';
            await api(`/api/scoreboard/update?line=${line}&text=${encodeURIComponent(text)}`);
            loadScoreboard();
            loadEvents();
        }
        
        async function loadEvents() {
            const events = await api('/api/events');
            document.getElementById('event-log').innerHTML = events.map(e => 
                `<div class="event-item">${e}</div>`
            ).join('');
        }
        
        async function runAllTests() {
            const tests = [
                { name: 'Framework Status', fn: () => api('/api/test') },
                { name: 'Player List', fn: () => api('/api/player/list') },
                { name: 'World Status', fn: () => api('/api/world/status') },
                { name: 'Scoreboard', fn: () => api('/api/scoreboard/get') },
                { name: 'Item Registry', fn: () => api('/api/inventory/items') }
            ];
            
            let passed = 0;
            for (const test of tests) {
                try {
                    const result = await test.fn();
                    if (result && !result.error) passed++;
                } catch (e) {}
            }
            
            alert(`API Tests: ${passed}/${tests.length} passed`);
        }
        
        // Initialize
        refreshStatus();
        loadPlayers();
        loadScoreboard();
        loadEvents();
        loadInventory();
        
        // Auto-refresh
        setInterval(() => {
            refreshStatus();
            loadPlayers();
            loadScoreboard();
            loadEvents();
        }, 5000);
    </script>
</body>
</html>
""";
    }
}
