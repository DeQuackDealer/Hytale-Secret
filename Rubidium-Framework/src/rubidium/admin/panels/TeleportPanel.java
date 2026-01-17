package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.api.world.Location;
import rubidium.api.world.World;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

import java.util.*;

public class TeleportPanel implements AdminPanel {
    
    private final Map<String, Location> savedLocations = new HashMap<>();
    
    @Override
    public String getId() {
        return "teleport";
    }
    
    @Override
    public String getName() {
        return "Teleportation";
    }
    
    @Override
    public String getDescription() {
        return "Teleport to players, coordinates, or saved locations";
    }
    
    @Override
    public String getIcon() {
        return "ender_pearl";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.teleport";
    }
    
    @Override
    public int getPriority() {
        return 25;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("teleport")
            .setTitle("Teleportation")
            .setSize(450, 550)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Teleportation")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UIText coordsTitle = new UIText("coords_title")
            .setText("Teleport to Coordinates")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, 55);
        panel.addChild(coordsTitle);
        
        Location currentLoc = admin.getLocation();
        
        UITextField xField = new UITextField("x")
            .setPlaceholder("X")
            .setValue(String.format("%.1f", currentLoc.getX()))
            .setSize(90, 35)
            .setPosition(20, 80);
        panel.addChild(xField);
        
        UITextField yField = new UITextField("y")
            .setPlaceholder("Y")
            .setValue(String.format("%.1f", currentLoc.getY()))
            .setSize(90, 35)
            .setPosition(120, 80);
        panel.addChild(yField);
        
        UITextField zField = new UITextField("z")
            .setPlaceholder("Z")
            .setValue(String.format("%.1f", currentLoc.getZ()))
            .setSize(90, 35)
            .setPosition(220, 80);
        panel.addChild(zField);
        
        UIButton tpCoordsBtn = new UIButton("tp_coords")
            .setText("Teleport")
            .setSize(100, 35)
            .setPosition(320, 80)
            .setBackground(0x4169E1)
            .onClick(() -> {
                try {
                    double x = Double.parseDouble(xField.getValue());
                    double y = Double.parseDouble(yField.getValue());
                    double z = Double.parseDouble(zField.getValue());
                    Location loc = new Location(currentLoc.getWorld(), x, y, z);
                    admin.teleport(loc);
                    admin.sendMessage("&aTeleported to " + String.format("%.1f, %.1f, %.1f", x, y, z));
                } catch (NumberFormatException e) {
                    admin.sendMessage("&cInvalid coordinates");
                }
            });
        panel.addChild(tpCoordsBtn);
        
        UIText playersTitle = new UIText("players_title")
            .setText("Teleport to Player")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, 130);
        panel.addChild(playersTitle);
        
        UIContainer playerList = new UIContainer("player_list")
            .setPosition(20, 155)
            .setSize(410, 150)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        int y = 5;
        for (Player player : Server.getOnlinePlayers()) {
            if (!player.equals(admin)) {
                UIButton playerBtn = new UIButton("player_" + player.getUniqueId())
                    .setText(player.getName())
                    .setSize(195, 30)
                    .setPosition(5 + (y / 35 % 2) * 200, 5 + (y / 35 / 2) * 35)
                    .setBackground(0x2D2D35)
                    .onClick(() -> {
                        admin.teleport(player.getLocation());
                        admin.sendMessage("&aTeleported to " + player.getName());
                    });
                playerList.addChild(playerBtn);
                y += 35;
            }
        }
        
        panel.addChild(playerList);
        
        UIText savedTitle = new UIText("saved_title")
            .setText("Saved Locations")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, 320);
        panel.addChild(savedTitle);
        
        UITextField locNameField = new UITextField("loc_name")
            .setPlaceholder("Location name")
            .setSize(200, 35)
            .setPosition(20, 345);
        panel.addChild(locNameField);
        
        UIButton saveLocBtn = new UIButton("save_loc")
            .setText("Save Current")
            .setSize(100, 35)
            .setPosition(230, 345)
            .setBackground(0x32CD32)
            .onClick(() -> {
                String name = locNameField.getValue();
                if (!name.isEmpty()) {
                    savedLocations.put(name, admin.getLocation().clone());
                    admin.sendMessage("&aSaved location: " + name);
                    RubidiumUI.showUI(admin, createUI(admin));
                }
            });
        panel.addChild(saveLocBtn);
        
        UIContainer savedList = new UIContainer("saved_list")
            .setPosition(20, 390)
            .setSize(410, 100)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        y = 5;
        for (Map.Entry<String, Location> entry : savedLocations.entrySet()) {
            UIContainer savedRow = createSavedLocationRow(admin, entry.getKey(), entry.getValue(), y);
            savedList.addChild(savedRow);
            y += 35;
        }
        
        if (savedLocations.isEmpty()) {
            UIText noSaved = new UIText("no_saved")
                .setText("No saved locations")
                .setFontSize(11)
                .setColor(0x808090)
                .setPosition(150, 40);
            savedList.addChild(noSaved);
        }
        
        panel.addChild(savedList);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(410, 40)
            .setPosition(20, 500)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private UIContainer createSavedLocationRow(Player admin, String name, Location loc, int y) {
        UIContainer row = new UIContainer("saved_" + name.hashCode())
            .setPosition(5, y)
            .setSize(395, 30)
            .setBackground(0x2D2D35);
        
        UIText nameText = new UIText("name")
            .setText(name)
            .setFontSize(12)
            .setColor(0xF0F0F5)
            .setPosition(10, 8);
        row.addChild(nameText);
        
        UIText coordsText = new UIText("coords")
            .setText(String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ()))
            .setFontSize(10)
            .setColor(0x808090)
            .setPosition(150, 9);
        row.addChild(coordsText);
        
        UIButton tpBtn = new UIButton("tp")
            .setText("TP")
            .setSize(35, 22)
            .setPosition(310, 4)
            .setBackground(0x4169E1)
            .onClick(() -> {
                admin.teleport(loc);
                admin.sendMessage("&aTeleported to " + name);
            });
        row.addChild(tpBtn);
        
        UIButton deleteBtn = new UIButton("delete")
            .setText("X")
            .setSize(25, 22)
            .setPosition(350, 4)
            .setBackground(0x8B0000)
            .onClick(() -> {
                savedLocations.remove(name);
                admin.sendMessage("&aDeleted location: " + name);
                RubidiumUI.showUI(admin, createUI(admin));
            });
        row.addChild(deleteBtn);
        
        return row;
    }
}
