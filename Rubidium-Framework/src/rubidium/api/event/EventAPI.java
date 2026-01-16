package rubidium.api.event;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class EventAPI {
    
    private static final Map<Class<? extends Event>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
    
    private EventAPI() {}
    
    @SuppressWarnings("unchecked")
    public static <T extends Event> void register(Class<T> eventClass, Consumer<T> handler) {
        register(eventClass, handler, EventPriority.NORMAL, false);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Event> void register(Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
        register(eventClass, handler, priority, false);
    }
    
    public static <T extends Event> void register(Class<T> eventClass, Consumer<T> handler, EventPriority priority, boolean ignoreCancelled) {
        EventHandler<T> eh = new EventHandler<>(eventClass, handler, priority, ignoreCancelled, null);
        handlers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(eh);
        sortHandlers(eventClass);
    }
    
    public static void registerListener(Object listener) {
        for (Method method : listener.getClass().getMethods()) {
            EventListener annotation = method.getAnnotation(EventListener.class);
            if (annotation != null && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(paramType)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Event> eventClass = (Class<? extends Event>) paramType;
                    
                    Consumer<Event> handler = event -> {
                        try {
                            method.invoke(listener, event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                    
                    EventHandler<Event> eh = new EventHandler<>(
                        (Class<Event>) eventClass, 
                        handler, 
                        annotation.priority(), 
                        annotation.ignoreCancelled(),
                        listener
                    );
                    handlers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(eh);
                    sortHandlers(eventClass);
                }
            }
        }
    }
    
    public static void unregisterListener(Object listener) {
        for (List<EventHandler<?>> handlerList : handlers.values()) {
            handlerList.removeIf(h -> h.owner() == listener);
        }
    }
    
    public static void unregisterAll(Class<? extends Event> eventClass) {
        handlers.remove(eventClass);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Event> T fire(T event) {
        List<EventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler<?> handler : eventHandlers) {
                if (event instanceof Cancellable cancellable) {
                    if (cancellable.isCancelled() && handler.ignoreCancelled()) {
                        continue;
                    }
                }
                ((EventHandler<T>) handler).handler().accept(event);
            }
        }
        return event;
    }
    
    public static boolean fireAndCheck(Cancellable event) {
        fire((Event) event);
        return !event.isCancelled();
    }
    
    private static void sortHandlers(Class<? extends Event> eventClass) {
        List<EventHandler<?>> handlerList = handlers.get(eventClass);
        if (handlerList != null) {
            handlerList.sort(Comparator.comparingInt(h -> h.priority().ordinal()));
        }
    }
    
    public enum EventPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST,
        MONITOR
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EventListener {
        EventPriority priority() default EventPriority.NORMAL;
        boolean ignoreCancelled() default false;
    }
    
    public record EventHandler<T extends Event>(
        Class<T> eventClass,
        Consumer<T> handler,
        EventPriority priority,
        boolean ignoreCancelled,
        Object owner
    ) {}
    
    public abstract static class Event {
        private final long timestamp = System.currentTimeMillis();
        private final String name;
        private boolean async = false;
        
        protected Event() {
            this.name = getClass().getSimpleName();
        }
        
        protected Event(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
        public long getTimestamp() { return timestamp; }
        public boolean isAsync() { return async; }
        protected void setAsync(boolean async) { this.async = async; }
    }
    
    public interface Cancellable {
        boolean isCancelled();
        void setCancelled(boolean cancelled);
    }
    
    public abstract static class CancellableEvent extends Event implements Cancellable {
        private boolean cancelled = false;
        
        @Override
        public boolean isCancelled() { return cancelled; }
        
        @Override
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
    
    public static class PlayerJoinEvent extends Event {
        private final Object player;
        private String joinMessage;
        
        public PlayerJoinEvent(Object player, String joinMessage) {
            this.player = player;
            this.joinMessage = joinMessage;
        }
        
        public Object getPlayer() { return player; }
        public String getJoinMessage() { return joinMessage; }
        public void setJoinMessage(String msg) { this.joinMessage = msg; }
    }
    
    public static class PlayerQuitEvent extends Event {
        private final Object player;
        private String quitMessage;
        
        public PlayerQuitEvent(Object player, String quitMessage) {
            this.player = player;
            this.quitMessage = quitMessage;
        }
        
        public Object getPlayer() { return player; }
        public String getQuitMessage() { return quitMessage; }
        public void setQuitMessage(String msg) { this.quitMessage = msg; }
    }
    
    public static class PlayerChatEvent extends CancellableEvent {
        private final Object player;
        private String message;
        private String format = "<%s> %s";
        
        public PlayerChatEvent(Object player, String message) {
            this.player = player;
            this.message = message;
        }
        
        public Object getPlayer() { return player; }
        public String getMessage() { return message; }
        public void setMessage(String msg) { this.message = msg; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
    
    public static class BlockBreakEvent extends CancellableEvent {
        private final Object player;
        private final Object block;
        private boolean dropItems = true;
        
        public BlockBreakEvent(Object player, Object block) {
            this.player = player;
            this.block = block;
        }
        
        public Object getPlayer() { return player; }
        public Object getBlock() { return block; }
        public boolean shouldDropItems() { return dropItems; }
        public void setDropItems(boolean drop) { this.dropItems = drop; }
    }
    
    public static class EntityDamageEvent extends CancellableEvent {
        private final Object entity;
        private final Object damager;
        private double damage;
        private final DamageCause cause;
        
        public EntityDamageEvent(Object entity, Object damager, double damage, DamageCause cause) {
            this.entity = entity;
            this.damager = damager;
            this.damage = damage;
            this.cause = cause;
        }
        
        public Object getEntity() { return entity; }
        public Object getDamager() { return damager; }
        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }
        public DamageCause getCause() { return cause; }
        
        public enum DamageCause {
            ATTACK, FALL, FIRE, LAVA, DROWN, EXPLOSION, PROJECTILE, MAGIC, VOID, CUSTOM
        }
    }
}
