package rubidium.qol;

import rubidium.core.logging.RubidiumLogger;

public abstract class QoLFeature {
    
    protected final String id;
    protected final String name;
    protected final String description;
    protected final RubidiumLogger logger;
    protected volatile boolean enabled;
    
    protected QoLFeature(String id, String name, String description, RubidiumLogger logger) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.logger = logger;
        this.enabled = false;
    }
    
    protected QoLFeature(String id, String name, RubidiumLogger logger) {
        this(id, name, name + " feature", logger);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void enable() {
        if (!enabled) {
            enabled = true;
            onEnable();
            logger.info("QoL feature '{}' enabled", name);
        }
    }
    
    public void disable() {
        if (enabled) {
            enabled = false;
            onDisable();
            logger.info("QoL feature '{}' disabled", name);
        }
    }
    
    public boolean toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
        return enabled;
    }
    
    protected abstract void onEnable();
    
    protected abstract void onDisable();
    
    public void tick() {
    }
    
    public void reload() {
    }
}
