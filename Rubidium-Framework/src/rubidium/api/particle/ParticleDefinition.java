package rubidium.api.particle;

import rubidium.api.registry.ResourceId;

public class ParticleDefinition {
    
    private final ResourceId id;
    private final String displayName;
    private final int red, green, blue;
    private final float size;
    private final int lifetime;
    private final boolean physics;
    private final boolean glow;
    private final String texture;
    
    private ParticleDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.red = builder.red;
        this.green = builder.green;
        this.blue = builder.blue;
        this.size = builder.size;
        this.lifetime = builder.lifetime;
        this.physics = builder.physics;
        this.glow = builder.glow;
        this.texture = builder.texture;
    }
    
    public ResourceId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }
    public float getSize() { return size; }
    public int getLifetime() { return lifetime; }
    public boolean hasPhysics() { return physics; }
    public boolean isGlowing() { return glow; }
    public String getTexture() { return texture; }
    
    public static Builder builder(String id) {
        return new Builder(ResourceId.of(id));
    }
    
    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final ResourceId id;
        private String displayName;
        private int red = 255, green = 255, blue = 255;
        private float size = 1.0f;
        private int lifetime = 20;
        private boolean physics = false;
        private boolean glow = false;
        private String texture;
        
        private Builder(ResourceId id) {
            this.id = id;
            this.displayName = id.path();
        }
        
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder color(int r, int g, int b) { this.red = r; this.green = g; this.blue = b; return this; }
        public Builder colorHex(int hex) { 
            this.red = (hex >> 16) & 0xFF; 
            this.green = (hex >> 8) & 0xFF; 
            this.blue = hex & 0xFF; 
            return this; 
        }
        public Builder size(float size) { this.size = size; return this; }
        public Builder lifetime(int ticks) { this.lifetime = ticks; return this; }
        public Builder physics() { this.physics = true; return this; }
        public Builder glow() { this.glow = true; return this; }
        public Builder texture(String texture) { this.texture = texture; return this; }
        
        public ParticleDefinition build() {
            return new ParticleDefinition(this);
        }
    }
}
