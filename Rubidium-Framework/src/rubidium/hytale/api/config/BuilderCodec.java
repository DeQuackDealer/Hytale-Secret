package rubidium.hytale.api.config;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;

/**
 * Builder-based codec for configuration serialization.
 */
public class BuilderCodec<T> {
    
    private final Class<T> type;
    private final Gson gson;
    
    private BuilderCodec(Class<T> type) {
        this.type = type;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }
    
    public static <T> BuilderCodec<T> of(Class<T> type) {
        return new BuilderCodec<>(type);
    }
    
    public T load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        String content = Files.readString(file);
        return gson.fromJson(content, type);
    }
    
    public T loadOrDefault(Path file, T defaultValue) throws IOException {
        T loaded = load(file);
        if (loaded == null) {
            save(file, defaultValue);
            return defaultValue;
        }
        return loaded;
    }
    
    public void save(Path file, T value) throws IOException {
        Files.createDirectories(file.getParent());
        String json = gson.toJson(value);
        Files.writeString(file, json);
    }
    
    public String toJson(T value) {
        return gson.toJson(value);
    }
    
    public T fromJson(String json) {
        return gson.fromJson(json, type);
    }
    
    public T fromReader(Reader reader) {
        return gson.fromJson(reader, type);
    }
}
