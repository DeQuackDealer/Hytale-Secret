package rubidium.sound;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player sound state tracking.
 */
public class PlayerSoundState {
    
    private final Player player;
    private String currentMusic;
    private String currentAmbient;
    private final Map<String, Float> soundVolumes;
    private final Map<String, Long> soundCooldowns;
    
    public PlayerSoundState(Player player) {
        this.player = player;
        this.soundVolumes = new ConcurrentHashMap<>();
        this.soundCooldowns = new ConcurrentHashMap<>();
    }
    
    public Player getPlayer() { return player; }
    
    public String getCurrentMusic() { return currentMusic; }
    public void setCurrentMusic(String music) { this.currentMusic = music; }
    
    public String getCurrentAmbient() { return currentAmbient; }
    public void setCurrentAmbient(String ambient) { this.currentAmbient = ambient; }
    
    public float getSoundVolume(String soundId, float defaultVolume) {
        return soundVolumes.getOrDefault(soundId, defaultVolume);
    }
    
    public void setSoundVolume(String soundId, float volume) {
        soundVolumes.put(soundId, volume);
    }
    
    public boolean isOnCooldown(String soundId) {
        Long cooldownEnd = soundCooldowns.get(soundId);
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    public void setCooldown(String soundId, long durationMs) {
        soundCooldowns.put(soundId, System.currentTimeMillis() + durationMs);
    }
}
