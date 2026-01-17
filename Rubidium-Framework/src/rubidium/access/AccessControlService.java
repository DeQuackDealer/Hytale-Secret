package rubidium.access;

import rubidium.hytale.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class AccessControlService {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );
    
    private static final Pattern UUID_NO_DASHES = Pattern.compile(
        "[0-9a-fA-F]{32}"
    );
    
    public record AccessEntry(
        UUID uuid,
        String username,
        String reason,
        Instant addedAt,
        String addedBy,
        Instant expiresAt
    ) {
        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
        
        public boolean isPermanent() {
            return expiresAt == null;
        }
    }
    
    public record AccessCheckResult(
        boolean permitted,
        AccessType listType,
        AccessEntry entry,
        String message
    ) {
        public boolean allowed() {
            return permitted;
        }
        
        public static AccessCheckResult allow() {
            return new AccessCheckResult(true, null, null, null);
        }
        
        public static AccessCheckResult denied(AccessType type, AccessEntry entry, String message) {
            return new AccessCheckResult(false, type, entry, message);
        }
    }
    
    public enum AccessType {
        WHITELIST,
        BLACKLIST
    }
    
    public enum AccessMode {
        DISABLED,
        WHITELIST_ONLY,
        BLACKLIST_ONLY,
        WHITELIST_PRIORITY,
        BLACKLIST_PRIORITY
    }
    
    private final RubidiumLogger logger;
    private final Path dataDirectory;
    private final HttpClient httpClient;
    private final UUIDScraper uuidScraper;
    
    private final Map<UUID, AccessEntry> whitelist = new ConcurrentHashMap<>();
    private final Map<UUID, AccessEntry> blacklist = new ConcurrentHashMap<>();
    private final List<Consumer<AccessAuditEvent>> auditListeners = new CopyOnWriteArrayList<>();
    
    private volatile AccessMode mode = AccessMode.DISABLED;
    private volatile String whitelistMessage = "You are not whitelisted on this server.";
    private volatile String blacklistMessage = "You have been banned from this server.";
    
    public AccessControlService(RubidiumLogger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newCachedThreadPool())
            .build();
        this.uuidScraper = new UUIDScraper(httpClient, logger);
        
        loadFromDisk();
    }
    
    public AccessCheckResult checkAccess(UUID uuid, String username) {
        cleanupExpired();
        
        return switch (mode) {
            case DISABLED -> AccessCheckResult.allow();
            
            case WHITELIST_ONLY -> {
                var entry = whitelist.get(uuid);
                if (entry != null && !entry.isExpired()) {
                    yield AccessCheckResult.allow();
                }
                yield AccessCheckResult.denied(AccessType.WHITELIST, null, whitelistMessage);
            }
            
            case BLACKLIST_ONLY -> {
                var entry = blacklist.get(uuid);
                if (entry != null && !entry.isExpired()) {
                    yield AccessCheckResult.denied(AccessType.BLACKLIST, entry, 
                        entry.reason() != null ? entry.reason() : blacklistMessage);
                }
                yield AccessCheckResult.allow();
            }
            
            case WHITELIST_PRIORITY -> {
                var whiteEntry = whitelist.get(uuid);
                if (whiteEntry != null && !whiteEntry.isExpired()) {
                    yield AccessCheckResult.allow();
                }
                var blackEntry = blacklist.get(uuid);
                if (blackEntry != null && !blackEntry.isExpired()) {
                    yield AccessCheckResult.denied(AccessType.BLACKLIST, blackEntry,
                        blackEntry.reason() != null ? blackEntry.reason() : blacklistMessage);
                }
                yield AccessCheckResult.denied(AccessType.WHITELIST, null, whitelistMessage);
            }
            
            case BLACKLIST_PRIORITY -> {
                var blackEntry = blacklist.get(uuid);
                if (blackEntry != null && !blackEntry.isExpired()) {
                    yield AccessCheckResult.denied(AccessType.BLACKLIST, blackEntry,
                        blackEntry.reason() != null ? blackEntry.reason() : blacklistMessage);
                }
                var whiteEntry = whitelist.get(uuid);
                if (whiteEntry != null && !whiteEntry.isExpired()) {
                    yield AccessCheckResult.allow();
                }
                yield AccessCheckResult.allow();
            }
        };
    }
    
    public CompletableFuture<Boolean> addToWhitelist(String usernameOrUuid, String addedBy, String reason, Duration duration) {
        return resolveUUID(usernameOrUuid).thenApply(resolved -> {
            if (resolved == null) return false;
            
            var entry = new AccessEntry(
                resolved.uuid(),
                resolved.username(),
                reason,
                Instant.now(),
                addedBy,
                duration != null ? Instant.now().plus(duration) : null
            );
            
            whitelist.put(resolved.uuid(), entry);
            auditLog(new AccessAuditEvent(AccessType.WHITELIST, "ADD", entry, addedBy));
            saveToDisk();
            return true;
        });
    }
    
    public CompletableFuture<Boolean> addToBlacklist(String usernameOrUuid, String addedBy, String reason, Duration duration) {
        return resolveUUID(usernameOrUuid).thenApply(resolved -> {
            if (resolved == null) return false;
            
            var entry = new AccessEntry(
                resolved.uuid(),
                resolved.username(),
                reason,
                Instant.now(),
                addedBy,
                duration != null ? Instant.now().plus(duration) : null
            );
            
            blacklist.put(resolved.uuid(), entry);
            auditLog(new AccessAuditEvent(AccessType.BLACKLIST, "ADD", entry, addedBy));
            saveToDisk();
            return true;
        });
    }
    
    public boolean removeFromWhitelist(UUID uuid, String removedBy) {
        var entry = whitelist.remove(uuid);
        if (entry != null) {
            auditLog(new AccessAuditEvent(AccessType.WHITELIST, "REMOVE", entry, removedBy));
            saveToDisk();
            return true;
        }
        return false;
    }
    
    public boolean removeFromBlacklist(UUID uuid, String removedBy) {
        var entry = blacklist.remove(uuid);
        if (entry != null) {
            auditLog(new AccessAuditEvent(AccessType.BLACKLIST, "REMOVE", entry, removedBy));
            saveToDisk();
            return true;
        }
        return false;
    }
    
    public CompletableFuture<List<ResolvedPlayer>> scrapeUUIDs(List<String> usernames) {
        return uuidScraper.bulkResolve(usernames);
    }
    
    public CompletableFuture<Integer> importFromUrl(String url, AccessType type, String importedBy) {
        return uuidScraper.scrapeFromUrl(url).thenApply(players -> {
            int count = 0;
            for (var player : players) {
                var entry = new AccessEntry(
                    player.uuid(),
                    player.username(),
                    "Imported from " + url,
                    Instant.now(),
                    importedBy,
                    null
                );
                
                if (type == AccessType.WHITELIST) {
                    whitelist.put(player.uuid(), entry);
                } else {
                    blacklist.put(player.uuid(), entry);
                }
                count++;
            }
            
            if (count > 0) {
                auditLog(new AccessAuditEvent(type, "IMPORT", null, importedBy + " (count: " + count + ")"));
                saveToDisk();
            }
            return count;
        });
    }
    
    public void setMode(AccessMode mode) {
        this.mode = mode;
        logger.info("Access control mode set to: " + mode);
    }
    
    public AccessMode getMode() {
        return mode;
    }
    
    public Collection<AccessEntry> getWhitelist() {
        cleanupExpired();
        return Collections.unmodifiableCollection(whitelist.values());
    }
    
    public Collection<AccessEntry> getBlacklist() {
        cleanupExpired();
        return Collections.unmodifiableCollection(blacklist.values());
    }
    
    public void registerAuditListener(Consumer<AccessAuditEvent> listener) {
        auditListeners.add(listener);
    }
    
    private CompletableFuture<ResolvedPlayer> resolveUUID(String input) {
        var normalizedInput = input.replace("-", "");
        
        if (UUID_NO_DASHES.matcher(normalizedInput).matches()) {
            var uuid = insertDashes(normalizedInput);
            return CompletableFuture.completedFuture(new ResolvedPlayer(UUID.fromString(uuid), input));
        }
        
        if (UUID_PATTERN.matcher(input).matches()) {
            return CompletableFuture.completedFuture(new ResolvedPlayer(UUID.fromString(input), input));
        }
        
        return uuidScraper.resolveUsername(input);
    }
    
    private String insertDashes(String uuid) {
        return uuid.substring(0, 8) + "-" +
               uuid.substring(8, 12) + "-" +
               uuid.substring(12, 16) + "-" +
               uuid.substring(16, 20) + "-" +
               uuid.substring(20);
    }
    
    private void cleanupExpired() {
        whitelist.entrySet().removeIf(e -> e.getValue().isExpired());
        blacklist.entrySet().removeIf(e -> e.getValue().isExpired());
    }
    
    private void auditLog(AccessAuditEvent event) {
        logger.info("[AccessControl] " + event.action() + " " + event.listType() + 
            (event.entry() != null ? " - " + event.entry().username() : "") +
            " by " + event.performedBy());
        
        for (var listener : auditListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.warn("Audit listener failed: " + e.getMessage());
            }
        }
    }
    
    private void loadFromDisk() {
        try {
            var whitelistFile = validateAndResolvePath("whitelist.json");
            var blacklistFile = validateAndResolvePath("blacklist.json");
            
            if (whitelistFile != null && Files.exists(whitelistFile)) {
                loadEntries(whitelistFile, whitelist);
            }
            if (blacklistFile != null && Files.exists(blacklistFile)) {
                loadEntries(blacklistFile, blacklist);
            }
            
            logger.info("Loaded " + whitelist.size() + " whitelist entries, " + 
                       blacklist.size() + " blacklist entries");
        } catch (Exception e) {
            logger.error("Failed to load access control data: " + e.getMessage());
        }
    }
    
    private Path validateAndResolvePath(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        var sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "");
        if (!sanitized.equals(filename)) {
            logger.warn("Invalid filename rejected: " + filename);
            return null;
        }
        
        if (filename.contains("..") || filename.startsWith("/") || filename.contains(":")) {
            logger.warn("Path traversal attempt blocked: " + filename);
            return null;
        }
        
        var resolved = dataDirectory.resolve(filename).normalize();
        if (!resolved.startsWith(dataDirectory.normalize())) {
            logger.warn("Path traversal attempt blocked: " + filename);
            return null;
        }
        
        return resolved;
    }
    
    private void loadEntries(Path file, Map<UUID, AccessEntry> target) throws IOException {
        var content = Files.readString(file);
        var lines = content.split("\n");
        for (var line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            var parts = line.split(",", 6);
            if (parts.length >= 2) {
                try {
                    var uuid = UUID.fromString(parts[0].trim());
                    var username = parts[1].trim();
                    var reason = parts.length > 2 ? parts[2].trim() : null;
                    var addedAt = parts.length > 3 ? Instant.parse(parts[3].trim()) : Instant.now();
                    var addedBy = parts.length > 4 ? parts[4].trim() : "system";
                    var expiresAt = parts.length > 5 && !parts[5].trim().equals("permanent") 
                        ? Instant.parse(parts[5].trim()) : null;
                    
                    target.put(uuid, new AccessEntry(uuid, username, reason, addedAt, addedBy, expiresAt));
                } catch (Exception e) {
                    logger.warn("Invalid access entry: " + line);
                }
            }
        }
    }
    
    private void saveToDisk() {
        try {
            Files.createDirectories(dataDirectory);
            saveEntries(dataDirectory.resolve("whitelist.json"), whitelist);
            saveEntries(dataDirectory.resolve("blacklist.json"), blacklist);
        } catch (Exception e) {
            logger.error("Failed to save access control data: " + e.getMessage());
        }
    }
    
    private void saveEntries(Path file, Map<UUID, AccessEntry> entries) throws IOException {
        var sb = new StringBuilder();
        sb.append("# Access Control List\n");
        sb.append("# Format: UUID,username,reason,addedAt,addedBy,expiresAt\n\n");
        
        for (var entry : entries.values()) {
            sb.append(entry.uuid()).append(",");
            sb.append(entry.username()).append(",");
            sb.append(entry.reason() != null ? entry.reason() : "").append(",");
            sb.append(entry.addedAt()).append(",");
            sb.append(entry.addedBy()).append(",");
            sb.append(entry.expiresAt() != null ? entry.expiresAt() : "permanent");
            sb.append("\n");
        }
        
        Files.writeString(file, sb.toString());
    }
    
    public record ResolvedPlayer(UUID uuid, String username) {}
    
    public record AccessAuditEvent(
        AccessType listType,
        String action,
        AccessEntry entry,
        String performedBy
    ) {}
}
