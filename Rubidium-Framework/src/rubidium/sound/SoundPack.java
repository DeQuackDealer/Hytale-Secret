package rubidium.sound;

import java.util.*;

/**
 * Custom sound pack containing multiple sounds.
 */
public class SoundPack {
    
    private final String id;
    private final String name;
    private final Map<String, Sound> sounds;
    private final String basePath;
    
    public SoundPack(String id, String name, String basePath) {
        this.id = id;
        this.name = name;
        this.basePath = basePath;
        this.sounds = new HashMap<>();
    }
    
    public SoundPack addSound(Sound sound) {
        sounds.put(sound.getId(), sound);
        return this;
    }
    
    public Sound getSound(String soundId) {
        return sounds.get(soundId);
    }
    
    public Collection<Sound> getAllSounds() {
        return sounds.values();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getBasePath() { return basePath; }
}
