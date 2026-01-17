package rubidium.api;

public interface RubidiumModule {
    
    String getId();
    
    String getName();
    
    String getVersion();
    
    void onEnable();
    
    void onDisable();
    
    default void log(String message) {
        System.out.println("[" + getName() + "] " + message);
    }
}
