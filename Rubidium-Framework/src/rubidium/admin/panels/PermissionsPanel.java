package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.admin.AdminUIModule;
import rubidium.api.player.Player;
import rubidium.ui.components.*;

public class PermissionsPanel implements AdminPanel {
    
    @Override
    public String getId() { return "permissions"; }
    
    @Override
    public String getName() { return "Permissions"; }
    
    @Override
    public String getDescription() { return "Manage player permissions and groups"; }
    
    @Override
    public String getIcon() { return "key"; }
    
    @Override
    public String getPermission() { return "rubidium.admin.permissions"; }
    
    @Override
    public UIContainer createUI(Player player) {
        UIContainer panel = new UIContainer("permissions")
            .setTitle("Permissions")
            .setSize(400, 300)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        panel.addChild(new UIText("title").setText("Permissions Manager").setFontSize(20).setColor(0x8A2BE2).setPosition(20, 20));
        panel.addChild(new UIText("info").setText("Permission management features - Coming soon").setFontSize(14).setColor(0x808090).setPosition(20, 60));
        panel.addChild(new UIButton("back").setText("Back").setSize(360, 40).setPosition(20, 240).setBackground(0x505060).onClick(() -> AdminUIModule.getInstance().openMainMenu(player)));
        
        return panel;
    }
}
