package rubidium.settings;

import rubidium.api.player.Player;
import rubidium.ui.components.*;
import rubidium.hud.HUDRegistry;
import rubidium.hud.HUDEditorUI;
import rubidium.voicechat.VoiceChatModule;
import rubidium.minimap.MinimapModule;
import rubidium.stats.PerformanceStatsModule;

import java.util.UUID;

public class RubidiumSettingsTab {
    
    private static final int BG_COLOR = 0x1E1E23;
    private static final int ACCENT_COLOR = 0x8A2BE2;
    private static final int TEXT_COLOR = 0xF0F0F5;
    private static final int TEXT_DIM = 0x808090;
    private static final int TOGGLE_ON = 0x32CD32;
    private static final int TOGGLE_OFF = 0x505060;
    
    public static UIContainer create(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
        SettingsRegistry.PermissionLevel permLevel = serverSettings.getPermissionLevel(playerId);
        
        UIContainer container = new UIContainer("rubidium_settings")
            .setTitle("Rubidium Settings")
            .setSize(500, 600)
            .setBackground(BG_COLOR)
            .setCentered(true);
        
        container.addChild(new UIContainer("status_indicator")
            .setPosition(470, 15)
            .setSize(12, 12)
            .setBackground(0x32CD32)
            .setCornerRadius(6));
        
        container.addChild(new UIText("status_tooltip")
            .setText("Online")
            .setFontSize(9)
            .setColor(0x32CD32)
            .setPosition(435, 17));
        
        container.addChild(new UIText("header")
            .setText("Rubidium Settings")
            .setFontSize(24)
            .setColor(ACCENT_COLOR)
            .setPosition(20, 20));
        
        container.addChild(new UIText("version_label")
            .setText("v1.0.0")
            .setFontSize(10)
            .setColor(TEXT_DIM)
            .setPosition(210, 28));
        
        container.addChild(new UIText("subtitle")
            .setText("Configure your Rubidium experience")
            .setFontSize(12)
            .setColor(TEXT_DIM)
            .setPosition(20, 50));
        
        int y = 90;
        
        y = addSection(container, y, "Display Features", new SettingItem[] {
            new SettingItem("minimap", "Minimap", "Show the minimap overlay", settings.isMinimapEnabled(), serverSettings.isMinimapAllowed()),
            new SettingItem("waypoints", "Waypoints", "Show waypoint markers", settings.isWaypointsEnabled(), serverSettings.isWaypointsAllowed()),
            new SettingItem("statistics", "Performance Stats", "Show FPS, DPS, RAM usage", settings.isStatisticsEnabled(), serverSettings.isStatisticsAllowed()),
        }, player, settings);
        
        y = addSection(container, y, "Voice Chat", new SettingItem[] {
            new SettingItem("voicechat", "Voice Chat", "Enable voice communication", settings.isVoiceChatEnabled(), serverSettings.isVoiceChatAllowed()),
        }, player, settings);
        
        y = addVoiceChatSettings(container, y, player, settings);
        
        y = addMinimapSettings(container, y, player, settings);
        
        if (permLevel == SettingsRegistry.PermissionLevel.OWNER || permLevel == SettingsRegistry.PermissionLevel.ADMIN) {
            y = addAdminSection(container, y, player, serverSettings, permLevel);
        }
        
        container.addChild(new UIButton("edit_hud")
            .setText("Edit HUD Layout")
            .setSize(220, 40)
            .setPosition(20, y)
            .setBackground(ACCENT_COLOR)
            .onClick(() -> HUDEditorUI.open(player)));
        
        container.addChild(new UIButton("reset_defaults")
            .setText("Reset to Defaults")
            .setSize(220, 40)
            .setPosition(260, y)
            .setBackground(0x8B0000)
            .onClick(() -> resetToDefaults(player, settings)));
        
        return container;
    }
    
