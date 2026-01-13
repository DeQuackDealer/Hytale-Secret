package rubidium.models;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Model {
    
    private final String id;
    private final ModelDefinition definition;
    private final List<Mesh> meshes;
    private final List<String> textures;
    private final Map<String, Object> metadata;
    
    private float scale;
    private float[] rotation;
    private float[] offset;
    
    public Model(String id, ModelDefinition definition, List<Mesh> meshes, 
                 List<String> textures, Map<String, Object> metadata) {
        this.id = id;
        this.definition = definition;
        this.meshes = meshes;
        this.textures = textures;
        this.metadata = metadata;
        this.scale = definition.getDefaultScale();
        this.rotation = new float[]{0, 0, 0};
        this.offset = new float[]{0, 0, 0};
    }
    
    public String getId() { return id; }
    public ModelDefinition getDefinition() { return definition; }
    public List<Mesh> getMeshes() { return Collections.unmodifiableList(meshes); }
    public List<String> getTextures() { return Collections.unmodifiableList(textures); }
    public Map<String, Object> getMetadata() { return metadata; }
    
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }
    
    public float[] getRotation() { return rotation.clone(); }
    public void setRotation(float pitch, float yaw, float roll) {
        this.rotation = new float[]{pitch, yaw, roll};
    }
    
    public float[] getOffset() { return offset.clone(); }
    public void setOffset(float x, float y, float z) {
        this.offset = new float[]{x, y, z};
    }
    
    public BoundingBox getBoundingBox() {
        return definition.getBoundingBox().scale(scale);
    }
    
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
}
