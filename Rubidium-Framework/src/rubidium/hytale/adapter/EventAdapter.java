package rubidium.hytale.adapter;

import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.event.*;
import com.hypixel.hytale.server.core.event.events.player.*;
import com.hypixel.hytale.server.core.event.events.ecs.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Adapts Hytale server events to Rubidium's event system.
 */
public class EventAdapter {
    
    private static final Logger logger = Logger.getLogger("Rubidium-EventAdapter");
    
    private static EventAdapter instance;
    
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();
    
    private EventAdapter() {}
    
    public static EventAdapter getInstance() {
        if (instance == null) {
            instance = new EventAdapter();
        }
        return instance;
    }
    
    public <T> void registerListener(Class<T> eventType, Consumer<T> handler) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
    
    public void onPlayerJoin(AddPlayerToWorldEvent hytaleEvent) {
        Player player = ServerAdapter.getInstance().wrapPlayer(hytaleEvent.getPlayer());
        
        PlayerJoinEvent rubidiumEvent = new PlayerJoinEvent(player);
        fireEvent(PlayerJoinEvent.class, rubidiumEvent);
    }
    
    public void onPlayerQuit(DrainPlayerFromWorldEvent hytaleEvent) {
        Player player = ServerAdapter.getInstance().wrapPlayer(hytaleEvent.getPlayer());
        
        PlayerQuitEvent rubidiumEvent = new PlayerQuitEvent(player);
        fireEvent(PlayerQuitEvent.class, rubidiumEvent);
        
        ServerAdapter.getInstance().removePlayer(player.getUuid());
    }
    
    public void onBlockBreak(BreakBlockEvent hytaleEvent) {
        BlockBreakEvent rubidiumEvent = new BlockBreakEvent(
            hytaleEvent.getX(),
            hytaleEvent.getY(),
            hytaleEvent.getZ(),
            hytaleEvent.getWorld(),
            hytaleEvent.getBlockType()
        );
        
        fireEvent(BlockBreakEvent.class, rubidiumEvent);
        
        if (rubidiumEvent.isCancelled()) {
            hytaleEvent.setCancelled(true);
        }
    }
    
    public void onBlockPlace(PlaceBlockEvent hytaleEvent) {
        BlockPlaceEvent rubidiumEvent = new BlockPlaceEvent(
            hytaleEvent.getX(),
            hytaleEvent.getY(),
            hytaleEvent.getZ(),
            hytaleEvent.getWorld(),
            hytaleEvent.getBlockType()
        );
        
        fireEvent(BlockPlaceEvent.class, rubidiumEvent);
        
        if (rubidiumEvent.isCancelled()) {
            hytaleEvent.setCancelled(true);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> void fireEvent(Class<T> eventType, T event) {
        List<Consumer<?>> handlers = listeners.get(eventType);
        if (handlers != null) {
            for (Consumer<?> handler : handlers) {
                try {
                    ((Consumer<T>) handler).accept(event);
                } catch (Exception e) {
                    logger.warning("Error in event handler: " + e.getMessage());
                }
            }
        }
    }
}
