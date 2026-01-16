package rubidium.api.particle;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class ParticleAPI {
    
    private ParticleAPI() {}
    
    public static ParticleDefinition register(ParticleDefinition particle) {
        return Registries.PARTICLES.register(particle.getId(), particle);
    }
    
    public static ParticleDefinition.Builder create(String id) {
        return ParticleDefinition.builder(id);
    }
    
    public static Optional<ParticleDefinition> get(String id) {
        return Registries.PARTICLES.get(id);
    }
    
    public static Collection<ParticleDefinition> all() {
        return Registries.PARTICLES.values();
    }
    
    public static ParticleDefinition dust(String id, int r, int g, int b) {
        return create(id).color(r, g, b).size(1.0f).lifetime(20).build();
    }
    
    public static ParticleDefinition dust(String id, int hex) {
        return create(id).colorHex(hex).size(1.0f).lifetime(20).build();
    }
    
    public static ParticleDefinition spark(String id, int hex) {
        return create(id).colorHex(hex).size(0.5f).lifetime(10).glow().build();
    }
    
    public static ParticleDefinition smoke(String id) {
        return create(id).color(64, 64, 64).size(2.0f).lifetime(40).physics().build();
    }
    
    public static ParticleDefinition flame(String id) {
        return create(id).color(255, 128, 0).size(0.8f).lifetime(15).glow().build();
    }
    
    public static ParticleDefinition magic(String id, int hex) {
        return create(id).colorHex(hex).size(0.6f).lifetime(30).glow().build();
    }
    
    public static ParticleDefinition blood(String id) {
        return create(id).color(139, 0, 0).size(0.4f).lifetime(25).physics().build();
    }
    
    public static ParticleDefinition rain(String id) {
        return create(id).color(100, 149, 237).size(0.3f).lifetime(60).physics().build();
    }
}
