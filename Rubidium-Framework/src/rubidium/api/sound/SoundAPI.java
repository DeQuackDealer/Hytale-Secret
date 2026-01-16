package rubidium.api.sound;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class SoundAPI {
    
    private SoundAPI() {}
    
    public static SoundDefinition register(SoundDefinition sound) {
        return Registries.SOUNDS.register(sound.getId(), sound);
    }
    
    public static SoundDefinition.Builder create(String id) {
        return SoundDefinition.builder(id);
    }
    
    public static Optional<SoundDefinition> get(String id) {
        return Registries.SOUNDS.get(id);
    }
    
    public static Collection<SoundDefinition> all() {
        return Registries.SOUNDS.values();
    }
    
    public static SoundDefinition ambient(String id, String... files) {
        return create(id)
            .category(SoundDefinition.SoundCategory.AMBIENT)
            .sounds(files)
            .volume(0.5f)
            .build();
    }
    
    public static SoundDefinition music(String id, String file) {
        return create(id)
            .category(SoundDefinition.SoundCategory.MUSIC)
            .sound(file)
            .stream()
            .build();
    }
    
    public static SoundDefinition effect(String id, String... files) {
        return create(id)
            .category(SoundDefinition.SoundCategory.MASTER)
            .sounds(files)
            .build();
    }
    
    public static SoundDefinition voice(String id, String... files) {
        return create(id)
            .category(SoundDefinition.SoundCategory.VOICE)
            .sounds(files)
            .attenuationDistance(24)
            .build();
    }
    
    public static SoundDefinition mob(String id, String... files) {
        return create(id)
            .category(SoundDefinition.SoundCategory.HOSTILE)
            .sounds(files)
            .pitchRange(0.9f, 1.1f)
            .build();
    }
    
    public static SoundDefinition block(String id, String... files) {
        return create(id)
            .category(SoundDefinition.SoundCategory.BLOCKS)
            .sounds(files)
            .volume(1.0f)
            .build();
    }
    
    public static SoundDefinition dinoRoar(String id, String file) {
        return create(id)
            .category(SoundDefinition.SoundCategory.HOSTILE)
            .sound(file)
            .volume(2.0f)
            .attenuationDistance(48)
            .pitchRange(0.7f, 1.0f)
            .build();
    }
}
