package rubidium.api.registry;

import java.util.Objects;

public record ResourceId(String namespace, String path) {
    
    public static final String DEFAULT_NAMESPACE = "rubidium";
    
    public ResourceId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty()) throw new IllegalArgumentException("namespace cannot be empty");
        if (path.isEmpty()) throw new IllegalArgumentException("path cannot be empty");
    }
    
    public static ResourceId of(String namespace, String path) {
        return new ResourceId(namespace, path);
    }
    
    public static ResourceId of(String path) {
        return new ResourceId(DEFAULT_NAMESPACE, path);
    }
    
    public static ResourceId parse(String id) {
        int colonIndex = id.indexOf(':');
        if (colonIndex == -1) {
            return of(id);
        }
        return of(id.substring(0, colonIndex), id.substring(colonIndex + 1));
    }
    
    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
