package rubidium.models;

import java.util.ArrayList;
import java.util.List;

public class Mesh {
    
    private final String path;
    private final ModelDefinition.MeshFormat format;
    
    private final List<Bone> bones;
    private final List<String> materialSlots;
    
    private boolean loaded;
    private int vertexCount;
    private int triangleCount;
    
    public Mesh(String path, ModelDefinition.MeshFormat format) {
        this.path = path;
        this.format = format;
        this.bones = new ArrayList<>();
        this.materialSlots = new ArrayList<>();
        this.loaded = false;
        this.vertexCount = 0;
        this.triangleCount = 0;
    }
    
    public String getPath() { return path; }
    public ModelDefinition.MeshFormat getFormat() { return format; }
    public List<Bone> getBones() { return bones; }
    public List<String> getMaterialSlots() { return materialSlots; }
    public boolean isLoaded() { return loaded; }
    public int getVertexCount() { return vertexCount; }
    public int getTriangleCount() { return triangleCount; }
    
    public void addBone(Bone bone) {
        bones.add(bone);
    }
    
    public void addMaterialSlot(String slot) {
        materialSlots.add(slot);
    }
    
    public void setLoaded(boolean loaded, int vertexCount, int triangleCount) {
        this.loaded = loaded;
        this.vertexCount = vertexCount;
        this.triangleCount = triangleCount;
    }
    
    public Bone findBone(String name) {
        return bones.stream()
            .filter(b -> b.name().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    public record Bone(
        String name,
        int index,
        int parentIndex,
        float[] position,
        float[] rotation,
        float[] scale
    ) {
        public static Bone root(String name) {
            return new Bone(name, 0, -1, 
                new float[]{0, 0, 0}, 
                new float[]{0, 0, 0, 1}, 
                new float[]{1, 1, 1});
        }
    }
}
