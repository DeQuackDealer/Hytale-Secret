package rubidium.models;

import java.util.ArrayList;
import java.util.List;

public class ModelDefinition {
    
    private final String id;
    private final String name;
    private final ModelType type;
    private final MeshFormat meshFormat;
    
    private final List<String> meshPaths;
    private final List<String> texturePaths;
    private final BoundingBox boundingBox;
    private final float defaultScale;
    
    private final List<String> animationIds;
    private final List<String> attachmentPoints;
    
    private ModelDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.meshFormat = builder.meshFormat;
        this.meshPaths = List.copyOf(builder.meshPaths);
        this.texturePaths = List.copyOf(builder.texturePaths);
        this.boundingBox = builder.boundingBox;
        this.defaultScale = builder.defaultScale;
        this.animationIds = List.copyOf(builder.animationIds);
        this.attachmentPoints = List.copyOf(builder.attachmentPoints);
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public ModelType getType() { return type; }
    public MeshFormat getMeshFormat() { return meshFormat; }
    public List<String> getMeshPaths() { return meshPaths; }
    public List<String> getTexturePaths() { return texturePaths; }
    public BoundingBox getBoundingBox() { return boundingBox; }
    public float getDefaultScale() { return defaultScale; }
    public List<String> getAnimationIds() { return animationIds; }
    public List<String> getAttachmentPoints() { return attachmentPoints; }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String name;
        private ModelType type = ModelType.ENTITY;
        private MeshFormat meshFormat = MeshFormat.GLTF;
        private List<String> meshPaths = new ArrayList<>();
        private List<String> texturePaths = new ArrayList<>();
        private BoundingBox boundingBox = new BoundingBox(0, 0, 0, 1, 1, 1);
        private float defaultScale = 1.0f;
        private List<String> animationIds = new ArrayList<>();
        private List<String> attachmentPoints = new ArrayList<>();
        
        private Builder(String id) {
            this.id = id;
            this.name = id;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder type(ModelType type) {
            this.type = type;
            return this;
        }
        
        public Builder meshFormat(MeshFormat format) {
            this.meshFormat = format;
            return this;
        }
        
        public Builder addMesh(String path) {
            this.meshPaths.add(path);
            return this;
        }
        
        public Builder addTexture(String path) {
            this.texturePaths.add(path);
            return this;
        }
        
        public Builder boundingBox(double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
            this.boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
            return this;
        }
        
        public Builder defaultScale(float scale) {
            this.defaultScale = scale;
            return this;
        }
        
        public Builder addAnimation(String animationId) {
            this.animationIds.add(animationId);
            return this;
        }
        
        public Builder addAttachmentPoint(String point) {
            this.attachmentPoints.add(point);
            return this;
        }
        
        public ModelDefinition build() {
            return new ModelDefinition(this);
        }
    }
    
    public enum ModelType {
        ENTITY,
        BLOCK,
        ITEM,
        PARTICLE,
        VEHICLE,
        STRUCTURE
    }
    
    public enum MeshFormat {
        GLTF,
        GLB,
        OBJ,
        FBX,
        CUSTOM
    }
}
