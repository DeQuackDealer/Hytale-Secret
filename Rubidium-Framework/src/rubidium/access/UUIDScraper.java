package rubidium.access;

import rubidium.access.AccessControlService.ResolvedPlayer;
import rubidium.core.logging.RubidiumLogger;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class UUIDScraper {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"
    );
    
    private static final Pattern UUID_NO_DASHES_PATTERN = Pattern.compile(
        "\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\""
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "\"name\"\\s*:\\s*\"([a-zA-Z0-9_]{3,16})\""
    );
    
    public enum Provider {
        MOJANG_API("https://api.mojang.com/users/profiles/minecraft/"),
        PLAYERDB("https://playerdb.co/api/player/minecraft/"),
        MCUUID("https://mcuuid.net/?q="),
        ASHCON("https://api.ashcon.app/mojang/v2/user/");
        
        private final String baseUrl;
        
        Provider(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
    }
    
    private final HttpClient httpClient;
    private final RubidiumLogger logger;
    private final Map<String, ResolvedPlayer> cache = new ConcurrentHashMap<>();
    private final Semaphore rateLimiter;
    private final List<Provider> providers;
    
    public UUIDScraper(HttpClient httpClient, RubidiumLogger logger) {
        this.httpClient = httpClient;
        this.logger = logger;
        this.rateLimiter = new Semaphore(10);
        this.providers = List.of(Provider.MOJANG_API, Provider.ASHCON, Provider.PLAYERDB);
    }
    
    public CompletableFuture<ResolvedPlayer> resolveUsername(String username) {
        var cached = cache.get(username.toLowerCase());
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return tryProviders(username, 0);
    }
    
    private CompletableFuture<ResolvedPlayer> tryProviders(String username, int providerIndex) {
        if (providerIndex >= providers.size()) {
            return CompletableFuture.completedFuture(null);
        }
        
        var provider = providers.get(providerIndex);
        
        return fetchFromProvider(provider, username)
            .thenCompose(result -> {
                if (result != null) {
                    cache.put(username.toLowerCase(), result);
                    return CompletableFuture.completedFuture(result);
                }
                return tryProviders(username, providerIndex + 1);
            })
            .exceptionally(e -> {
                logger.warn("Provider " + provider + " failed for " + username + ": " + e.getMessage());
                return null;
            });
    }
    
    private CompletableFuture<ResolvedPlayer> fetchFromProvider(Provider provider, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                rateLimiter.acquire();
                try {
                    var url = provider.getBaseUrl() + username;
                    var request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Rubidium-Server/1.0")
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                    
                    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        return parseResponse(provider, response.body(), username);
                    }
                    return null;
                } finally {
                    rateLimiter.release();
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    private ResolvedPlayer parseResponse(Provider provider, String body, String username) {
        try {
            return switch (provider) {
                case MOJANG_API -> parseMojangResponse(body);
                case ASHCON -> parseAshconResponse(body);
                case PLAYERDB -> parsePlayerDBResponse(body);
                default -> null;
            };
        } catch (Exception e) {
            logger.warn("Failed to parse response from " + provider + ": " + e.getMessage());
            return null;
        }
    }
    
    private ResolvedPlayer parseMojangResponse(String body) {
        var uuidMatcher = UUID_NO_DASHES_PATTERN.matcher(body);
        var nameMatcher = USERNAME_PATTERN.matcher(body);
        
        if (uuidMatcher.find() && nameMatcher.find()) {
            var uuidStr = insertDashes(uuidMatcher.group(1));
            return new ResolvedPlayer(UUID.fromString(uuidStr), nameMatcher.group(1));
        }
        return null;
    }
    
    private ResolvedPlayer parseAshconResponse(String body) {
        var uuidMatcher = UUID_PATTERN.matcher(body);
        var nameMatcher = USERNAME_PATTERN.matcher(body);
        
        if (uuidMatcher.find() && nameMatcher.find()) {
            return new ResolvedPlayer(UUID.fromString(uuidMatcher.group(1)), nameMatcher.group(1));
        }
        return null;
    }
    
    private ResolvedPlayer parsePlayerDBResponse(String body) {
        var uuidMatcher = Pattern.compile("\"raw_id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"").matcher(body);
        var nameMatcher = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        
        if (uuidMatcher.find() && nameMatcher.find()) {
            return new ResolvedPlayer(UUID.fromString(uuidMatcher.group(1)), nameMatcher.group(1));
        }
        return null;
    }
    
    public CompletableFuture<List<ResolvedPlayer>> bulkResolve(List<String> usernames) {
        var futures = usernames.stream()
            .map(this::resolveUsername)
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList());
    }
    
    public CompletableFuture<List<ResolvedPlayer>> scrapeFromUrl(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Rubidium-Server/1.0")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
                
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    logger.warn("Failed to fetch URL " + url + ": " + response.statusCode());
                    return List.<ResolvedPlayer>of();
                }
                
                return extractPlayersFromContent(response.body());
            } catch (Exception e) {
                logger.error("Error scraping URL " + url + ": " + e.getMessage());
                return List.of();
            }
        });
    }
    
    private List<ResolvedPlayer> extractPlayersFromContent(String content) {
        var players = new ArrayList<ResolvedPlayer>();
        
        var uuidMatcher = UUID_PATTERN.matcher(content);
        while (uuidMatcher.find()) {
            try {
                var uuid = UUID.fromString(uuidMatcher.group(1));
                players.add(new ResolvedPlayer(uuid, "unknown"));
            } catch (IllegalArgumentException ignored) {}
        }
        
        var noDashesMatcher = Pattern.compile("(?<![0-9a-fA-F])([0-9a-fA-F]{32})(?![0-9a-fA-F])").matcher(content);
        while (noDashesMatcher.find()) {
            try {
                var uuidStr = insertDashes(noDashesMatcher.group(1));
                var uuid = UUID.fromString(uuidStr);
                if (players.stream().noneMatch(p -> p.uuid().equals(uuid))) {
                    players.add(new ResolvedPlayer(uuid, "unknown"));
                }
            } catch (IllegalArgumentException ignored) {}
        }
        
        return players;
    }
    
    private String insertDashes(String uuid) {
        return uuid.substring(0, 8) + "-" +
               uuid.substring(8, 12) + "-" +
               uuid.substring(12, 16) + "-" +
               uuid.substring(16, 20) + "-" +
               uuid.substring(20);
    }
    
    public void clearCache() {
        cache.clear();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
}
