package rubidium.model;

import java.util.*;

/**
 * Group of related models for organization.
 */
public class ModelGroup {
    
    private final String id;
    private final String name;
    private final List<String> modelIds;
    private final Map<String, String> metadata;
    
    public ModelGroup(String id, String name) {
        this.id = id;
        this.name = name;
        this.modelIds = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public ModelGroup addModel(String modelId) {
        modelIds.add(modelId);
        return this;
    }
    
    public ModelGroup setMetadata(String key, String value) {
        metadata.put(key, value);
        return this;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getModelIds() { return modelIds; }
    public Map<String, String> getMetadata() { return metadata; }
}
