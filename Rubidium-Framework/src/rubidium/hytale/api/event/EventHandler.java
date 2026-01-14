package rubidium.hytale.api.event;

import java.lang.annotation.*;

/**
 * Marks a method as an event handler.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    
    EventPriority priority() default EventPriority.NORMAL;
    
    boolean ignoreCancelled() default false;
}
