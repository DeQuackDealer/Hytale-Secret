package com.yellowtale.rubidium.api.config;

import java.io.*;
import java.util.*;

public class PluginConfig {
    
    private final File file;
    private Map<String, Object> data;
    
    public PluginConfig(File file) {
        this.file = file;
        this.data = new LinkedHashMap<>();
        reload();
    }
    
    public File getFile() {
        return file;
    }
    
    public void reload() {
        if (!file.exists()) {
            data = new LinkedHashMap<>();
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            data = parseYaml(reader);
        } catch (IOException e) {
            data = new LinkedHashMap<>();
        }
    }
    
    public void save() {
        try {
            file.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writeYaml(writer, data, 0);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }
    
    public void saveFromStream(InputStream stream) {
        try {
            file.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
            reload();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save default config", e);
        }
    }
    
    public void set(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        
        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<>();
                current.put(keys[i], next);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> nextMap = (Map<String, Object>) next;
            current = nextMap;
        }
        
        current.put(keys[keys.length - 1], value);
    }
    
    public Object get(String path) {
        return get(path, null);
    }
    
    public Object get(String path, Object defaultValue) {
        String[] keys = path.split("\\.");
        Object current = data;
        
        for (String key : keys) {
            if (!(current instanceof Map)) {
                return defaultValue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) current;
            current = map.get(key);
            if (current == null) {
                return defaultValue;
            }
        }
        
        return current;
    }
    
    public String getString(String path) {
        return getString(path, "");
    }
    
    public String getString(String path, String defaultValue) {
        Object value = get(path);
        return value != null ? String.valueOf(value) : defaultValue;
    }
    
    public int getInt(String path) {
        return getInt(path, 0);
    }
    
    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public long getLong(String path) {
        return getLong(path, 0L);
    }
    
    public long getLong(String path, long defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value != null ? Long.parseLong(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }
    
    public double getDouble(String path, double defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null ? Boolean.parseBoolean(String.valueOf(value)) : defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object value = get(path);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        return new ArrayList<>();
    }
    
    public boolean contains(String path) {
        return get(path) != null;
    }
    
    public Set<String> getKeys(boolean deep) {
        if (deep) {
            Set<String> keys = new LinkedHashSet<>();
            collectKeys(data, "", keys);
            return keys;
        }
        return data.keySet();
    }
    
    private void collectKeys(Map<String, Object> map, String prefix, Set<String> keys) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(key);
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                collectKeys(nested, key, keys);
            }
        }
    }
    
    private Map<String, Object> parseYaml(BufferedReader reader) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                if (!value.isEmpty()) {
                    result.put(key, parseValue(value));
                }
            }
        }
        return result;
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
    
    private void writeYaml(BufferedWriter writer, Map<String, Object> map, int indent) throws IOException {
        String prefix = "  ".repeat(indent);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                writer.write(prefix + entry.getKey() + ":\n");
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                writeYaml(writer, nested, indent + 1);
            } else {
                writer.write(prefix + entry.getKey() + ": " + formatValue(value) + "\n");
            }
        }
    }
    
    private String formatValue(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.contains(":") || str.contains("#")) {
                return "\"" + str + "\"";
            }
            return str;
        }
        return String.valueOf(value);
    }
}
