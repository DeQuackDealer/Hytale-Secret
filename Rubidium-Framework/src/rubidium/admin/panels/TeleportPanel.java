package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.admin.AdminUIModule;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.ui.components.*;

import java.util.*;

public class TeleportPanel implements AdminPanel {
    
    @Override
    public String getId() { return "teleport"; }
    
    @Override
    public String getName() { return "Teleportation"; }
    
    @Override
    public String getDescription() { return "Teleport to players, coordinates, or saved locations"; }
    
    @Override
    public String getIcon() { return "ender_pearl"; }
    
    @Override
    public String getPermission() { return "rubidium.admin.teleport"; }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("teleport")
            .setTitle("Teleportation")
            .setSize(500, 400)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        panel.addChild(new UIText("title").setText("Teleportation").setFontSize(20).setColor(0x8A2BE2).setPosition(20, 20));
        
        int y = 60;
        Collection<Player> players = Server.getOnlinePlayers();
        for (Player target : players) {
            UIButton tpBtn = new UIButton("tp_" + target.getUniqueId())
                .setText("TP to " + target.getName())
                .setSize(460, 35)
                .setPosition(20, y)
                .setBackground(0x2D2D35)
                .onClick(() -> {
                    Player.Location loc = target.getLocation();
                    admin.teleport(loc.x(), loc.y(), loc.z());
                    admin.sendMessage("&aTeleported to " + target.getName());
                });
            panel.addChild(tpBtn);
            y += 40;
        }
        
        if (players.isEmpty()) {
            panel.addChild(new UIText("no_players").setText("No other players online").setFontSize(14).setColor(0x808090).setPosition(20, 60));
        }
        
        panel.addChild(new UIButton("back").setText("Back").setSize(460, 40).setPosition(20, 340).setBackground(0x505060).onClick(() -> AdminUIModule.getInstance().openMainMenu(admin)));
        
        return panel;
    }
}
