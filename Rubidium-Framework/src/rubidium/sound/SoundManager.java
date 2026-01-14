package rubidium.sound;

import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.world.Location;

import java.util.*;
import java.util.concurrent.*;

/**
 * 3D Positional Audio System.
 * Handles sound playback, ambient sounds, music, and custom sound packs.
 */
public class SoundManager {
    
    private static SoundManager instance;
    
    private final Map<String, SoundCategory> categories;
    private final Map<String, SoundPack> soundPacks;
    private final Map<UUID, PlayerSoundState> playerStates;
    private final Map<String, AmbientSoundZone> ambientZones;
    private final ScheduledExecutorService scheduler;
    
    private float masterVolume = 1.0f;
    private boolean soundEnabled = true;
    
    private SoundManager() {
        this.categories = new ConcurrentHashMap<>();
        this.soundPacks = new ConcurrentHashMap<>();
        this.playerStates = new ConcurrentHashMap<>();
        this.ambientZones = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Rubidium-Sound");
            t.setDaemon(true);
            return t;
        });
        
        registerDefaultCategories();
    }
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    private void registerDefaultCategories() {
        registerCategory(new SoundCategory("master", 1.0f));
        registerCategory(new SoundCategory("music", 0.7f));
        registerCategory(new SoundCategory("ambient", 0.8f));
        registerCategory(new SoundCategory("effects", 1.0f));
        registerCategory(new SoundCategory("voice", 1.0f));
        registerCategory(new SoundCategory("ui", 0.8f));
    }
    
    public void registerCategory(SoundCategory category) {
        categories.put(category.getId(), category);
    }
    
    public void registerSoundPack(SoundPack pack) {
        soundPacks.put(pack.getId(), pack);
    }
    
    public void playSound(Player player, String soundId) {
        playSound(player, soundId, 1.0f, 1.0f);
    }
    
    public void playSound(Player player, String soundId, float volume, float pitch) {
        if (!soundEnabled) return;
        
        Sound sound = resolveSound(soundId);
        if (sound == null) return;
        
        float finalVolume = calculateVolume(sound, volume);
        player.playSound(sound.getResourcePath(), finalVolume, pitch);
    }
    
    public void playSound3D(Location location, String soundId, float volume, float pitch, float maxDistance) {
        if (!soundEnabled) return;
        
        Sound sound = resolveSound(soundId);
        if (sound == null) return;
        
        for (PlayerSoundState state : playerStates.values()) {
            Player player = state.getPlayer();
            if (player == null || !player.isOnline()) continue;
            
            double distance = player.getLocation().distance(location);
            if (distance > maxDistance) continue;
            
            float distanceFactor = 1.0f - (float)(distance / maxDistance);
            float finalVolume = calculateVolume(sound, volume) * distanceFactor;
            
            if (finalVolume > 0.01f) {
                player.playSound(location, sound.getResourcePath(), finalVolume, pitch);
            }
        }
    }
    
    public void playMusic(Player player, String trackId) {
        PlayerSoundState state = getPlayerState(player);
        if (state.getCurrentMusic() != null) {
            stopMusic(player);
        }
        
        Sound track = resolveSound(trackId);
        if (track != null) {
            state.setCurrentMusic(trackId);
            float volume = calculateVolume(track, 1.0f);
            player.playSound(track.getResourcePath(), volume, 1.0f);
        }
    }
    
    public void stopMusic(Player player) {
        PlayerSoundState state = getPlayerState(player);
        state.setCurrentMusic(null);
    }
    
    public void startAmbient(Player player, String zoneId) {
        AmbientSoundZone zone = ambientZones.get(zoneId);
        if (zone == null) return;
        
        PlayerSoundState state = getPlayerState(player);
        state.setCurrentAmbient(zoneId);
    }
    
    public void stopAmbient(Player player) {
        PlayerSoundState state = getPlayerState(player);
        state.setCurrentAmbient(null);
    }
    
    public void registerAmbientZone(AmbientSoundZone zone) {
        ambientZones.put(zone.getId(), zone);
    }
    
    private Sound resolveSound(String soundId) {
        for (SoundPack pack : soundPacks.values()) {
            Sound sound = pack.getSound(soundId);
            if (sound != null) return sound;
        }
        return new Sound(soundId, soundId, "effects", 1.0f, false);
    }
    
    private float calculateVolume(Sound sound, float baseVolume) {
        SoundCategory category = categories.get(sound.getCategory());
        float categoryVolume = category != null ? category.getVolume() : 1.0f;
        return masterVolume * categoryVolume * sound.getBaseVolume() * baseVolume;
    }
    
    private PlayerSoundState getPlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUuid(), 
            k -> new PlayerSoundState(player));
    }
    
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));
    }
    
    public float getMasterVolume() {
        return masterVolume;
    }
    
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }
    
    public void setCategoryVolume(String categoryId, float volume) {
        SoundCategory category = categories.get(categoryId);
        if (category != null) {
            category.setVolume(volume);
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}