    private static int addSection(UIContainer container, int y, String title, SettingItem[] items, Player player, PlayerSettings settings) {
        container.addChild(new UIText("section_" + title.toLowerCase().replace(" ", "_"))
            .setText(title)
            .setFontSize(16)
            .setColor(TEXT_COLOR)
            .setPosition(20, y));
        y += 30;
        
        for (SettingItem item : items) {
            y = addToggleRow(container, y, item.id, item.name, item.description, item.enabled, item.serverAllowed, player, settings);
        }
        
        return y + 10;
    }
    
    private static int addToggleRow(UIContainer container, int y, String id, String name, String description, boolean enabled, boolean serverAllowed, Player player, PlayerSettings settings) {
        UIContainer row = new UIContainer("row_" + id)
            .setPosition(20, y)
            .setSize(460, 45)
            .setBackground(0x2D2D35);
        
        row.addChild(new UIText("name_" + id)
            .setText(name)
            .setFontSize(14)
            .setColor(TEXT_COLOR)
            .setPosition(15, 8));
        
        row.addChild(new UIText("desc_" + id)
            .setText(serverAllowed ? description : "Disabled by server")
            .setFontSize(10)
            .setColor(TEXT_DIM)
            .setPosition(15, 26));
        
        if (serverAllowed) {
            UIButton toggle = new UIButton("toggle_" + id)
                .setText(enabled ? "ON" : "OFF")
                .setSize(60, 30)
                .setPosition(385, 8)
                .setBackground(enabled ? TOGGLE_ON : TOGGLE_OFF)
                .onClick(() -> toggleSetting(id, player, settings));
            row.addChild(toggle);
        } else {
            row.addChild(new UIText("disabled_" + id)
                .setText("N/A")
                .setFontSize(12)
                .setColor(0x8B0000)
                .setPosition(400, 15));
        }
        
        container.addChild(row);
        return y + 50;
    }
    
    private static int addVoiceChatSettings(UIContainer container, int y, Player player, PlayerSettings settings) {
        container.addChild(new UIText("vc_settings")
            .setText("Voice Chat Settings")
            .setFontSize(14)
            .setColor(TEXT_COLOR)
            .setPosition(40, y));
        y += 25;
        
        container.addChild(new UIText("ptt_label")
            .setText("Push-to-Talk Key: " + settings.getPushToTalkKey())
            .setFontSize(12)
            .setColor(TEXT_DIM)
            .setPosition(40, y));
        
        container.addChild(new UIButton("ptt_change")
            .setText("Change")
            .setSize(70, 25)
            .setPosition(250, y - 5)
            .setBackground(0x505060)
            .onClick(() -> promptKeybind(player, "ptt")));
        y += 35;
        
        container.addChild(new UIText("volume_label")
            .setText("Voice Volume: " + (int)(settings.getVoiceChatVolume() * 100) + "%")
            .setFontSize(12)
            .setColor(TEXT_DIM)
            .setPosition(40, y));
        y += 35;
        
        return y;
    }
    
    private static int addMinimapSettings(UIContainer container, int y, Player player, PlayerSettings settings) {
        container.addChild(new UIText("mm_settings")
            .setText("Minimap Settings")
            .setFontSize(14)
            .setColor(TEXT_COLOR)
            .setPosition(40, y));
        y += 25;
        
        container.addChild(new UIText("zoom_label")
            .setText("Zoom: " + String.format("%.1fx", settings.getMinimapZoom()))
            .setFontSize(12)
            .setColor(TEXT_DIM)
            .setPosition(40, y));
        y += 25;
        
        container.addChild(new UIText("size_label")
            .setText("Size: " + settings.getMinimapSize() + "px")
            .setFontSize(12)
            .setColor(TEXT_DIM)
            .setPosition(40, y));
        y += 25;
        
        String rotateText = settings.isMinimapRotate() ? "Rotate with player" : "Fixed north";
        container.addChild(new UIText("rotate_label")
            .setText("Rotation: " + rotateText)
            .setFontSize(12)
            .setColor(TEXT_DIM)
            .setPosition(40, y));
        y += 35;
        
        return y;
    }
    
