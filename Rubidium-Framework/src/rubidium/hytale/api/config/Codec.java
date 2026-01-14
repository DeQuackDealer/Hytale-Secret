package rubidium.hytale.api.config;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;

/**
 * Generic codec for configuration serialization.
 */
public interface Codec<T> {
    
    T decode(JsonElement element);
    
    JsonElement encode(T value);
    
    default T load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        String content = Files.readString(file);
        JsonElement element = JsonParser.parseString(content);
        return decode(element);
    }
    
    default void save(Path file, T value) throws IOException {
        Files.createDirectories(file.getParent());
        JsonElement element = encode(value);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(file, gson.toJson(element));
    }
    
    static <T> Codec<T> json(Class<T> type) {
        Gson gson = new Gson();
        return new Codec<>() {
            @Override
            public T decode(JsonElement element) {
                return gson.fromJson(element, type);
            }
            
            @Override
            public JsonElement encode(T value) {
                return gson.toJsonTree(value);
            }
        };
    }
}
