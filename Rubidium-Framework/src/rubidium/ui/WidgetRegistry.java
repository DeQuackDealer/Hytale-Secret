package rubidium.ui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for UI widget types.
 */
public class WidgetRegistry {
    
    private static final Logger logger = Logger.getLogger("Rubidium-WidgetRegistry");
    
    private final Map<String, Class<? extends Widget>> widgetTypes = new ConcurrentHashMap<>();
    
    public void register(String type, Class<? extends Widget> widgetClass) {
        widgetTypes.put(type, widgetClass);
        logger.fine("Registered widget type: " + type);
    }
    
    public void unregister(String type) {
        widgetTypes.remove(type);
    }
    
    public Widget create(String type, String id) {
        Class<? extends Widget> widgetClass = widgetTypes.get(type);
        if (widgetClass == null) {
            throw new IllegalArgumentException("Unknown widget type: " + type);
        }
        
        try {
            return widgetClass.getConstructor(String.class).newInstance(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create widget: " + type, e);
        }
    }
    
    public boolean hasType(String type) {
        return widgetTypes.containsKey(type);
    }
    
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(widgetTypes.keySet());
    }
    
    public int getWidgetCount() {
        return widgetTypes.size();
    }
}
