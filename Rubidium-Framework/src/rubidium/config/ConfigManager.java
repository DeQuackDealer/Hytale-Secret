package rubidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ConfigManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path dataFolder;
    private final Map<String, Config> loadedConfigs;
    
    public ConfigManager(Path dataFolder) {
        this.dataFolder = dataFolder;
        this.loadedConfigs = new ConcurrentHashMap<>();
        
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger.severe("Failed to create data folder: " + e.getMessage());
        }
    }
    
    public Config getConfig(String name) {
        return loadedConfigs.computeIfAbsent(name, this::loadConfig);
    }
    
    public Config loadConfig(String name) {
        String fileName = name.endsWith(".yml") || name.endsWith(".json") ? name : name + ".yml";
        Path configPath = dataFolder.resolve(fileName);
        
        Config config = new Config(configPath);
        config.load();
        
        return config;
    }
    
    public void saveAll() {
        for (Config config : loadedConfigs.values()) {
            config.save();
        }
    }
    
    public void reloadAll() {
        for (Config config : loadedConfigs.values()) {
            config.reload();
        }
    }
    
    public <T> T loadAs(String name, Class<T> type) {
        Path configPath = dataFolder.resolve(name);
        
        try {
            if (name.endsWith(".json")) {
                String content = Files.readString(configPath);
                return GSON.fromJson(content, type);
            } else {
                Yaml yaml = new Yaml();
                try (Reader reader = Files.newBufferedReader(configPath)) {
                    return yaml.loadAs(reader, type);
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to load config " + name + ": " + e.getMessage());
            return null;
        }
    }
    
    public void saveAs(String name, Object object) {
        Path configPath = dataFolder.resolve(name);
        
        try {
            Files.createDirectories(configPath.getParent());
            
            if (name.endsWith(".json")) {
                String content = GSON.toJson(object);
                Files.writeString(configPath, content);
            } else {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setPrettyFlow(true);
                
                Yaml yaml = new Yaml(options);
                try (Writer writer = Files.newBufferedWriter(configPath)) {
                    yaml.dump(object, writer);
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to save config " + name + ": " + e.getMessage());
        }
    }
    
    public void copyDefaults(String resourceName) {
        Path targetPath = dataFolder.resolve(resourceName);
        
        if (Files.exists(targetPath)) {
            return;
        }
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                Files.createDirectories(targetPath.getParent());
                Files.copy(is, targetPath);
                logger.fine("Copied default config: " + resourceName);
            }
        } catch (IOException e) {
            logger.warning("Failed to copy default config " + resourceName + ": " + e.getMessage());
        }
    }
    
    public Path getDataFolder() {
        return dataFolder;
    }
    
    public static class Config {
        private static final Gson CONFIG_GSON = new GsonBuilder().setPrettyPrinting().create();
        
        private final Path path;
        private Map<String, Object> data;
        private boolean dirty = false;
        
        public Config(Path path) {
            this.path = path;
            this.data = new LinkedHashMap<>();
        }
        
        public void load() {
            if (!Files.exists(path)) {
                return;
            }
            
            try {
                if (path.toString().endsWith(".json")) {
                    String content = Files.readString(path);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> loaded = CONFIG_GSON.fromJson(content, Map.class);
                    if (loaded != null) {
                        data = new LinkedHashMap<>(loaded);
                    }
                } else {
                    Yaml yaml = new Yaml();
                    try (Reader reader = Files.newBufferedReader(path)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> loaded = yaml.load(reader);
                        if (loaded != null) {
                            data = new LinkedHashMap<>(loaded);
                        }
                    }
                }
                dirty = false;
            } catch (IOException e) {
                Logger.getLogger("Rubidium-Config").warning("Failed to load " + path + ": " + e.getMessage());
            }
        }
        
        public void save() {
            if (!dirty) return;
            
            try {
                Files.createDirectories(path.getParent());
                
                if (path.toString().endsWith(".json")) {
                    String content = CONFIG_GSON.toJson(data);
                    Files.writeString(path, content);
                } else {
                    DumperOptions options = new DumperOptions();
                    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                    options.setPrettyFlow(true);
                    options.setIndent(2);
                    
                    Yaml yaml = new Yaml(options);
                    try (Writer writer = Files.newBufferedWriter(path)) {
                        yaml.dump(data, writer);
                    }
                }
                dirty = false;
            } catch (IOException e) {
                Logger.getLogger("Rubidium-Config").warning("Failed to save " + path + ": " + e.getMessage());
            }
        }
        
        public void reload() {
            load();
        }
        
        @SuppressWarnings("unchecked")
        public <T> T get(String path) {
            return (T) getByPath(path);
        }
        
        public <T> T get(String path, T defaultValue) {
            T value = get(path);
            return value != null ? value : defaultValue;
        }
        
        public String getString(String path) {
            return get(path);
        }
        
        public String getString(String path, String defaultValue) {
            return get(path, defaultValue);
        }
        
        public int getInt(String path) {
            return getInt(path, 0);
        }
        
        public int getInt(String path, int defaultValue) {
            Number value = get(path);
            return value != null ? value.intValue() : defaultValue;
        }
        
        public long getLong(String path) {
            return getLong(path, 0L);
        }
        
        public long getLong(String path, long defaultValue) {
            Number value = get(path);
            return value != null ? value.longValue() : defaultValue;
        }
        
        public double getDouble(String path) {
            return getDouble(path, 0.0);
        }
        
        public double getDouble(String path, double defaultValue) {
            Number value = get(path);
            return value != null ? value.doubleValue() : defaultValue;
        }
        
        public boolean getBoolean(String path) {
            return getBoolean(path, false);
        }
        
        public boolean getBoolean(String path, boolean defaultValue) {
            Boolean value = get(path);
            return value != null ? value : defaultValue;
        }
        
        @SuppressWarnings("unchecked")
        public List<String> getStringList(String path) {
            List<?> list = get(path);
            if (list == null) return new ArrayList<>();
            
            List<String> result = new ArrayList<>();
            for (Object obj : list) {
                result.add(String.valueOf(obj));
            }
            return result;
        }
        
        public void set(String path, Object value) {
            setByPath(path, value);
            dirty = true;
        }
        
        public boolean contains(String path) {
            return getByPath(path) != null;
        }
        
        public Set<String> getKeys() {
            return data.keySet();
        }
        
        @SuppressWarnings("unchecked")
        private Object getByPath(String path) {
            String[] parts = path.split("\\.");
            Map<String, Object> current = data;
            
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    return null;
                }
            }
            
            return current.get(parts[parts.length - 1]);
        }
        
        @SuppressWarnings("unchecked")
        private void setByPath(String path, Object value) {
            String[] parts = path.split("\\.");
            Map<String, Object> current = data;
            
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    current.put(parts[i], newMap);
                    current = newMap;
                }
            }
            
            current.put(parts[parts.length - 1], value);
        }
    }
}
