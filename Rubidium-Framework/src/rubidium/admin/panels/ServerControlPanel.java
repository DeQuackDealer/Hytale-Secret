package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.admin.AdminUIModule;
import rubidium.api.player.Player;
import rubidium.ui.components.*;

public class ServerControlPanel implements AdminPanel {
    
    @Override
    public String getId() { return "server"; }
    
    @Override
    public String getName() { return "Server Control"; }
    
    @Override
    public String getDescription() { return "Control server settings, restart, and performance monitoring"; }
    
    @Override
    public String getIcon() { return "server"; }
    
    @Override
    public String getPermission() { return "rubidium.admin.server"; }
    
    @Override
    public UIContainer createUI(Player player) {
        UIContainer panel = new UIContainer("server_control")
            .setTitle("Server Control")
            .setSize(400, 300)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        panel.addChild(new UIText("title").setText("Server Control").setFontSize(20).setColor(0x8A2BE2).setPosition(20, 20));
        panel.addChild(new UIText("info").setText("Server control features - Coming soon").setFontSize(14).setColor(0x808090).setPosition(20, 60));
        panel.addChild(new UIButton("back").setText("Back").setSize(360, 40).setPosition(20, 240).setBackground(0x505060).onClick(() -> AdminUIModule.getInstance().openMainMenu(player)));
        
        return panel;
    }
}
