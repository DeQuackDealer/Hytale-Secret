package rubidium.sound;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;

public class SoundManager {
    
    private final RubidiumLogger logger;
    private final Map<String, SoundDefinition> sounds;
    
    public SoundManager(RubidiumLogger logger) {
        this.logger = logger;
        this.sounds = new HashMap<>();
    }
    
    public void registerSound(String id, String soundPath) {
        registerSound(id, soundPath, 1.0f, 1.0f, SoundCategory.MASTER);
    }
    
    public void registerSound(String id, String soundPath, float defaultVolume, float defaultPitch, SoundCategory category) {
        sounds.put(id, new SoundDefinition(id, soundPath, defaultVolume, defaultPitch, category));
    }
    
    public void playSound(Player player, String soundId) {
        SoundDefinition def = sounds.get(soundId);
        if (def == null) {
            logger.warn("Unknown sound: " + soundId);
            return;
        }
        player.sendPacket(new SoundPacket(def.soundPath(), def.defaultVolume(), def.defaultPitch(), def.category()));
    }
    
    public void playSound(Player player, String soundId, float volume, float pitch) {
        SoundDefinition def = sounds.get(soundId);
        if (def == null) {
            logger.warn("Unknown sound: " + soundId);
            return;
        }
        player.sendPacket(new SoundPacket(def.soundPath(), volume, pitch, def.category()));
    }
    
    public void playSoundAt(Collection<Player> players, String soundId, double x, double y, double z) {
        playSoundAt(players, soundId, x, y, z, 1.0f, 1.0f);
    }
    
    public void playSoundAt(Collection<Player> players, String soundId, double x, double y, double z, float volume, float pitch) {
        SoundDefinition def = sounds.get(soundId);
        if (def == null) {
            logger.warn("Unknown sound: " + soundId);
            return;
        }
        
        for (Player player : players) {
            double distance = calculateDistance(player, x, y, z);
            if (distance > 64.0) continue;
            
            float adjustedVolume = calculateVolumeAtDistance(volume, distance);
            float pan = calculatePan(player, x, z);
            
            player.sendPacket(new SpatialSoundPacket(def.soundPath(), adjustedVolume, pitch, def.category(), pan));
        }
    }
    
    public void playSoundAtPlayer(Player player, String soundId, Player target) {
        playSoundAt(List.of(player), soundId, 
            target.getLocation().x(), 
            target.getLocation().y(), 
            target.getLocation().z());
    }
    
    private double calculateDistance(Player player, double x, double y, double z) {
        double dx = player.getLocation().x() - x;
        double dy = player.getLocation().y() - y;
        double dz = player.getLocation().z() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    private float calculateVolumeAtDistance(float baseVolume, double distance) {
        if (distance < 1.0) return baseVolume;
        return (float) (baseVolume / (1.0 + distance * 0.1));
    }
    
    private float calculatePan(Player listener, double soundX, double soundZ) {
        double dx = soundX - listener.getLocation().x();
        double dz = soundZ - listener.getLocation().z();
        double angle = Math.atan2(dz, dx);
        double playerYaw = Math.toRadians(listener.getLocation().yaw());
        double relativeAngle = angle - playerYaw;
        return (float) Math.sin(relativeAngle);
    }
    
    public void stopAllSounds(Player player) {
        player.sendPacket(new StopAllSoundsPacket());
    }
    
    public void stopSound(Player player, String soundId) {
        SoundDefinition def = sounds.get(soundId);
        if (def != null) {
            player.sendPacket(new StopSoundPacket(def.soundPath()));
        }
    }
    
    public enum SoundCategory {
        MASTER, MUSIC, AMBIENT, WEATHER, BLOCKS, HOSTILE, NEUTRAL, PLAYERS, VOICE
    }
    
    public record SoundDefinition(String id, String soundPath, float defaultVolume, float defaultPitch, SoundCategory category) {}
    public record SoundPacket(String soundPath, float volume, float pitch, SoundCategory category) {}
    public record SpatialSoundPacket(String soundPath, float volume, float pitch, SoundCategory category, float pan) {}
    public record StopAllSoundsPacket() {}
    public record StopSoundPacket(String soundPath) {}
}
