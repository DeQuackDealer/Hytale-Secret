package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.admin.AdminUIModule;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.ui.components.*;

import java.util.*;

public class BanManagementPanel implements AdminPanel {
    
    @Override
    public String getId() { return "bans"; }
    
    @Override
    public String getName() { return "Ban Management"; }
    
    @Override
    public String getDescription() { return "Manage player bans and IP bans"; }
    
    @Override
    public String getIcon() { return "barrier"; }
    
    @Override
    public String getPermission() { return "rubidium.admin.bans"; }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("ban_management")
            .setTitle("Ban Management")
            .setSize(500, 400)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        panel.addChild(new UIText("title").setText("Ban Management").setFontSize(20).setColor(0x8A2BE2).setPosition(20, 20));
        
        Collection<Server.BanEntry> bans = Server.getAllBans();
        if (bans.isEmpty()) {
            panel.addChild(new UIText("no_bans").setText("No active bans").setFontSize(14).setColor(0x808090).setPosition(20, 60));
        } else {
            int y = 60;
            for (Server.BanEntry ban : bans) {
                UIContainer row = new UIContainer("ban_" + ban.uuid())
                    .setPosition(20, y)
                    .setSize(460, 40)
                    .setBackground(0x2D2D35);
                
                row.addChild(new UIText("uuid").setText(ban.uuid().toString().substring(0, 8) + "...").setFontSize(12).setColor(0xF0F0F5).setPosition(10, 8));
                row.addChild(new UIText("reason").setText(ban.reason()).setFontSize(10).setColor(0x808090).setPosition(10, 24));
                row.addChild(new UIButton("unban").setText("Unban").setSize(60, 30).setPosition(390, 5).setBackground(0x32CD32).onClick(() -> {
                    Server.unbanPlayer(ban.uuid());
                    admin.sendMessage("&aUnbanned player");
                }));
                
                panel.addChild(row);
                y += 45;
            }
        }
        
        panel.addChild(new UIButton("back").setText("Back").setSize(460, 40).setPosition(20, 340).setBackground(0x505060).onClick(() -> AdminUIModule.getInstance().openMainMenu(admin)));
        
        return panel;
    }
}
