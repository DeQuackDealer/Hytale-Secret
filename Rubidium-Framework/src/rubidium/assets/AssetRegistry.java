package rubidium.assets;

import rubidium.core.logging.RubidiumLogger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AssetRegistry {
    
    private final RubidiumLogger logger;
    private final Path assetsDir;
    
    private final Map<String, AssetEntry<?>> assets;
    private final Map<AssetType, Set<String>> assetsByType;
    private final Map<String, AssetLoader<?>> loaders;
    
    private final List<Consumer<AssetEvent>> listeners;
    
    public AssetRegistry(RubidiumLogger logger, Path dataDir) {
        this.logger = logger;
        this.assetsDir = dataDir.resolve("assets");
        this.assets = new ConcurrentHashMap<>();
        this.assetsByType = new ConcurrentHashMap<>();
        this.loaders = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        
        for (AssetType type : AssetType.values()) {
            assetsByType.put(type, ConcurrentHashMap.newKeySet());
        }
    }
    
    public <T> void registerLoader(AssetType type, AssetLoader<T> loader) {
        loaders.put(type.name(), loader);
        logger.debug("Registered asset loader for type: {}", type);
    }
    
    public <T> void register(String id, AssetType type, T asset) {
        AssetEntry<T> entry = new AssetEntry<>(id, type, asset, System.currentTimeMillis());
        assets.put(id, entry);
        assetsByType.get(type).add(id);
        
        notifyListeners(new AssetEvent(id, type, AssetEvent.Type.REGISTERED));
        logger.debug("Registered asset: {} ({})", id, type);
    }
    
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String id, Class<T> type) {
        AssetEntry<?> entry = assets.get(id);
        if (entry == null) return Optional.empty();
        
        Object asset = entry.asset();
        if (type.isInstance(asset)) {
            return Optional.of((T) asset);
        }
        return Optional.empty();
    }
    
    public Optional<AssetEntry<?>> getEntry(String id) {
        return Optional.ofNullable(assets.get(id));
    }
    
    public boolean exists(String id) {
        return assets.containsKey(id);
    }
    
    public void unregister(String id) {
        AssetEntry<?> entry = assets.remove(id);
        if (entry != null) {
            assetsByType.get(entry.type()).remove(id);
            notifyListeners(new AssetEvent(id, entry.type(), AssetEvent.Type.UNREGISTERED));
            logger.debug("Unregistered asset: {}", id);
        }
    }
    
    public Set<String> getAssetsByType(AssetType type) {
        return Collections.unmodifiableSet(assetsByType.get(type));
    }
    
    public <T> T loadAsset(String id, AssetType type, Path path) {
        @SuppressWarnings("unchecked")
        AssetLoader<T> loader = (AssetLoader<T>) loaders.get(type.name());
        if (loader == null) {
            throw new IllegalStateException("No loader registered for type: " + type);
        }
        
        T asset = loader.load(path);
        register(id, type, asset);
        return asset;
    }
    
    public void reload(String id) {
        AssetEntry<?> entry = assets.get(id);
        if (entry != null) {
            notifyListeners(new AssetEvent(id, entry.type(), AssetEvent.Type.RELOADED));
            logger.info("Reloaded asset: {}", id);
        }
    }
    
    public void reloadAll(AssetType type) {
        for (String id : assetsByType.get(type)) {
            reload(id);
        }
    }
    
    public void onAssetEvent(Consumer<AssetEvent> listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners(AssetEvent event) {
        for (Consumer<AssetEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Error in asset event listener: {}", e.getMessage());
            }
        }
    }
    
    public int getAssetCount() {
        return assets.size();
    }
    
    public int getAssetCount(AssetType type) {
        return assetsByType.get(type).size();
    }
    
    public Path getAssetsDir() {
        return assetsDir;
    }
    
    public record AssetEntry<T>(String id, AssetType type, T asset, long loadedAt) {}
    
    public record AssetEvent(String assetId, AssetType type, Type eventType) {
        public enum Type { REGISTERED, UNREGISTERED, RELOADED }
    }
    
    @FunctionalInterface
    public interface AssetLoader<T> {
        T load(Path path);
    }
    
    public enum AssetType {
        MODEL,
        TEXTURE,
        ANIMATION,
        SOUND,
        CONFIG,
        SCRIPT,
        SHADER,
        FONT,
        LOCALE,
        DATA
    }
}
