package rubidium.network;

import rubidium.api.player.Player;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PacketHandler {
    
    private static final Logger logger = Logger.getLogger("Rubidium-PacketHandler");
    
    private final Map<String, List<RegisteredHandler>> handlers;
    
    public PacketHandler() {
        this.handlers = new ConcurrentHashMap<>();
    }
    
    public void registerListener(Object listener) {
        Class<?> clazz = listener.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            PacketListener annotation = method.getAnnotation(PacketListener.class);
            if (annotation == null) continue;
            
            Class<?>[] params = method.getParameterTypes();
            if (params.length < 2) {
                logger.warn("PacketListener method must have at least 2 parameters (Player, packet): " + method);
                continue;
            }
            
            if (!Player.class.isAssignableFrom(params[0])) {
                logger.warn("First parameter must be Player: " + method);
                continue;
            }
            
            String packetType = annotation.value().isEmpty() ? params[1].getSimpleName() : annotation.value();
            
            method.setAccessible(true);
            
            handlers.computeIfAbsent(packetType, k -> new ArrayList<>())
                .add(new RegisteredHandler(listener, method, annotation.priority(), annotation.ignoreCancelled()));
            
            handlers.get(packetType).sort(Comparator.comparingInt(h -> -h.priority()));
            
            logger.fine("Registered packet handler: " + packetType + " -> " + method.getName());
        }
    }
    
    public void unregisterListener(Object listener) {
        for (List<RegisteredHandler> handlerList : handlers.values()) {
            handlerList.removeIf(h -> h.listener() == listener);
        }
    }
    
    public boolean handleIncoming(Player player, Object packet) {
        String packetType = packet.getClass().getSimpleName();
        return handle(packetType, player, packet, Direction.INCOMING);
    }
    
    public boolean handleOutgoing(Player player, Object packet) {
        String packetType = packet.getClass().getSimpleName();
        return handle(packetType, player, packet, Direction.OUTGOING);
    }
    
    private boolean handle(String packetType, Player player, Object packet, Direction direction) {
        List<RegisteredHandler> handlerList = handlers.get(packetType);
        if (handlerList == null || handlerList.isEmpty()) {
            return false;
        }
        
        PacketEvent event = new PacketEvent(player, packet, direction);
        
        for (RegisteredHandler handler : handlerList) {
            if (event.isCancelled() && handler.ignoreCancelled()) {
                continue;
            }
            
            try {
                handler.method().invoke(handler.listener(), player, packet, event);
            } catch (Exception e) {
                logger.error("Error handling packet " + packetType + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return event.isCancelled();
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PacketListener {
        String value() default "";
        int priority() default 0;
        boolean ignoreCancelled() default false;
    }
    
    public enum Direction {
        INCOMING, OUTGOING
    }
    
    public static class PacketEvent {
        private final Player player;
        private final Object packet;
        private final Direction direction;
        private boolean cancelled = false;
        
        public PacketEvent(Player player, Object packet, Direction direction) {
            this.player = player;
            this.packet = packet;
            this.direction = direction;
        }
        
        public Player getPlayer() { return player; }
        public Object getPacket() { return packet; }
        public Direction getDirection() { return direction; }
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
    
    private record RegisteredHandler(Object listener, Method method, int priority, boolean ignoreCancelled) {}
}
