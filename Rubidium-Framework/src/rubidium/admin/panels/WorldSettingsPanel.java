package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.admin.AdminUIModule;
import rubidium.api.player.Player;
import rubidium.ui.components.*;

public class WorldSettingsPanel implements AdminPanel {
    
    @Override
    public String getId() { return "worlds"; }
    
    @Override
    public String getName() { return "World Settings"; }
    
    @Override
    public String getDescription() { return "Manage world settings, time, weather, and game rules"; }
    
    @Override
    public String getIcon() { return "world"; }
    
    @Override
    public String getPermission() { return "rubidium.admin.worlds"; }
    
    @Override
    public UIContainer createUI(Player player) {
        UIContainer panel = new UIContainer("world_settings")
            .setTitle("World Settings")
            .setSize(400, 300)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        panel.addChild(new UIText("title").setText("World Settings").setFontSize(20).setColor(0x8A2BE2).setPosition(20, 20));
        panel.addChild(new UIText("info").setText("World settings features - Coming soon").setFontSize(14).setColor(0x808090).setPosition(20, 60));
        panel.addChild(new UIButton("back").setText("Back").setSize(360, 40).setPosition(20, 240).setBackground(0x505060).onClick(() -> AdminUIModule.getInstance().openMainMenu(player)));
        
        return panel;
    }
}
