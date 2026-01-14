package rubidium.model;

import java.util.*;

/**
 * Represents a .blockymodel file with elements, bones, and textures.
 */
public class BlockyModel {
    
    private final String id;
    private final int formatVersion;
    private final String texture;
    private final List<ModelElement> elements;
    private final Map<String, ModelBone> bones;
    private float[] boundingBox;
    
    public BlockyModel(String id, int formatVersion, String texture, 
                       List<ModelElement> elements, Map<String, ModelBone> bones) {
        this.id = id;
        this.formatVersion = formatVersion;
        this.texture = texture;
        this.elements = elements;
        this.bones = bones;
        calculateBoundingBox();
    }
    
    private void calculateBoundingBox() {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;
        
        for (ModelElement elem : elements) {
            float[] from = elem.getFrom();
            float[] to = elem.getTo();
            
            minX = Math.min(minX, Math.min(from[0], to[0]));
            minY = Math.min(minY, Math.min(from[1], to[1]));
            minZ = Math.min(minZ, Math.min(from[2], to[2]));
            
            maxX = Math.max(maxX, Math.max(from[0], to[0]));
            maxY = Math.max(maxY, Math.max(from[1], to[1]));
            maxZ = Math.max(maxZ, Math.max(from[2], to[2]));
        }
        
        this.boundingBox = new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }
    
    public String getId() { return id; }
    public int getFormatVersion() { return formatVersion; }
    public String getTexture() { return texture; }
    public List<ModelElement> getElements() { return elements; }
    public Map<String, ModelBone> getBones() { return bones; }
    public float[] getBoundingBox() { return boundingBox; }
    
    public Optional<ModelBone> getBone(String name) {
        return Optional.ofNullable(bones.get(name));
    }
    
    public List<ModelElement> getElementsByParent(String parent) {
        return elements.stream()
            .filter(e -> Objects.equals(e.getParent(), parent))
            .toList();
    }
    
    public float getWidth() { return boundingBox[3] - boundingBox[0]; }
    public float getHeight() { return boundingBox[4] - boundingBox[1]; }
    public float getDepth() { return boundingBox[5] - boundingBox[2]; }
}
