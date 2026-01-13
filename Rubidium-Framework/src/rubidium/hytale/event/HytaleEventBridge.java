package rubidium.hytale.event;

import rubidium.api.player.Player;
import rubidium.hytale.adapter.HytaleAdapter;
import rubidium.hytale.entity.HytaleEntityWrapper;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class HytaleEventBridge {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytaleEvents");
    
    private final HytaleAdapter adapter;
    private final Map<Class<?>, List<Consumer<Object>>> rubidiumListeners;
    private final List<Object> registeredHytaleListeners;
    
    private static final Map<String, String> EVENT_MAPPINGS = Map.ofEntries(
        Map.entry("PlayerJoinEvent", "com.hypixel.hytale.event.player.PlayerJoinEvent"),
        Map.entry("PlayerQuitEvent", "com.hypixel.hytale.event.player.PlayerQuitEvent"),
        Map.entry("PlayerChatEvent", "com.hypixel.hytale.event.player.PlayerChatEvent"),
        Map.entry("PlayerMoveEvent", "com.hypixel.hytale.event.player.PlayerMoveEvent"),
        Map.entry("PlayerDeathEvent", "com.hypixel.hytale.event.player.PlayerDeathEvent"),
        Map.entry("PlayerInteractEvent", "com.hypixel.hytale.event.player.PlayerInteractEvent"),
        Map.entry("PlayerCommandEvent", "com.hypixel.hytale.event.player.PlayerCommandEvent"),
        Map.entry("EntitySpawnEvent", "com.hypixel.hytale.event.entity.EntitySpawnEvent"),
        Map.entry("EntityDeathEvent", "com.hypixel.hytale.event.entity.EntityDeathEvent"),
        Map.entry("EntityDamageEvent", "com.hypixel.hytale.event.entity.EntityDamageEvent"),
        Map.entry("BlockBreakEvent", "com.hypixel.hytale.event.block.BlockBreakEvent"),
        Map.entry("BlockPlaceEvent", "com.hypixel.hytale.event.block.BlockPlaceEvent"),
        Map.entry("WorldLoadEvent", "com.hypixel.hytale.event.world.WorldLoadEvent"),
        Map.entry("WorldUnloadEvent", "com.hypixel.hytale.event.world.WorldUnloadEvent")
    );
    
    public HytaleEventBridge(HytaleAdapter adapter) {
        this.adapter = adapter;
        this.rubidiumListeners = new ConcurrentHashMap<>();
        this.registeredHytaleListeners = new ArrayList<>();
    }
    
    public void registerHytaleEventListeners(Object hytaleServer) {
        try {
            Object eventManager = extractEventManager(hytaleServer);
            if (eventManager == null) {
                logger.warning("Could not find Hytale event manager");
                return;
            }
            
            for (Map.Entry<String, String> mapping : EVENT_MAPPINGS.entrySet()) {
                try {
                    Class<?> eventClass = Class.forName(mapping.getValue());
                    registerEventListener(eventManager, eventClass, mapping.getKey());
                } catch (ClassNotFoundException e) {
                    logger.fine("Event class not found: " + mapping.getValue());
                }
            }
            
            logger.info("Registered Hytale event listeners");
            
        } catch (Exception e) {
            logger.warning("Failed to register Hytale event listeners: " + e.getMessage());
        }
    }
    
    private Object extractEventManager(Object server) {
        String[] methodNames = {"getEventManager", "getEventBus", "getEvents", "events"};
        
        for (String name : methodNames) {
            try {
                Method m = server.getClass().getMethod(name);
                return m.invoke(server);
            } catch (Exception ignored) {}
        }
        
        String[] fieldNames = {"eventManager", "eventBus", "events"};
        for (String name : fieldNames) {
            try {
                var field = server.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(server);
            } catch (Exception ignored) {}
        }
        
        return null;
    }
    
    private void registerEventListener(Object eventManager, Class<?> eventClass, String rubidiumEventName) {
        try {
            Method registerMethod = findRegisterMethod(eventManager);
            if (registerMethod == null) {
                logger.warning("Could not find register method on event manager");
                return;
            }
            
            Consumer<Object> bridgeConsumer = new EventBridgeConsumer(rubidiumEventName);
            
            Class<?>[] paramTypes = registerMethod.getParameterTypes();
            
            if (paramTypes.length == 2 && paramTypes[0] == Class.class) {
                registerMethod.invoke(eventManager, eventClass, bridgeConsumer);
            } else if (paramTypes.length == 1) {
                registerMethod.invoke(eventManager, bridgeConsumer);
            } else {
                Object listenerWrapper = createListenerWrapper(eventManager, eventClass, rubidiumEventName);
                if (listenerWrapper != null) {
                    registerMethod.invoke(eventManager, listenerWrapper);
                }
            }
            
            registeredHytaleListeners.add(bridgeConsumer);
            logger.fine("Registered bridge for " + rubidiumEventName);
            
        } catch (Exception e) {
            logger.warning("Failed to register listener for " + rubidiumEventName + ": " + e.getMessage());
        }
    }
    
    private Object createListenerWrapper(Object eventManager, Class<?> eventClass, String rubidiumEventName) {
        try {
            Class<?> listenerInterface = findListenerInterface(eventManager);
            if (listenerInterface == null) {
                logger.fine("No listener interface found, using direct consumer");
                return null;
            }
            
            return Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class<?>[]{listenerInterface},
                (proxy, method, args) -> {
                    if (args != null && args.length > 0 && eventClass.isInstance(args[0])) {
                        bridgeEvent(args[0], rubidiumEventName);
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            logger.fine("Could not create listener wrapper: " + e.getMessage());
            return null;
        }
    }
    
    private Class<?> findListenerInterface(Object eventManager) {
        String[] interfaceNames = {
            "com.hypixel.hytale.event.EventListener",
            "com.hypixel.hytale.event.Listener",
            "com.hypixel.hytale.api.event.EventHandler"
        };
        
        for (String name : interfaceNames) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        
        for (Method m : eventManager.getClass().getMethods()) {
            if (m.getName().startsWith("register") || m.getName().equals("subscribe")) {
                Class<?>[] params = m.getParameterTypes();
                for (Class<?> param : params) {
                    if (param.isInterface() && !param.equals(Class.class) && !param.equals(Consumer.class)) {
                        return param;
                    }
                }
            }
        }
        
        return null;
    }
    
    private Method findRegisterMethod(Object eventManager) {
        String[] methodNames = {"register", "subscribe", "on", "addListener", "addEventListener"};
        
        for (String name : methodNames) {
            for (Method m : eventManager.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() >= 1) {
                    return m;
                }
            }
        }
        
        return null;
    }
    
    private class EventBridgeConsumer implements Consumer<Object> {
        private final String rubidiumEventName;
        
        EventBridgeConsumer(String rubidiumEventName) {
            this.rubidiumEventName = rubidiumEventName;
        }
        
        @Override
        public void accept(Object hytaleEvent) {
            bridgeEvent(hytaleEvent, rubidiumEventName);
        }
    }
    
    private void bridgeEvent(Object hytaleEvent, String rubidiumEventName) {
        try {
            RubidiumEvent rubidiumEvent = convertToRubidiumEvent(hytaleEvent, rubidiumEventName);
            if (rubidiumEvent != null) {
                fireRubidiumEvent(rubidiumEvent);
            }
        } catch (Exception e) {
            logger.warning("Failed to bridge event " + rubidiumEventName + ": " + e.getMessage());
        }
    }
    
    private RubidiumEvent convertToRubidiumEvent(Object hytaleEvent, String eventName) {
        switch (eventName) {
            case "PlayerJoinEvent":
                return new RubidiumPlayerJoinEvent(extractPlayer(hytaleEvent));
                
            case "PlayerQuitEvent":
                return new RubidiumPlayerQuitEvent(extractPlayer(hytaleEvent), extractString(hytaleEvent, "getReason", "reason"));
                
            case "PlayerChatEvent":
                return new RubidiumPlayerChatEvent(
                    extractPlayer(hytaleEvent),
                    extractString(hytaleEvent, "getMessage", "message"),
                    isCancelled(hytaleEvent)
                );
                
            case "PlayerMoveEvent":
                return new RubidiumPlayerMoveEvent(
                    extractPlayer(hytaleEvent),
                    extractLocation(hytaleEvent, "getFrom", "from"),
                    extractLocation(hytaleEvent, "getTo", "to")
                );
                
            case "PlayerDeathEvent":
                return new RubidiumPlayerDeathEvent(
                    extractPlayer(hytaleEvent),
                    extractString(hytaleEvent, "getDeathMessage", "deathMessage")
                );
                
            case "EntitySpawnEvent":
                return new RubidiumEntitySpawnEvent(extractEntity(hytaleEvent));
                
            case "EntityDeathEvent":
                return new RubidiumEntityDeathEvent(extractEntity(hytaleEvent));
                
            case "EntityDamageEvent":
                return new RubidiumEntityDamageEvent(
                    extractEntity(hytaleEvent),
                    extractDouble(hytaleEvent, "getDamage", "damage"),
                    extractString(hytaleEvent, "getCause", "cause")
                );
                
            default:
                return new RubidiumGenericEvent(eventName, hytaleEvent);
        }
    }
    
    private Player extractPlayer(Object event) {
        try {
            Method m = event.getClass().getMethod("getPlayer");
            Object hytalePlayer = m.invoke(event);
            return adapter.wrapPlayer(hytalePlayer);
        } catch (Exception e) {
            return null;
        }
    }
    
    private HytaleEntityWrapper extractEntity(Object event) {
        try {
            Method m = event.getClass().getMethod("getEntity");
            Object hytaleEntity = m.invoke(event);
            return adapter.wrapEntity(hytaleEntity);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractString(Object obj, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = obj.getClass().getMethod(name);
                return (String) m.invoke(obj);
            } catch (Exception ignored) {}
            
            try {
                var field = obj.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return (String) field.get(obj);
            } catch (Exception ignored) {}
        }
        return null;
    }
    
    private double extractDouble(Object obj, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object result = m.invoke(obj);
                if (result instanceof Number) return ((Number) result).doubleValue();
            } catch (Exception ignored) {}
        }
        return 0;
    }
    
    private Player.Location extractLocation(Object event, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = event.getClass().getMethod(name);
                Object pos = m.invoke(event);
                if (pos != null) {
                    double x = extractDouble(pos, "getX", "x");
                    double y = extractDouble(pos, "getY", "y");
                    double z = extractDouble(pos, "getZ", "z");
                    float yaw = (float) extractDouble(pos, "getYaw", "yaw");
                    float pitch = (float) extractDouble(pos, "getPitch", "pitch");
                    return new Player.Location(x, y, z, yaw, pitch);
                }
            } catch (Exception ignored) {}
        }
        return new Player.Location(0, 0, 0, 0, 0);
    }
    
    private boolean isCancelled(Object event) {
        try {
            Method m = event.getClass().getMethod("isCancelled");
            return (boolean) m.invoke(event);
        } catch (Exception e) {
            return false;
        }
    }
    
    public <T extends RubidiumEvent> void registerListener(Class<T> eventClass, Consumer<T> listener) {
        rubidiumListeners.computeIfAbsent(eventClass, k -> new ArrayList<>())
            .add(event -> listener.accept(eventClass.cast(event)));
    }
    
    @SuppressWarnings("unchecked")
    private void fireRubidiumEvent(RubidiumEvent event) {
        List<Consumer<Object>> listeners = rubidiumListeners.get(event.getClass());
        if (listeners != null) {
            for (Consumer<Object> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    logger.warning("Error in event listener: " + e.getMessage());
                }
            }
        }
    }
    
    public void unregisterAll() {
        rubidiumListeners.clear();
        registeredHytaleListeners.clear();
        logger.info("Unregistered all event listeners");
    }
    
    public interface RubidiumEvent {
        String getEventName();
    }
    
    public record RubidiumPlayerJoinEvent(Player player) implements RubidiumEvent {
        @Override public String getEventName() { return "PlayerJoinEvent"; }
    }
    
    public record RubidiumPlayerQuitEvent(Player player, String reason) implements RubidiumEvent {
        @Override public String getEventName() { return "PlayerQuitEvent"; }
    }
    
    public record RubidiumPlayerChatEvent(Player player, String message, boolean cancelled) implements RubidiumEvent {
        @Override public String getEventName() { return "PlayerChatEvent"; }
    }
    
    public record RubidiumPlayerMoveEvent(Player player, Player.Location from, Player.Location to) implements RubidiumEvent {
        @Override public String getEventName() { return "PlayerMoveEvent"; }
    }
    
    public record RubidiumPlayerDeathEvent(Player player, String deathMessage) implements RubidiumEvent {
        @Override public String getEventName() { return "PlayerDeathEvent"; }
    }
    
    public record RubidiumEntitySpawnEvent(HytaleEntityWrapper entity) implements RubidiumEvent {
        @Override public String getEventName() { return "EntitySpawnEvent"; }
    }
    
    public record RubidiumEntityDeathEvent(HytaleEntityWrapper entity) implements RubidiumEvent {
        @Override public String getEventName() { return "EntityDeathEvent"; }
    }
    
    public record RubidiumEntityDamageEvent(HytaleEntityWrapper entity, double damage, String cause) implements RubidiumEvent {
        @Override public String getEventName() { return "EntityDamageEvent"; }
    }
    
    public record RubidiumGenericEvent(String eventName, Object originalEvent) implements RubidiumEvent {
        @Override public String getEventName() { return eventName; }
    }
}
