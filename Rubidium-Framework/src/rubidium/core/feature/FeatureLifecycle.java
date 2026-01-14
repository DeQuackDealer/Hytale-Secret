package rubidium.core.feature;

/**
 * Interface for features that support lifecycle management and graceful degradation.
 */
public interface FeatureLifecycle {
    
    String getFeatureId();
    
    String getFeatureName();
    
    default String getDescription() {
        return getFeatureName() + " feature";
    }
    
    void initialize() throws FeatureInitException;
    
    void start();
    
    void stop();
    
    void shutdown();
    
    default void onReload() {
        stop();
        start();
    }
    
    FeatureHealth healthCheck();
    
    default boolean isOptional() {
        return true;
    }
    
    default FeaturePriority getPriority() {
        return FeaturePriority.NORMAL;
    }
    
    default String[] getDependencies() {
        return new String[0];
    }
    
    enum FeaturePriority {
        CRITICAL(0),
        HIGH(1),
        NORMAL(2),
        LOW(3);
        
        private final int order;
        
        FeaturePriority(int order) {
            this.order = order;
        }
        
        public int getOrder() {
            return order;
        }
    }
    
    class FeatureInitException extends Exception {
        public FeatureInitException(String message) {
            super(message);
        }
        
        public FeatureInitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
