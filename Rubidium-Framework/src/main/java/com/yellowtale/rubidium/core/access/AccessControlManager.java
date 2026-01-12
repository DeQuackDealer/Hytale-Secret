package com.yellowtale.rubidium.core.access;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccessControlManager {
    
    private final RubidiumLogger logger;
    private final Path dataDirectory;
    
    private volatile AccessMode accessMode = AccessMode.PUBLIC;
    private final Set<String> whitelist = ConcurrentHashMap.newKeySet();
    private final Map<String, BanEntry> banList = new ConcurrentHashMap<>();
    
    private static final String WHITELIST_FILE = "whitelist.txt";
    private static final String BANLIST_FILE = "banlist.json";
    private static final String CONFIG_FILE = "access-config.properties";
    
    public AccessControlManager(RubidiumLogger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    public void initialize() {
        try {
            Files.createDirectories(dataDirectory);
            loadConfig();
            loadWhitelist();
            loadBanList();
            logger.info("Access control initialized in {} mode", accessMode);
        } catch (IOException e) {
            logger.error("Failed to initialize access control", e);
        }
    }
    
    public void shutdown() {
        saveConfig();
        saveWhitelist();
        saveBanList();
    }
    
    public AccessMode getAccessMode() {
        return accessMode;
    }
    
    public void setAccessMode(AccessMode mode) {
        this.accessMode = mode;
        saveConfig();
        logger.info("Access mode changed to {}", mode);
    }
    
    public boolean canJoin(String playerId, String playerName) {
        if (isBanned(playerId)) {
            return false;
        }
        
        if (accessMode == AccessMode.PRIVATE) {
            return isWhitelisted(playerId) || isWhitelisted(playerName);
        }
        
        return true;
    }
    
    public boolean isBanned(String playerId) {
        BanEntry entry = banList.get(playerId.toLowerCase());
        if (entry == null) {
            return false;
        }
        
        if (entry.expiresAt() != null && Instant.now().isAfter(entry.expiresAt())) {
            banList.remove(playerId.toLowerCase());
            saveBanList();
            return false;
        }
        
        return true;
    }
    
    public Optional<BanEntry> getBanEntry(String playerId) {
        return Optional.ofNullable(banList.get(playerId.toLowerCase()));
    }
    
    public boolean isWhitelisted(String playerIdOrName) {
        return whitelist.contains(playerIdOrName.toLowerCase());
    }
    
    public BanResult ban(String playerId, String playerName, String reason, String bannedBy, Instant expiresAt) {
        if (isBanned(playerId)) {
            return new BanResult(false, "Player is already banned");
        }
        
        BanEntry entry = new BanEntry(
            playerId,
            playerName,
            reason,
            bannedBy,
            Instant.now(),
            expiresAt
        );
        
        banList.put(playerId.toLowerCase(), entry);
        saveBanList();
        
        logger.info("Player {} ({}) banned by {} - Reason: {}", 
            playerName, playerId, bannedBy, reason);
        
        return new BanResult(true, "Player banned successfully");
    }
    
    public BanResult tempBan(String playerId, String playerName, String reason, String bannedBy, long durationSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(durationSeconds);
        return ban(playerId, playerName, reason, bannedBy, expiresAt);
    }
    
    public boolean unban(String playerId) {
        BanEntry removed = banList.remove(playerId.toLowerCase());
        if (removed != null) {
            saveBanList();
            logger.info("Player {} unbanned", playerId);
            return true;
        }
        return false;
    }
    
    public WhitelistResult addToWhitelist(String playerIdOrName) {
        if (whitelist.contains(playerIdOrName.toLowerCase())) {
            return new WhitelistResult(false, "Player is already whitelisted");
        }
        
        whitelist.add(playerIdOrName.toLowerCase());
        saveWhitelist();
        logger.info("Player {} added to whitelist", playerIdOrName);
        
        return new WhitelistResult(true, "Player added to whitelist");
    }
    
    public boolean removeFromWhitelist(String playerIdOrName) {
        boolean removed = whitelist.remove(playerIdOrName.toLowerCase());
        if (removed) {
            saveWhitelist();
            logger.info("Player {} removed from whitelist", playerIdOrName);
        }
        return removed;
    }
    
    public Set<String> getWhitelist() {
        return Collections.unmodifiableSet(new HashSet<>(whitelist));
    }
    
    public Collection<BanEntry> getBanList() {
        return Collections.unmodifiableCollection(banList.values());
    }
    
    public void clearWhitelist() {
        whitelist.clear();
        saveWhitelist();
        logger.info("Whitelist cleared");
    }
    
    public void clearBanList() {
        banList.clear();
        saveBanList();
        logger.info("Ban list cleared");
    }
    
    private void loadConfig() {
        Path configPath = dataDirectory.resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try {
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(configPath)) {
                    props.load(is);
                }
                String mode = props.getProperty("access_mode", "PUBLIC");
                accessMode = AccessMode.valueOf(mode.toUpperCase());
            } catch (Exception e) {
                logger.warn("Failed to load access config, using defaults", e);
            }
        }
    }
    
    private void saveConfig() {
        Path configPath = dataDirectory.resolve(CONFIG_FILE);
        try {
            Properties props = new Properties();
            props.setProperty("access_mode", accessMode.name());
            try (OutputStream os = Files.newOutputStream(configPath)) {
                props.store(os, "Rubidium Access Control Configuration");
            }
        } catch (IOException e) {
            logger.error("Failed to save access config", e);
        }
    }
    
    private void loadWhitelist() {
        Path whitelistPath = dataDirectory.resolve(WHITELIST_FILE);
        if (Files.exists(whitelistPath)) {
            try {
                List<String> lines = Files.readAllLines(whitelistPath);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        whitelist.add(trimmed.toLowerCase());
                    }
                }
                logger.debug("Loaded {} whitelisted players", whitelist.size());
            } catch (IOException e) {
                logger.error("Failed to load whitelist", e);
            }
        }
    }
    
    private void saveWhitelist() {
        Path whitelistPath = dataDirectory.resolve(WHITELIST_FILE);
        try {
            List<String> lines = new ArrayList<>();
            lines.add("# Rubidium Whitelist");
            lines.add("# One player ID or name per line");
            lines.addAll(whitelist);
            Files.write(whitelistPath, lines);
        } catch (IOException e) {
            logger.error("Failed to save whitelist", e);
        }
    }
    
    private void loadBanList() {
        Path banPath = dataDirectory.resolve(BANLIST_FILE);
        if (Files.exists(banPath)) {
            try {
                String content = Files.readString(banPath);
                parseBanList(content);
                logger.debug("Loaded {} banned players", banList.size());
            } catch (IOException e) {
                logger.error("Failed to load ban list", e);
            }
        }
    }
    
    private void parseBanList(String json) {
        if (json.isBlank()) return;
        
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start == -1 || end == -1) return;
        
        String arrayContent = json.substring(start + 1, end).trim();
        if (arrayContent.isEmpty()) return;
        
        int braceDepth = 0;
        int objStart = -1;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (braceDepth == 0) objStart = i;
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0 && objStart != -1) {
                    String objStr = arrayContent.substring(objStart, i + 1);
                    BanEntry entry = parseBanEntry(objStr);
                    if (entry != null) {
                        banList.put(entry.playerId().toLowerCase(), entry);
                    }
                    objStart = -1;
                }
            }
        }
    }
    
    private BanEntry parseBanEntry(String json) {
        try {
            String playerId = extractJsonString(json, "playerId");
            String playerName = extractJsonString(json, "playerName");
            String reason = extractJsonString(json, "reason");
            String bannedBy = extractJsonString(json, "bannedBy");
            String bannedAtStr = extractJsonString(json, "bannedAt");
            String expiresAtStr = extractJsonString(json, "expiresAt");
            
            Instant bannedAt = bannedAtStr != null ? Instant.parse(bannedAtStr) : Instant.now();
            Instant expiresAt = expiresAtStr != null && !expiresAtStr.equals("null") ? Instant.parse(expiresAtStr) : null;
            
            return new BanEntry(playerId, playerName, reason, bannedBy, bannedAt, expiresAt);
        } catch (Exception e) {
            logger.warn("Failed to parse ban entry: {}", json);
            return null;
        }
    }
    
    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        
        int start = idx + pattern.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        if (start >= json.length()) return null;
        
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            if (end == -1) return null;
            return json.substring(start + 1, end);
        } else if (json.substring(start).startsWith("null")) {
            return null;
        }
        
        return null;
    }
    
    private void saveBanList() {
        Path banPath = dataDirectory.resolve(BANLIST_FILE);
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            
            Iterator<BanEntry> it = banList.values().iterator();
            while (it.hasNext()) {
                BanEntry entry = it.next();
                sb.append("  {\n");
                sb.append("    \"playerId\": \"").append(escapeJson(entry.playerId())).append("\",\n");
                sb.append("    \"playerName\": \"").append(escapeJson(entry.playerName())).append("\",\n");
                sb.append("    \"reason\": \"").append(escapeJson(entry.reason())).append("\",\n");
                sb.append("    \"bannedBy\": \"").append(escapeJson(entry.bannedBy())).append("\",\n");
                sb.append("    \"bannedAt\": \"").append(entry.bannedAt()).append("\",\n");
                sb.append("    \"expiresAt\": ").append(entry.expiresAt() != null ? "\"" + entry.expiresAt() + "\"" : "null").append("\n");
                sb.append("  }");
                if (it.hasNext()) sb.append(",");
                sb.append("\n");
            }
            
            sb.append("]");
            Files.writeString(banPath, sb.toString());
        } catch (IOException e) {
            logger.error("Failed to save ban list", e);
        }
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    public record BanEntry(
        String playerId,
        String playerName,
        String reason,
        String bannedBy,
        Instant bannedAt,
        Instant expiresAt
    ) {
        public boolean isPermanent() {
            return expiresAt == null;
        }
        
        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }
    
    public record BanResult(boolean success, String message) {}
    public record WhitelistResult(boolean success, String message) {}
}