    private static int addAdminSection(UIContainer container, int y, Player player, ServerSettings serverSettings, SettingsRegistry.PermissionLevel permLevel) {
        container.addChild(new UIText("admin_header")
            .setText(permLevel == SettingsRegistry.PermissionLevel.OWNER ? "Owner Settings" : "Admin Settings")
            .setFontSize(16)
            .setColor(0xFFD700)
            .setPosition(20, y));
        y += 30;
        
        if (permLevel == SettingsRegistry.PermissionLevel.OWNER) {
            y = addServerToggle(container, y, "opti", "Optimizations", serverSettings.isOptimizationsEnabled(), serverSettings);
            y = addServerToggle(container, y, "allow_vc", "Allow Voice Chat", serverSettings.isVoiceChatAllowed(), serverSettings);
            y = addServerToggle(container, y, "allow_mm", "Allow Minimap", serverSettings.isMinimapAllowed(), serverSettings);
            y = addServerToggle(container, y, "allow_stats", "Allow Statistics", serverSettings.isStatisticsAllowed(), serverSettings);
        }
        
        return y + 10;
    }
    
    private static int addServerToggle(UIContainer container, int y, String id, String name, boolean enabled, ServerSettings serverSettings) {
        UIContainer row = new UIContainer("admin_row_" + id)
            .setPosition(20, y)
            .setSize(460, 40)
            .setBackground(0x3D3D45);
        
        row.addChild(new UIText("admin_name_" + id)
            .setText(name)
            .setFontSize(14)
            .setColor(0xFFD700)
            .setPosition(15, 12));
        
        UIButton toggle = new UIButton("admin_toggle_" + id)
            .setText(enabled ? "ON" : "OFF")
            .setSize(60, 28)
            .setPosition(385, 6)
            .setBackground(enabled ? TOGGLE_ON : TOGGLE_OFF)
            .onClick(() -> toggleServerSetting(id, serverSettings));
        row.addChild(toggle);
        
        container.addChild(row);
        return y + 45;
    }
    
    private static void toggleSetting(String id, Player player, PlayerSettings settings) {
        switch (id) {
            case "minimap" -> settings.setMinimapEnabled(!settings.isMinimapEnabled());
            case "waypoints" -> settings.setWaypointsEnabled(!settings.isWaypointsEnabled());
            case "statistics" -> settings.setStatisticsEnabled(!settings.isStatisticsEnabled());
            case "voicechat" -> settings.setVoiceChatEnabled(!settings.isVoiceChatEnabled());
        }
        settings.save();
        player.sendMessage("&7Setting updated: " + id);
    }
    
    private static void toggleServerSetting(String id, ServerSettings serverSettings) {
        switch (id) {
            case "opti" -> serverSettings.setOptimizationsEnabled(!serverSettings.isOptimizationsEnabled());
            case "allow_vc" -> serverSettings.setVoiceChatAllowed(!serverSettings.isVoiceChatAllowed());
            case "allow_mm" -> serverSettings.setMinimapAllowed(!serverSettings.isMinimapAllowed());
            case "allow_stats" -> serverSettings.setStatisticsAllowed(!serverSettings.isStatisticsAllowed());
        }
        serverSettings.save();
    }
    
    private static void promptKeybind(Player player, String setting) {
        player.sendMessage("&ePress a key to bind to " + setting + "...");
    }
    
    private static void resetToDefaults(Player player, PlayerSettings settings) {
        settings.setMinimapEnabled(true);
        settings.setWaypointsEnabled(true);
        settings.setStatisticsEnabled(false);
        settings.setVoiceChatEnabled(true);
        settings.setMinimapZoom(1.0f);
        settings.setMinimapSize(150);
        settings.setMinimapRotate(true);
        settings.setVoiceChatVolume(1.0f);
        settings.setPushToTalkKey("V");
        settings.save();
        player.sendMessage("&aSettings reset to defaults!");
    }
    
    private record SettingItem(String id, String name, String description, boolean enabled, boolean serverAllowed) {}
}
