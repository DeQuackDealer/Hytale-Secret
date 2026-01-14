package rubidium.teleport;

import rubidium.core.logging.RubidiumLogger;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class TeleportService {
    
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int DEFAULT_COOLDOWN_SECONDS = 3;
    
    private final RubidiumLogger logger;
    private final Path dataDirectory;
    
    private final Map<UUID, Deque<Location>> locationHistory = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Location>> playerHomes = new ConcurrentHashMap<>();
    private final Map<String, Location> warps = new ConcurrentHashMap<>();
    
    private Location spawnLocation;
    private int cooldownSeconds = DEFAULT_COOLDOWN_SECONDS;
    private boolean safetyChecksEnabled = true;
    
    private Function<UUID, Location> playerLocationProvider;
    private Function<UUID, String> playerNameProvider;
    private Function<String, UUID> playerLookupProvider;
    private java.util.function.BiConsumer<UUID, Location> teleportExecutor;
    private Function<Location, Boolean> safetyChecker;
    
    public TeleportService(RubidiumLogger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        loadData();
    }
    
    public void setPlayerLocationProvider(Function<UUID, Location> provider) {
        this.playerLocationProvider = provider;
    }
    
    public void setPlayerNameProvider(Function<UUID, String> provider) {
        this.playerNameProvider = provider;
    }
    
    public void setPlayerLookupProvider(Function<String, UUID> provider) {
        this.playerLookupProvider = provider;
    }
    
    public void setTeleportExecutor(java.util.function.BiConsumer<UUID, Location> executor) {
        this.teleportExecutor = executor;
    }
    
    public void setSafetyChecker(Function<Location, Boolean> checker) {
        this.safetyChecker = checker;
    }
    
    public void setCooldownSeconds(int seconds) {
        this.cooldownSeconds = seconds;
    }
    
    public void setSafetyChecksEnabled(boolean enabled) {
        this.safetyChecksEnabled = enabled;
    }
    
    public TeleportResult teleportToPlayer(UUID player, String targetName, boolean bypassCooldown) {
        if (!bypassCooldown && isOnCooldown(player)) {
            return TeleportResult.onCooldown(getRemainingCooldown(player));
        }
        
        if (playerLookupProvider == null) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Player lookup not configured");
        }
        
        UUID target = playerLookupProvider.apply(targetName);
        if (target == null) {
            return TeleportResult.playerNotFound(targetName);
        }
        
        Location targetLocation = playerLocationProvider.apply(target);
        if (targetLocation == null) {
            return TeleportResult.playerNotFound(targetName);
        }
        
        return teleportTo(player, targetLocation, bypassCooldown);
    }
    
    public TeleportResult teleportPlayerToTarget(UUID player, String targetName, boolean bypassCooldown) {
        if (playerLookupProvider == null) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Player lookup not configured");
        }
        
        UUID target = playerLookupProvider.apply(targetName);
        if (target == null) {
            return TeleportResult.playerNotFound(targetName);
        }
        
        Location targetLocation = playerLocationProvider.apply(target);
        if (targetLocation == null) {
            return TeleportResult.playerNotFound(targetName);
        }
        
        return teleportTo(player, targetLocation, bypassCooldown);
    }
    
    public TeleportResult teleportTo(UUID player, Location destination, boolean bypassCooldown) {
        if (!bypassCooldown && isOnCooldown(player)) {
            return TeleportResult.onCooldown(getRemainingCooldown(player));
        }
        
        if (safetyChecksEnabled && safetyChecker != null && !safetyChecker.apply(destination)) {
            return TeleportResult.unsafeLocation();
        }
        
        Location currentLocation = playerLocationProvider != null ? 
            playerLocationProvider.apply(player) : null;
        if (currentLocation != null) {
            saveToHistory(player, currentLocation);
        }
        
        if (teleportExecutor != null) {
            teleportExecutor.accept(player, destination);
        }
        
        if (!bypassCooldown) {
            setCooldown(player);
        }
        
        return TeleportResult.success(destination);
    }
    
    public TeleportResult teleportToCoordinates(UUID player, String world, double x, double y, double z, 
            boolean bypassCooldown) {
        Location destination = new Location(world, x, y, z);
        return teleportTo(player, destination, bypassCooldown);
    }
    
    public TeleportResult teleportHere(UUID player, String targetName, boolean bypassCooldown) {
        if (playerLookupProvider == null || playerLocationProvider == null) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Player lookup not configured");
        }
        
        UUID target = playerLookupProvider.apply(targetName);
        if (target == null) {
            return TeleportResult.playerNotFound(targetName);
        }
        
        Location myLocation = playerLocationProvider.apply(player);
        if (myLocation == null) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Could not get your location");
        }
        
        Location targetCurrentLoc = playerLocationProvider.apply(target);
        if (targetCurrentLoc != null) {
            saveToHistory(target, targetCurrentLoc);
        }
        
        if (teleportExecutor != null) {
            teleportExecutor.accept(target, myLocation);
        }
        
        String targetNameStr = playerNameProvider != null ? playerNameProvider.apply(target) : targetName;
        return TeleportResult.success(myLocation, "Teleported " + targetNameStr + " to you");
    }
    
    public List<TeleportResult> teleportAll(UUID sender, Collection<UUID> players, boolean bypassCooldown) {
        if (playerLocationProvider == null) {
            return List.of(TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Player lookup not configured"));
        }
        
        Location senderLocation = playerLocationProvider.apply(sender);
        if (senderLocation == null) {
            return List.of(TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Could not get your location"));
        }
        
        List<TeleportResult> results = new ArrayList<>();
        for (UUID player : players) {
            if (player.equals(sender)) continue;
            
            Location currentLoc = playerLocationProvider.apply(player);
            if (currentLoc != null) {
                saveToHistory(player, currentLoc);
            }
            
            if (teleportExecutor != null) {
                teleportExecutor.accept(player, senderLocation);
            }
            
            String name = playerNameProvider != null ? playerNameProvider.apply(player) : player.toString();
            results.add(TeleportResult.success(senderLocation, "Teleported " + name));
        }
        
        return results;
    }
    
    public TeleportResult back(UUID player) {
        var history = locationHistory.get(player);
        if (history == null || history.isEmpty()) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "No previous location found");
        }
        
        Location previousLocation = history.pollLast();
        return teleportTo(player, previousLocation, true);
    }
    
    public TeleportResult teleportToSpawn(UUID player) {
        if (spawnLocation == null) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Spawn location not set");
        }
        return teleportTo(player, spawnLocation, false);
    }
    
    public void setSpawn(Location location) {
        this.spawnLocation = location;
        saveData();
        logger.info("Spawn location set to " + location.toReadableString());
    }
    
    public Location getSpawn() {
        return spawnLocation;
    }
    
    public TeleportResult setHome(UUID player, String name, Location location) {
        playerHomes.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
            .put(name.toLowerCase(), location);
        saveData();
        return TeleportResult.success(location, "Home '" + name + "' set");
    }
    
    public TeleportResult teleportToHome(UUID player, String name) {
        var homes = playerHomes.get(player);
        if (homes == null || homes.isEmpty()) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "You have no homes set");
        }
        
        Location home = homes.get(name.toLowerCase());
        if (home == null) {
            if (name.equals("home") && homes.size() == 1) {
                home = homes.values().iterator().next();
            } else {
                return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                    "Home '" + name + "' not found");
            }
        }
        
        return teleportTo(player, home, false);
    }
    
    public boolean deleteHome(UUID player, String name) {
        var homes = playerHomes.get(player);
        if (homes == null) return false;
        boolean removed = homes.remove(name.toLowerCase()) != null;
        if (removed) saveData();
        return removed;
    }
    
    public Map<String, Location> getHomes(UUID player) {
        return playerHomes.getOrDefault(player, Map.of());
    }
    
    public TeleportResult setWarp(String name, Location location) {
        warps.put(name.toLowerCase(), location);
        saveData();
        return TeleportResult.success(location, "Warp '" + name + "' created");
    }
    
    public TeleportResult teleportToWarp(UUID player, String name) {
        Location warp = warps.get(name.toLowerCase());
        if (warp == null) {
            return TeleportResult.failure(TeleportResult.TeleportFailReason.UNKNOWN, 
                "Warp '" + name + "' not found");
        }
        return teleportTo(player, warp, false);
    }
    
    public boolean deleteWarp(String name) {
        boolean removed = warps.remove(name.toLowerCase()) != null;
        if (removed) saveData();
        return removed;
    }
    
    public Map<String, Location> getWarps() {
        return Collections.unmodifiableMap(warps);
    }
    
    public void recordLocationChange(UUID player, Location from, LocationChangeReason reason) {
        if (reason == LocationChangeReason.DEATH || reason == LocationChangeReason.TELEPORT) {
            saveToHistory(player, from);
        }
    }
    
    private void saveToHistory(UUID player, Location location) {
        var history = locationHistory.computeIfAbsent(player, k -> new LinkedList<>());
        history.addLast(location);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.pollFirst();
        }
    }
    
    private boolean isOnCooldown(UUID player) {
        Instant cooldownEnd = cooldowns.get(player);
        return cooldownEnd != null && Instant.now().isBefore(cooldownEnd);
    }
    
    private long getRemainingCooldown(UUID player) {
        Instant cooldownEnd = cooldowns.get(player);
        if (cooldownEnd == null) return 0;
        return Math.max(0, Duration.between(Instant.now(), cooldownEnd).getSeconds());
    }
    
    private void setCooldown(UUID player) {
        cooldowns.put(player, Instant.now().plusSeconds(cooldownSeconds));
    }
    
    public void clearCooldown(UUID player) {
        cooldowns.remove(player);
    }
    
    private void loadData() {
        try {
            Files.createDirectories(dataDirectory);
            
            Path spawnFile = dataDirectory.resolve("spawn.dat");
            if (Files.exists(spawnFile)) {
                String data = Files.readString(spawnFile).trim();
                spawnLocation = Location.deserialize(data);
            }
            
            Path warpsFile = dataDirectory.resolve("warps.dat");
            if (Files.exists(warpsFile)) {
                for (String line : Files.readAllLines(warpsFile)) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        Location loc = Location.deserialize(parts[1]);
                        if (loc != null) {
                            warps.put(parts[0].toLowerCase(), loc);
                        }
                    }
                }
            }
            
            Path homesDir = dataDirectory.resolve("homes");
            if (Files.exists(homesDir)) {
                try (var stream = Files.list(homesDir)) {
                    stream.filter(p -> p.toString().endsWith(".dat"))
                        .forEach(this::loadPlayerHomes);
                }
            }
            
            logger.info("Loaded teleport data: " + warps.size() + " warps, spawn=" + 
                (spawnLocation != null));
        } catch (IOException e) {
            logger.warn("Failed to load teleport data: " + e.getMessage());
        }
    }
    
    private void loadPlayerHomes(Path file) {
        try {
            String filename = file.getFileName().toString();
            UUID playerId = UUID.fromString(filename.replace(".dat", ""));
            Map<String, Location> homes = new ConcurrentHashMap<>();
            
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    Location loc = Location.deserialize(parts[1]);
                    if (loc != null) {
                        homes.put(parts[0].toLowerCase(), loc);
                    }
                }
            }
            
            if (!homes.isEmpty()) {
                playerHomes.put(playerId, homes);
            }
        } catch (Exception e) {
            logger.warn("Failed to load homes from " + file + ": " + e.getMessage());
        }
    }
    
    private void saveData() {
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dataDirectory);
                
                if (spawnLocation != null) {
                    Files.writeString(dataDirectory.resolve("spawn.dat"), 
                        spawnLocation.serialize());
                }
                
                StringBuilder warpsData = new StringBuilder();
                for (var entry : warps.entrySet()) {
                    warpsData.append(entry.getKey()).append("=")
                        .append(entry.getValue().serialize()).append("\n");
                }
                Files.writeString(dataDirectory.resolve("warps.dat"), warpsData.toString());
                
                Path homesDir = dataDirectory.resolve("homes");
                Files.createDirectories(homesDir);
                for (var entry : playerHomes.entrySet()) {
                    StringBuilder homeData = new StringBuilder();
                    for (var home : entry.getValue().entrySet()) {
                        homeData.append(home.getKey()).append("=")
                            .append(home.getValue().serialize()).append("\n");
                    }
                    Files.writeString(homesDir.resolve(entry.getKey() + ".dat"), 
                        homeData.toString());
                }
            } catch (IOException e) {
                logger.warn("Failed to save teleport data: " + e.getMessage());
            }
        });
    }
    
    public enum LocationChangeReason {
        DEATH,
        TELEPORT,
        COMMAND,
        PORTAL,
        RESPAWN
    }
}
