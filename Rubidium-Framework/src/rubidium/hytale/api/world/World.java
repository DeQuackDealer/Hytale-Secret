package rubidium.hytale.api.world;

import rubidium.hytale.api.player.Player;

import java.util.Collection;

/**
 * Represents a game world.
 */
public interface World {
    
    String getName();
    
    String getId();
    
    Collection<Player> getPlayers();
    
    void spawnParticle(Location location, String particleType, int count);
    
    void playSound(Location location, String sound, float volume, float pitch);
    
    long getTime();
    
    void setTime(long time);
    
    String getWeather();
    
    void setWeather(String weather);
}
