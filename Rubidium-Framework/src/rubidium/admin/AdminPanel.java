package rubidium.admin;

import rubidium.api.player.Player;
import rubidium.ui.components.UIContainer;

public interface AdminPanel {
    
    String getId();
    
    String getName();
    
    String getDescription();
    
    String getIcon();
    
    String getPermission();
    
    UIContainer createUI(Player player);
    
    default int getPriority() {
        return 100;
    }
    
    default boolean isVisible(Player player) {
        String permission = getPermission();
        return permission == null || player.hasPermission(permission);
    }
}
