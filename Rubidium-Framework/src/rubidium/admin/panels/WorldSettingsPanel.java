package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.api.world.World;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

import java.util.Collection;

public class WorldSettingsPanel implements AdminPanel {
    
    @Override
    public String getId() {
        return "worlds";
    }
    
    @Override
    public String getName() {
        return "World Settings";
    }
    
    @Override
    public String getDescription() {
        return "Manage world settings, time, weather, and game rules";
    }
    
    @Override
    public String getIcon() {
        return "world";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.worlds";
    }
    
    @Override
    public int getPriority() {
        return 20;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("world_settings")
            .setTitle("World Settings")
            .setSize(450, 550)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("World Management")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        Collection<World> worlds = Server.getWorlds();
        
        UIDropdown worldSelector = new UIDropdown("world_select")
            .setSize(410, 40)
            .setPosition(20, 55);
        
        for (World world : worlds) {
            worldSelector.addOption(world.getName(), world.getName());
        }
        
        worldSelector.onChange(value -> {
            World selected = Server.getWorld(value);
            if (selected != null) {
                updateWorldInfo(panel, selected);
            }
        });
        panel.addChild(worldSelector);
        
        World currentWorld = admin.getLocation().getWorld();
        if (currentWorld != null) {
            worldSelector.setSelected(currentWorld.getName());
            createWorldControls(panel, admin, currentWorld);
        }
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(410, 40)
            .setPosition(20, 480)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private void createWorldControls(UIContainer panel, Player admin, World world) {
        int y = 110;
        
        UIContainer infoBox = new UIContainer("info")
            .setPosition(20, y)
            .setSize(410, 80)
            .setBackground(0x14141A);
        
        addInfoLine(infoBox, "Players", String.valueOf(world.getPlayers().size()), 10);
        addInfoLine(infoBox, "Time", formatTime(world.getTime()), 30);
        addInfoLine(infoBox, "Weather", world.getWeather().toString(), 50);
        
        panel.addChild(infoBox);
        y += 100;
        
        UIText timeTitle = new UIText("time_title")
            .setText("Time Control")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, y);
        panel.addChild(timeTitle);
        y += 25;
        
        UIButton dayBtn = new UIButton("day")
            .setText("Day")
            .setSize(95, 35)
            .setPosition(20, y)
            .setBackground(0xFFD700)
            .setTextColor(0x000000)
            .onClick(() -> {
                world.setTime(1000);
                admin.sendMessage("&aSet time to day");
            });
        panel.addChild(dayBtn);
        
        UIButton noonBtn = new UIButton("noon")
            .setText("Noon")
            .setSize(95, 35)
            .setPosition(125, y)
            .setBackground(0xFFA500)
            .onClick(() -> {
                world.setTime(6000);
                admin.sendMessage("&aSet time to noon");
            });
        panel.addChild(noonBtn);
        
        UIButton nightBtn = new UIButton("night")
            .setText("Night")
            .setSize(95, 35)
            .setPosition(230, y)
            .setBackground(0x191970)
            .onClick(() -> {
                world.setTime(13000);
                admin.sendMessage("&aSet time to night");
            });
        panel.addChild(nightBtn);
        
        UIButton midnightBtn = new UIButton("midnight")
            .setText("Midnight")
            .setSize(95, 35)
            .setPosition(335, y)
            .setBackground(0x0D0D2B)
            .onClick(() -> {
                world.setTime(18000);
                admin.sendMessage("&aSet time to midnight");
            });
        panel.addChild(midnightBtn);
        
        y += 50;
        
        UIText weatherTitle = new UIText("weather_title")
            .setText("Weather Control")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, y);
        panel.addChild(weatherTitle);
        y += 25;
        
        UIButton clearBtn = new UIButton("clear")
            .setText("Clear")
            .setSize(125, 35)
            .setPosition(20, y)
            .setBackground(0x87CEEB)
            .setTextColor(0x000000)
            .onClick(() -> {
                world.setWeather(World.Weather.CLEAR);
                admin.sendMessage("&aWeather set to clear");
            });
        panel.addChild(clearBtn);
        
        UIButton rainBtn = new UIButton("rain")
            .setText("Rain")
            .setSize(125, 35)
            .setPosition(155, y)
            .setBackground(0x4682B4)
            .onClick(() -> {
                world.setWeather(World.Weather.RAIN);
                admin.sendMessage("&aWeather set to rain");
            });
        panel.addChild(rainBtn);
        
        UIButton stormBtn = new UIButton("storm")
            .setText("Thunder")
            .setSize(125, 35)
            .setPosition(290, y)
            .setBackground(0x2F4F4F)
            .onClick(() -> {
                world.setWeather(World.Weather.THUNDER);
                admin.sendMessage("&aWeather set to thunderstorm");
            });
        panel.addChild(stormBtn);
        
        y += 50;
        
        UIText rulesTitle = new UIText("rules_title")
            .setText("Game Rules")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, y);
        panel.addChild(rulesTitle);
        y += 25;
        
        addGameRuleToggle(panel, world, admin, "PvP", "pvp", y, 20);
        addGameRuleToggle(panel, world, admin, "Mob Spawning", "doMobSpawning", y, 150);
        addGameRuleToggle(panel, world, admin, "Keep Inventory", "keepInventory", y, 280);
        
        y += 45;
        
        addGameRuleToggle(panel, world, admin, "Fire Spread", "doFireTick", y, 20);
        addGameRuleToggle(panel, world, admin, "Daylight Cycle", "doDaylightCycle", y, 150);
        addGameRuleToggle(panel, world, admin, "Weather Cycle", "doWeatherCycle", y, 280);
    }
    
    private void addInfoLine(UIContainer container, String label, String value, int y) {
        UIText labelText = new UIText("label_" + label.toLowerCase())
            .setText(label + ":")
            .setFontSize(11)
            .setColor(0x808090)
            .setPosition(15, y);
        container.addChild(labelText);
        
        UIText valueText = new UIText("value_" + label.toLowerCase())
            .setText(value)
            .setFontSize(11)
            .setColor(0xF0F0F5)
            .setPosition(150, y);
        container.addChild(valueText);
    }
    
    private void addGameRuleToggle(UIContainer panel, World world, Player admin, 
            String label, String rule, int y, int x) {
        boolean enabled = world.getGameRule(rule);
        
        UIButton toggle = new UIButton("rule_" + rule)
            .setText(label + ": " + (enabled ? "ON" : "OFF"))
            .setSize(120, 35)
            .setPosition(x, y)
            .setBackground(enabled ? 0x32CD32 : 0x505060)
            .onClick(() -> {
                world.setGameRule(rule, !enabled);
                admin.sendMessage("&a" + label + " " + (!enabled ? "enabled" : "disabled"));
                RubidiumUI.showUI(admin, createUI(admin));
            });
        panel.addChild(toggle);
    }
    
    private String formatTime(long time) {
        int hours = (int) ((time / 1000 + 6) % 24);
        int minutes = (int) ((time % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }
    
    private void updateWorldInfo(UIContainer panel, World world) {
    }
}
