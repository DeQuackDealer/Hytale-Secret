package rubidium.api.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigAPI {
    
    private static final Map<String, Config> configs = new ConcurrentHashMap<>();
    private static Path configRoot = Paths.get("config");
    
    private ConfigAPI() {}
    
    public static void setConfigRoot(Path root) {
        configRoot = root;
    }
    
    public static Path getConfigRoot() {
        return configRoot;
    }
    
    public static Config load(String name) throws IOException {
        Path path = configRoot.resolve(name + ".yml");
        Config config = new Config(name, path);
        config.load();
        configs.put(name, config);
        return config;
    }
    
    public static Config loadOrCreate(String name, Map<String, Object> defaults) throws IOException {
        Path path = configRoot.resolve(name + ".yml");
        Config config = new Config(name, path);
        
        if (Files.exists(path)) {
            config.load();
        } else {
            config.setDefaults(defaults);
            config.save();
        }
        
        configs.put(name, config);
        return config;
    }
    
    public static Optional<Config> get(String name) {
        return Optional.ofNullable(configs.get(name));
    }
    
    public static void saveAll() throws IOException {
        for (Config config : configs.values()) {
            config.save();
        }
    }
    
    public static void reloadAll() throws IOException {
        for (Config config : configs.values()) {
            config.load();
        }
    }
    
    public static class Config {
        private final String name;
        private final Path path;
        private final Map<String, Object> data = new LinkedHashMap<>();
        private final Map<String, Object> defaults = new HashMap<>();
        
        public Config(String name, Path path) {
            this.name = name;
            this.path = path;
        }
        
        public String getName() { return name; }
        public Path getPath() { return path; }
        
        public void load() throws IOException {
            if (!Files.exists(path)) return;
            
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                String currentSection = null;
                Map<String, Object> currentMap = data;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    if (line.endsWith(":") && !line.contains(": ")) {
                        currentSection = line.substring(0, line.length() - 1);
                        Map<String, Object> section = new LinkedHashMap<>();
                        data.put(currentSection, section);
                        currentMap = section;
                    } else if (line.contains(": ")) {
                        String[] parts = line.split(": ", 2);
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        currentMap.put(key, parseValue(value));
                    }
                }
            }
        }
        
        public void save() throws IOException {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writeMap(writer, data, 0);
            }
        }
        
        private void writeMap(BufferedWriter writer, Map<String, Object> map, int indent) throws IOException {
            String prefix = "  ".repeat(indent);
            for (var entry : map.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    writer.write(prefix + entry.getKey() + ":\n");
                    writeMap(writer, (Map<String, Object>) entry.getValue(), indent + 1);
                } else {
                    writer.write(prefix + entry.getKey() + ": " + formatValue(entry.getValue()) + "\n");
                }
            }
        }
        
        private Object parseValue(String value) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1);
            }
            if (value.equalsIgnoreCase("true")) return true;
            if (value.equalsIgnoreCase("false")) return false;
            try {
                if (value.contains(".")) return Double.parseDouble(value);
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        
        private String formatValue(Object value) {
            if (value instanceof String) return "\"" + value + "\"";
            return String.valueOf(value);
        }
        
        public void setDefaults(Map<String, Object> defaults) {
            this.defaults.putAll(defaults);
            for (var entry : defaults.entrySet()) {
                if (!data.containsKey(entry.getKey())) {
                    data.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        public void set(String key, Object value) {
            String[] parts = key.split("\\.");
            Map<String, Object> current = data;
            
            for (int i = 0; i < parts.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new LinkedHashMap<>());
            }
            
            current.put(parts[parts.length - 1], value);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            String[] parts = key.split("\\.");
            Object current = data;
            
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else {
                    return null;
                }
            }
            
            return (T) current;
        }
        
        public <T> T get(String key, T defaultValue) {
            T value = get(key);
            return value != null ? value : defaultValue;
        }
        
        public String getString(String key) { return get(key); }
        public String getString(String key, String def) { return get(key, def); }
        
        public int getInt(String key) { return get(key, 0L).intValue(); }
        public int getInt(String key, int def) { 
            Number n = get(key);
            return n != null ? n.intValue() : def;
        }
        
        public long getLong(String key) { return get(key, 0L); }
        public long getLong(String key, long def) { return get(key, def); }
        
        public double getDouble(String key) { return get(key, 0.0); }
        public double getDouble(String key, double def) { return get(key, def); }
        
        public boolean getBoolean(String key) { return get(key, false); }
        public boolean getBoolean(String key, boolean def) { return get(key, def); }
        
        public List<String> getStringList(String key) {
            Object value = get(key);
            if (value instanceof List) return (List<String>) value;
            return new ArrayList<>();
        }
        
        @SuppressWarnings("unchecked")
        public Map<String, Object> getSection(String key) {
            Object value = get(key);
            if (value instanceof Map) return (Map<String, Object>) value;
            return new HashMap<>();
        }
        
        public boolean contains(String key) {
            return get(key) != null;
        }
        
        public Set<String> getKeys() {
            return data.keySet();
        }
        
        public Map<String, Object> getAll() {
            return Collections.unmodifiableMap(data);
        }
    }
    
    public static class ConfigBuilder {
        private final Map<String, Object> data = new LinkedHashMap<>();
        
        public ConfigBuilder set(String key, Object value) {
            data.put(key, value);
            return this;
        }
        
        public ConfigBuilder section(String name, Map<String, Object> section) {
            data.put(name, section);
            return this;
        }
        
        public Map<String, Object> build() {
            return new LinkedHashMap<>(data);
        }
    }
    
    public static ConfigBuilder builder() {
        return new ConfigBuilder();
    }
}
