package rubidium.integration.modtale;

import rubidium.core.logging.RubidiumLogger;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Modtale API client for searching and downloading plugins.
 * 
 * <p><b>IMPORTANT:</b> This implementation includes a basic JSON parser for zero-dependency operation.
 * For production use, you MUST provide a proper JSON parser implementation (Jackson, Gson, etc.)
 * via the constructor or {@link #setJsonParser(JsonParser)} method.</p>
 * 
 * <h3>Example with Gson:</h3>
 * <pre>{@code
 * ModtaleClient client = new ModtaleClient(logger, apiKey);
 * client.setJsonParser(new ModtaleClient.JsonParser() {
 *     private final Gson gson = new Gson();
 *     
 *     @Override
 *     public SearchResult parseSearchResult(String json) {
 *         return gson.fromJson(json, SearchResult.class);
 *     }
 *     
 *     @Override
 *     public ProjectDetails parseProjectDetails(String json) {
 *         return gson.fromJson(json, ProjectDetails.class);
 *     }
 *     
 *     @Override
 *     public List<String> parseStringArray(String json) {
 *         return gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
 *     }
 * });
 * }</pre>
 */
public class ModtaleClient {
    
    /**
     * Interface for JSON parsing. Implement this to use Jackson, Gson, or other JSON libraries.
     * The built-in parser is suitable for basic testing but not production use.
     */
    public interface JsonParser {
        SearchResult parseSearchResult(String json);
        ProjectDetails parseProjectDetails(String json);
        List<String> parseStringArray(String json);
    }
    
    private static final String API_BASE = "https://api.modtale.net";
    private static final String API_VERSION = "v1";
    
    private final HttpClient httpClient;
    private final RubidiumLogger logger;
    private final String apiKey;
    private JsonParser jsonParser;
    
    public ModtaleClient(RubidiumLogger logger, String apiKey) {
        this.logger = logger;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.jsonParser = null;
    }
    
    public ModtaleClient(RubidiumLogger logger, String apiKey, JsonParser jsonParser) {
        this.logger = logger;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.jsonParser = jsonParser;
    }
    
    /**
     * Set a custom JSON parser for production use.
     * @param parser Implementation using Jackson, Gson, or similar library
     */
    public void setJsonParser(JsonParser parser) {
        this.jsonParser = parser;
        logger.info("Custom JSON parser configured for Modtale client");
    }
    
    /**
     * Check if a custom JSON parser is configured.
     * @return true if using custom parser, false if using basic built-in parser
     */
    public boolean hasCustomParser() {
        return jsonParser != null;
    }
    
    private SearchResult delegateParseSearchResult(String json) {
        if (jsonParser != null) {
            return jsonParser.parseSearchResult(json);
        }
        logger.warn("Using basic JSON parser - configure a proper JSON parser for production use");
        return parseSearchResultBasic(json);
    }
    
    private ProjectDetails delegateParseProjectDetails(String json) {
        if (jsonParser != null) {
            return jsonParser.parseProjectDetails(json);
        }
        logger.warn("Using basic JSON parser - configure a proper JSON parser for production use");
        return parseProjectDetailsBasic(json);
    }
    
    private List<String> delegateParseStringArray(String json) {
        if (jsonParser != null) {
            return jsonParser.parseStringArray(json);
        }
        return parseStringArray(json);
    }
    
    public CompletableFuture<SearchResult> searchPlugins(SearchQuery query) {
        StringBuilder url = new StringBuilder(API_BASE + "/api/" + API_VERSION + "/projects?");
        url.append("classification=PLUGIN");
        url.append("&page=").append(query.page());
        url.append("&size=").append(query.size());
        url.append("&sort=").append(query.sort().getValue());
        
        if (query.search() != null && !query.search().isBlank()) {
            url.append("&search=").append(encodeUrl(query.search()));
        }
        if (query.tags() != null && !query.tags().isEmpty()) {
            url.append("&tags=").append(String.join(",", query.tags()));
        }
        if (query.gameVersion() != null) {
            url.append("&gameVersion=").append(encodeUrl(query.gameVersion()));
        }
        if (query.author() != null) {
            url.append("&author=").append(encodeUrl(query.author()));
        }
        
        return get(url.toString())
            .thenApply(this::delegateParseSearchResult);
    }
    
    public CompletableFuture<ProjectDetails> getProject(String projectId) {
        String url = API_BASE + "/api/" + API_VERSION + "/projects/" + projectId;
        return get(url)
            .thenApply(this::delegateParseProjectDetails);
    }
    
    public CompletableFuture<List<String>> getTags() {
        String url = API_BASE + "/api/" + API_VERSION + "/tags";
        return get(url)
            .thenApply(this::delegateParseStringArray);
    }
    
    public CompletableFuture<List<String>> getGameVersions() {
        String url = API_BASE + "/api/" + API_VERSION + "/meta/game-versions";
        return get(url)
            .thenApply(this::delegateParseStringArray);
    }
    
    public CompletableFuture<Path> downloadPlugin(String projectId, String versionId, Path targetDir) {
        String url = API_BASE + "/api/" + API_VERSION + "/projects/" + projectId + "/versions/" + versionId + "/download";
        
        return getProject(projectId).thenCompose(project -> {
            String fileName = sanitizeFileName(project.title()) + "-" + versionId + ".jar";
            Path targetPath = targetDir.resolve(fileName);
            
            return downloadFile(url, targetPath)
                .thenApply(success -> {
                    if (success) {
                        logger.info("Downloaded plugin {} v{} to {}", project.title(), versionId, targetPath);
                        return targetPath;
                    }
                    throw new RuntimeException("Failed to download plugin");
                });
        });
    }
    
    public CompletableFuture<Path> downloadLatestVersion(String projectId, Path targetDir) {
        return getProject(projectId).thenCompose(project -> {
            if (project.versions().isEmpty()) {
                throw new RuntimeException("No versions available for plugin: " + project.title());
            }
            
            ProjectVersion latestVersion = project.versions().get(0);
            String fileName = sanitizeFileName(project.title()) + "-" + latestVersion.versionNumber() + ".jar";
            Path targetPath = targetDir.resolve(fileName);
            
            String url = API_BASE + "/api/" + API_VERSION + "/projects/" + projectId + "/versions/" + latestVersion.id() + "/download";
            
            return downloadFile(url, targetPath)
                .thenApply(success -> {
                    if (success) {
                        logger.info("Downloaded plugin {} v{}", project.title(), latestVersion.versionNumber());
                        return targetPath;
                    }
                    throw new RuntimeException("Failed to download plugin");
                });
        });
    }
    
    private CompletableFuture<String> get(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(30));
        
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("X-MODTALE-KEY", apiKey);
        }
        
        HttpRequest request = builder.build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() >= 400) {
                    logger.error("Modtale API error {}: {}", response.statusCode(), response.body());
                    throw new RuntimeException("API error: " + response.statusCode());
                }
                return response.body();
            });
    }
    
    private CompletableFuture<Boolean> downloadFile(String url, Path targetPath) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofMinutes(5));
        
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("X-MODTALE-KEY", apiKey);
        }
        
        HttpRequest request = builder.build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApply(response -> {
                if (response.statusCode() >= 400) {
                    logger.error("Download failed with status {}", response.statusCode());
                    return false;
                }
                
                try {
                    Files.createDirectories(targetPath.getParent());
                    try (InputStream is = response.body();
                         OutputStream os = Files.newOutputStream(targetPath)) {
                        is.transferTo(os);
                    }
                    return true;
                } catch (IOException e) {
                    logger.error("Failed to save downloaded file", e);
                    return false;
                }
            });
    }
    
    private SearchResult parseSearchResultBasic(String json) {
        List<ProjectSummary> projects = new ArrayList<>();
        int totalPages = 0;
        int totalElements = 0;
        
        if (json == null || json.isBlank()) {
            logger.warn("Empty response from Modtale API");
            return new SearchResult(projects, totalPages, totalElements);
        }
        
        try {
            totalPages = extractJsonInt(json, "totalPages");
            totalElements = extractJsonInt(json, "totalElements");
            
            String contentArray = extractJsonArray(json, "content");
            if (contentArray != null && !contentArray.isBlank()) {
                projects = parseJsonObjectArray(contentArray, this::parseProjectSummary);
            }
            
            logger.debug("Parsed {} projects from search result", projects.size());
        } catch (Exception e) {
            logger.error("Failed to parse search result: {}", e.getMessage());
        }
        
        return new SearchResult(projects, totalPages, totalElements);
    }
    
    private <T> List<T> parseJsonObjectArray(String arrayJson, Function<String, T> parser) {
        List<T> results = new ArrayList<>();
        if (arrayJson == null || arrayJson.isBlank()) return results;
        
        int braceDepth = 0;
        int objStart = -1;
        boolean inString = false;
        char prevChar = 0;
        
        for (int i = 0; i < arrayJson.length(); i++) {
            char c = arrayJson.charAt(i);
            
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '{') {
                    if (braceDepth == 0) objStart = i;
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                    if (braceDepth == 0 && objStart != -1) {
                        String objStr = arrayJson.substring(objStart, i + 1);
                        try {
                            T item = parser.apply(objStr);
                            if (item != null) {
                                results.add(item);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to parse JSON object at position {}", objStart);
                        }
                        objStart = -1;
                    }
                }
            }
            prevChar = c;
        }
        
        return results;
    }
    
    private ProjectSummary parseProjectSummary(String json) {
        String id = extractJsonString(json, "id");
        String title = extractJsonString(json, "title");
        
        if (id == null || title == null) {
            return null;
        }
        
        return new ProjectSummary(
            id,
            title,
            extractJsonString(json, "author"),
            extractJsonString(json, "description"),
            extractJsonString(json, "imageUrl"),
            extractJsonInt(json, "downloads"),
            extractJsonDouble(json, "rating"),
            extractJsonString(json, "updatedAt"),
            parseStringArrayFromContent(extractJsonArray(json, "tags"))
        );
    }
    
    private ProjectDetails parseProjectDetailsBasic(String json) {
        if (json == null || json.isBlank()) {
            throw new RuntimeException("Empty project details response");
        }
        
        String id = extractJsonString(json, "id");
        String title = extractJsonString(json, "title");
        
        if (id == null) {
            throw new RuntimeException("Invalid project details: missing id");
        }
        
        List<ProjectVersion> versions = new ArrayList<>();
        String versionsArray = extractJsonArray(json, "versions");
        if (versionsArray != null && !versionsArray.isBlank()) {
            versions = parseJsonObjectArray(versionsArray, this::parseVersion);
        }
        
        return new ProjectDetails(
            id,
            title != null ? title : "Unknown",
            extractJsonString(json, "description"),
            extractJsonString(json, "about"),
            extractJsonString(json, "author"),
            extractJsonString(json, "status"),
            extractJsonString(json, "license"),
            extractJsonString(json, "repositoryUrl"),
            versions,
            parseStringArrayFromContent(extractJsonArray(json, "galleryImages"))
        );
    }
    
    private ProjectVersion parseVersion(String json) {
        String id = extractJsonString(json, "id");
        String versionNumber = extractJsonString(json, "versionNumber");
        
        if (id == null) {
            return null;
        }
        
        return new ProjectVersion(
            id,
            versionNumber != null ? versionNumber : "unknown",
            extractJsonString(json, "fileUrl"),
            extractJsonInt(json, "downloadCount")
        );
    }
    
    private List<String> parseStringArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start == -1 || end == -1 || end <= start) return result;
        
        return parseStringArrayFromContent(json.substring(start + 1, end));
    }
    
    private List<String> parseStringArrayFromContent(String content) {
        List<String> result = new ArrayList<>();
        if (content == null || content.isBlank()) return result;
        
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        char prevChar = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && prevChar != '\\') {
                if (inString) {
                    String value = current.toString();
                    if (!value.isEmpty()) {
                        result.add(value);
                    }
                    current = new StringBuilder();
                }
                inString = !inString;
            } else if (inString) {
                if (c == '\\' && prevChar == '\\') {
                    current.append('\\');
                    c = 0;
                } else if (prevChar != '\\') {
                    current.append(c);
                } else {
                    switch (c) {
                        case 'n' -> current.append('\n');
                        case 'r' -> current.append('\r');
                        case 't' -> current.append('\t');
                        default -> current.append(c);
                    }
                }
            }
            prevChar = c;
        }
        
        return result;
    }
    
    private String extractJsonString(String json, String key) {
        if (json == null || key == null) return null;
        
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return null;
        
        int colonIdx = json.indexOf(':', keyIdx + pattern.length());
        if (colonIdx == -1) return null;
        
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        if (start >= json.length()) return null;
        
        if (json.charAt(start) == 'n' && json.substring(start).startsWith("null")) {
            return null;
        }
        
        if (json.charAt(start) != '"') return null;
        
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        
        return null;
    }
    
    private int extractJsonInt(String json, String key) {
        if (json == null || key == null) return 0;
        
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return 0;
        
        int colonIdx = json.indexOf(':', keyIdx + pattern.length());
        if (colonIdx == -1) return 0;
        
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        StringBuilder sb = new StringBuilder();
        while (start < json.length()) {
            char c = json.charAt(start);
            if (Character.isDigit(c) || c == '-') {
                sb.append(c);
                start++;
            } else {
                break;
            }
        }
        
        if (sb.isEmpty()) return 0;
        
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private double extractJsonDouble(String json, String key) {
        if (json == null || key == null) return 0.0;
        
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return 0.0;
        
        int colonIdx = json.indexOf(':', keyIdx + pattern.length());
        if (colonIdx == -1) return 0.0;
        
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        StringBuilder sb = new StringBuilder();
        while (start < json.length()) {
            char c = json.charAt(start);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == 'e' || c == 'E' || c == '+') {
                sb.append(c);
                start++;
            } else {
                break;
            }
        }
        
        if (sb.isEmpty()) return 0.0;
        
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private String extractJsonArray(String json, String key) {
        if (json == null || key == null) return null;
        
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return null;
        
        int colonIdx = json.indexOf(':', keyIdx + pattern.length());
        if (colonIdx == -1) return null;
        
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        if (start >= json.length() || json.charAt(start) != '[') return null;
        
        int bracketDepth = 1;
        int end = start + 1;
        boolean inString = false;
        char prevChar = 0;
        
        while (end < json.length() && bracketDepth > 0) {
            char c = json.charAt(end);
            
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '[') bracketDepth++;
                else if (c == ']') bracketDepth--;
            }
            
            prevChar = c;
            end++;
        }
        
        if (bracketDepth != 0) return null;
        
        return json.substring(start + 1, end - 1);
    }
    
    private String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
    
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    public record SearchQuery(
        String search,
        int page,
        int size,
        SortOrder sort,
        List<String> tags,
        String gameVersion,
        String author
    ) {
        public static SearchQuery defaults() {
            return new SearchQuery(null, 0, 10, SortOrder.DOWNLOADS, null, null, null);
        }
        
        public SearchQuery withSearch(String search) {
            return new SearchQuery(search, page, size, sort, tags, gameVersion, author);
        }
        
        public SearchQuery withPage(int page) {
            return new SearchQuery(search, page, size, sort, tags, gameVersion, author);
        }
        
        public SearchQuery withSize(int size) {
            return new SearchQuery(search, page, size, sort, tags, gameVersion, author);
        }
        
        public SearchQuery withSort(SortOrder sort) {
            return new SearchQuery(search, page, size, sort, tags, gameVersion, author);
        }
    }
    
    public enum SortOrder {
        RELEVANCE("relevance"),
        DOWNLOADS("downloads"),
        UPDATED("updated"),
        NEWEST("newest"),
        RATING("rating"),
        FAVORITES("favorites");
        
        private final String value;
        
        SortOrder(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public record SearchResult(
        List<ProjectSummary> projects,
        int totalPages,
        int totalElements
    ) {}
    
    public record ProjectSummary(
        String id,
        String title,
        String author,
        String description,
        String imageUrl,
        int downloads,
        double rating,
        String updatedAt,
        List<String> tags
    ) {}
    
    public record ProjectDetails(
        String id,
        String title,
        String description,
        String about,
        String author,
        String status,
        String license,
        String repositoryUrl,
        List<ProjectVersion> versions,
        List<String> galleryImages
    ) {}
    
    public record ProjectVersion(
        String id,
        String versionNumber,
        String fileUrl,
        int downloadCount
    ) {}
}
