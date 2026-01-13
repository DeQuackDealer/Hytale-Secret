package rubidium.display;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {
    
    private final RubidiumLogger logger;
    private final Map<String, Hologram> holograms;
    
    public HologramManager(RubidiumLogger logger) {
        this.logger = logger;
        this.holograms = new ConcurrentHashMap<>();
    }
    
    public Hologram createHologram(String id, double x, double y, double z) {
        Hologram hologram = new Hologram(id, x, y, z);
        holograms.put(id, hologram);
        logger.debug("Created hologram: " + id);
        return hologram;
    }
    
    public Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(holograms.get(id));
    }
    
    public void removeHologram(String id) {
        Hologram hologram = holograms.remove(id);
        if (hologram != null) {
            logger.debug("Removed hologram: " + id);
        }
    }
    
    public Collection<Hologram> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }
    
    public void tick(Collection<Player> onlinePlayers) {
        for (Hologram hologram : holograms.values()) {
            if (!hologram.isVisible()) continue;
            
            for (Player player : onlinePlayers) {
                boolean inRange = hologram.isInRange(
                    player.getLocation().x(),
                    player.getLocation().y(),
                    player.getLocation().z()
                );
                
                boolean wasViewing = hologram.getViewers().contains(player.getUUID());
                
                if (inRange && !wasViewing) {
                    hologram.addViewer(player.getUUID());
                    sendHologramSpawn(player, hologram);
                } else if (!inRange && wasViewing) {
                    hologram.removeViewer(player.getUUID());
                    sendHologramDespawn(player, hologram);
                } else if (inRange && hologram.shouldUpdate()) {
                    sendHologramUpdate(player, hologram);
                }
            }
        }
    }
    
    private void sendHologramSpawn(Player player, Hologram hologram) {
        List<String> lines = hologram.renderFor(player);
        player.sendPacket(new HologramSpawnPacket(
            hologram.getId(),
            hologram.getX(), hologram.getY(), hologram.getZ(),
            lines
        ));
    }
    
    private void sendHologramDespawn(Player player, Hologram hologram) {
        player.sendPacket(new HologramDespawnPacket(hologram.getId()));
    }
    
    private void sendHologramUpdate(Player player, Hologram hologram) {
        List<String> lines = hologram.renderFor(player);
        player.sendPacket(new HologramUpdatePacket(hologram.getId(), lines));
    }
    
    public void onPlayerJoin(Player player) {
        for (Hologram hologram : holograms.values()) {
            if (hologram.isVisible() && hologram.isInRange(
                player.getLocation().x(),
                player.getLocation().y(),
                player.getLocation().z()
            )) {
                hologram.addViewer(player.getUUID());
                sendHologramSpawn(player, hologram);
            }
        }
    }
    
    public void onPlayerQuit(Player player) {
        for (Hologram hologram : holograms.values()) {
            hologram.removeViewer(player.getUUID());
        }
    }
    
    public record HologramSpawnPacket(String id, double x, double y, double z, List<String> lines) {}
    public record HologramDespawnPacket(String id) {}
    public record HologramUpdatePacket(String id, List<String> lines) {}
}
