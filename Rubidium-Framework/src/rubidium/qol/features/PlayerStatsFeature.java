package rubidium.qol.features;

import rubidium.core.logging.RubidiumLogger;
import rubidium.qol.QoLFeature;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsFeature extends QoLFeature {
    
    public record StatsConfig(
        boolean trackPlaytime,
        boolean trackJoins,
        boolean trackLastSeen,
        boolean persistStats,
        Path statsFile
    ) {
        public static StatsConfig defaults() {
            return new StatsConfig(
                true,
                true,
                true,
                true,
                Path.of("data/player-stats.properties")
            );
        }
    }
    
    public record PlayerStats(
        String playerId,
        Duration totalPlaytime,
        int totalJoins,
        Instant firstJoin,
        Instant lastSeen,
        Instant currentSessionStart
    ) {
        public Duration getCurrentSessionDuration() {
            if (currentSessionStart == null) return Duration.ZERO;
            return Duration.between(currentSessionStart, Instant.now());
        }
        
        public Duration getTotalPlaytimeIncludingSession() {
            return totalPlaytime.plus(getCurrentSessionDuration());
        }
    }
    
    private final Map<String, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private StatsConfig config;
    
    public PlayerStatsFeature(RubidiumLogger logger) {
        super("player-stats", "Player Statistics", 
            "Tracks player playtime, join counts, and activity history",
            logger);
        this.config = StatsConfig.defaults();
    }
    
    public void setConfig(StatsConfig config) {
        this.config = config;
    }
    
    public StatsConfig getConfig() {
        return config;
    }
    
    @Override
    protected void onEnable() {
        if (config.persistStats()) {
            loadStats();
        }
        logger.debug("Player stats tracking enabled");
    }
    
    @Override
    protected void onDisable() {
        if (config.persistStats()) {
            saveStats();
        }
        playerStats.clear();
    }
    
    public void onPlayerJoin(String playerId) {
        if (!enabled) return;
        
        Instant now = Instant.now();
        PlayerStats existing = playerStats.get(playerId);
        
        if (existing != null) {
            playerStats.put(playerId, new PlayerStats(
                playerId,
                existing.totalPlaytime(),
                existing.totalJoins() + (config.trackJoins() ? 1 : 0),
                existing.firstJoin(),
                now,
                now
            ));
        } else {
            playerStats.put(playerId, new PlayerStats(
                playerId,
                Duration.ZERO,
                1,
                now,
                now,
                now
            ));
        }
    }
    
    public void onPlayerLeave(String playerId) {
        if (!enabled) return;
        
        PlayerStats stats = playerStats.get(playerId);
        if (stats == null) return;
        
        Duration sessionDuration = stats.getCurrentSessionDuration();
        Instant now = Instant.now();
        
        playerStats.put(playerId, new PlayerStats(
            playerId,
            stats.totalPlaytime().plus(sessionDuration),
            stats.totalJoins(),
            stats.firstJoin(),
            now,
            null
        ));
        
        if (config.persistStats()) {
            saveStats();
        }
    }
    
    public Optional<PlayerStats> getStats(String playerId) {
        return Optional.ofNullable(playerStats.get(playerId));
    }
    
    public Map<String, PlayerStats> getAllStats() {
        return Collections.unmodifiableMap(playerStats);
    }
    
    public List<PlayerStats> getTopByPlaytime(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> b.getTotalPlaytimeIncludingSession().compareTo(a.getTotalPlaytimeIncludingSession()))
            .limit(limit)
            .toList();
    }
    
    public List<PlayerStats> getTopByJoins(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> Integer.compare(b.totalJoins(), a.totalJoins()))
            .limit(limit)
            .toList();
    }
    
    public List<PlayerStats> getOnlinePlayers() {
        return playerStats.values().stream()
            .filter(s -> s.currentSessionStart() != null)
            .toList();
    }
    
    public String formatStats(String playerId) {
        PlayerStats stats = playerStats.get(playerId);
        if (stats == null) {
            return "&cNo stats found for " + playerId;
        }
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
        
        StringBuilder sb = new StringBuilder();
        sb.append("&7Stats for &f").append(playerId).append("&7:\n");
        
        if (config.trackPlaytime()) {
            Duration playtime = stats.getTotalPlaytimeIncludingSession();
            sb.append("  &7Playtime: &f").append(formatDuration(playtime)).append("\n");
        }
        
        if (config.trackJoins()) {
            sb.append("  &7Total joins: &f").append(stats.totalJoins()).append("\n");
        }
        
        if (stats.firstJoin() != null) {
            sb.append("  &7First joined: &f").append(fmt.format(stats.firstJoin())).append("\n");
        }
        
        if (config.trackLastSeen() && stats.lastSeen() != null) {
            sb.append("  &7Last seen: &f").append(fmt.format(stats.lastSeen())).append("\n");
        }
        
        if (stats.currentSessionStart() != null) {
            sb.append("  &7Online for: &a").append(formatDuration(stats.getCurrentSessionDuration()));
        }
        
        return sb.toString();
    }
    
    private String formatDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    private void loadStats() {
        Path path = config.statsFile();
        if (!Files.exists(path)) return;
        
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            Properties props = new Properties();
            props.load(reader);
            
            Set<String> playerIds = new HashSet<>();
            for (String key : props.stringPropertyNames()) {
                String playerId = key.split("\\.")[0];
                playerIds.add(playerId);
            }
            
            for (String playerId : playerIds) {
                try {
                    Duration playtime = Duration.ofSeconds(
                        Long.parseLong(props.getProperty(playerId + ".playtime", "0")));
                    int joins = Integer.parseInt(props.getProperty(playerId + ".joins", "0"));
                    
                    Instant firstJoin = null;
                    String firstJoinStr = props.getProperty(playerId + ".firstJoin");
                    if (firstJoinStr != null) {
                        firstJoin = Instant.parse(firstJoinStr);
                    }
                    
                    Instant lastSeen = null;
                    String lastSeenStr = props.getProperty(playerId + ".lastSeen");
                    if (lastSeenStr != null) {
                        lastSeen = Instant.parse(lastSeenStr);
                    }
                    
                    playerStats.put(playerId, new PlayerStats(
                        playerId, playtime, joins, firstJoin, lastSeen, null));
                } catch (Exception e) {
                    logger.warn("Failed to load stats for {}: {}", playerId, e.getMessage());
                }
            }
            
            logger.debug("Loaded stats for {} players", playerStats.size());
        } catch (IOException e) {
            logger.error("Failed to load player stats: {}", e.getMessage());
        }
    }
    
    private void saveStats() {
        Path path = config.statsFile();
        
        try {
            Files.createDirectories(path.getParent());
            
            Properties props = new Properties();
            for (PlayerStats stats : playerStats.values()) {
                String id = stats.playerId();
                props.setProperty(id + ".playtime", String.valueOf(stats.totalPlaytime().toSeconds()));
                props.setProperty(id + ".joins", String.valueOf(stats.totalJoins()));
                if (stats.firstJoin() != null) {
                    props.setProperty(id + ".firstJoin", stats.firstJoin().toString());
                }
                if (stats.lastSeen() != null) {
                    props.setProperty(id + ".lastSeen", stats.lastSeen().toString());
                }
            }
            
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                props.store(writer, "Rubidium Player Statistics");
            }
            
            logger.debug("Saved stats for {} players", playerStats.size());
        } catch (IOException e) {
            logger.error("Failed to save player stats: {}", e.getMessage());
        }
    }
}
