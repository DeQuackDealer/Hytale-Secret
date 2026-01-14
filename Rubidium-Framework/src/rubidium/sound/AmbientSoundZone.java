package rubidium.sound;

import rubidium.hytale.api.player.Player;

import java.util.*;

/**
 * Ambient sound zone for environmental audio.
 */
public class AmbientSoundZone {
    
    private final String id;
    private final List<AmbientSound> ambientSounds;
    private final long tickInterval;
    private final Random random;
    
    public AmbientSoundZone(String id, long tickIntervalMs) {
        this.id = id;
        this.ambientSounds = new ArrayList<>();
        this.tickInterval = tickIntervalMs;
        this.random = new Random();
    }
    
    public AmbientSoundZone addSound(String soundId, float chance, float minVolume, float maxVolume) {
        ambientSounds.add(new AmbientSound(soundId, chance, minVolume, maxVolume));
        return this;
    }
    
    public void tick(Player player) {
        for (AmbientSound ambient : ambientSounds) {
            if (random.nextFloat() < ambient.chance()) {
                float volume = ambient.minVolume() + 
                    random.nextFloat() * (ambient.maxVolume() - ambient.minVolume());
                float pitch = 0.9f + random.nextFloat() * 0.2f;
                SoundManager.getInstance().playSound(player, ambient.soundId(), volume, pitch);
            }
        }
    }
    
    public String getId() { return id; }
    public long getTickInterval() { return tickInterval; }
    
    public record AmbientSound(String soundId, float chance, float minVolume, float maxVolume) {}
}
