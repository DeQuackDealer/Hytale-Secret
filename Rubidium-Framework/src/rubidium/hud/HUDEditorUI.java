package rubidium.hud;

import rubidium.api.player.Player;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.ui.components.*;

import java.util.UUID;

public class HUDEditorUI {
    
    private static final int BG_COLOR = 0x14141A;
    private static final int WIDGET_BG = 0x2D2D35;
    private static final int ACCENT_COLOR = 0x8A2BE2;
    private static final int TEXT_COLOR = 0xF0F0F5;
    
    public static void open(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        UIContainer editor = new UIContainer("hud_editor")
            .setTitle("HUD Editor")
            .setSize(800, 600)
            .setBackground(BG_COLOR)
            .setCentered(true);
        
        editor.addChild(new UIText("header")
            .setText("HUD Layout Editor")
            .setFontSize(24)
            .setColor(ACCENT_COLOR)
            .setPosition(20, 20));
        
        editor.addChild(new UIText("instructions")
            .setText("Drag widgets to reposition. Click to select, then use controls to adjust.")
            .setFontSize(12)
            .setColor(0x808090)
            .setPosition(20, 50));
        
        UIContainer previewArea = new UIContainer("preview")
            .setPosition(20, 80)
            .setSize(560, 400)
            .setBackground(0x0A0A0F);
        
        for (HUDRegistry.HUDWidget widget : HUDRegistry.get().getAllWidgets()) {
            PlayerSettings.HUDPosition pos = settings.getHUDPosition(widget.getId());
            if (pos == null) continue;
            
            UIContainer widgetPreview = createWidgetPreview(widget, pos, player);
            previewArea.addChild(widgetPreview);
        }
        
        editor.addChild(previewArea);
        
        UIContainer sidebar = new UIContainer("sidebar")
            .setPosition(600, 80)
            .setSize(180, 400)
            .setBackground(0x1E1E23);
        
        sidebar.addChild(new UIText("widgets_label")
            .setText("Widgets")
            .setFontSize(16)
            .setColor(TEXT_COLOR)
            .setPosition(10, 10));
        
        int y = 40;
        for (HUDRegistry.HUDWidget widget : HUDRegistry.get().getAllWidgets()) {
            PlayerSettings.HUDPosition pos = settings.getHUDPosition(widget.getId());
            boolean visible = pos != null && pos.isVisible();
            
            UIContainer row = new UIContainer("widget_row_" + widget.getId())
                .setPosition(10, y)
                .setSize(160, 35)
                .setBackground(WIDGET_BG);
            
            row.addChild(new UIText("widget_name_" + widget.getId())
                .setText(widget.getName())
                .setFontSize(12)
                .setColor(TEXT_COLOR)
                .setPosition(5, 5));
            
            UIButton toggleBtn = new UIButton("toggle_vis_" + widget.getId())
                .setText(visible ? "Hide" : "Show")
                .setSize(45, 25)
                .setPosition(105, 5)
                .setBackground(visible ? 0x32CD32 : 0x505060)
                .onClick(() -> toggleWidgetVisibility(widget.getId(), player, settings));
            row.addChild(toggleBtn);
            
            sidebar.addChild(row);
            y += 40;
        }
        
        editor.addChild(sidebar);
        
        UIContainer controls = new UIContainer("controls")
            .setPosition(20, 490)
            .setSize(760, 90)
            .setBackground(0x1E1E23);
        
        controls.addChild(new UIText("anchor_label")
            .setText("Anchor Point:")
            .setFontSize(12)
            .setColor(TEXT_COLOR)
            .setPosition(10, 10));
        
        int btnX = 10;
        for (PlayerSettings.AnchorPoint anchor : PlayerSettings.AnchorPoint.values()) {
            controls.addChild(new UIButton("anchor_" + anchor.name())
                .setText(anchor.name().replace("_", " "))
                .setSize(80, 25)
                .setPosition(btnX, 35)
                .setBackground(0x505060)
                .onClick(() -> setSelectedAnchor(anchor, player)));
            btnX += 85;
        }
        
        controls.addChild(new UIButton("save")
            .setText("Save Layout")
            .setSize(120, 35)
            .setPosition(520, 45)
            .setBackground(0x32CD32)
            .onClick(() -> saveLayout(player, settings)));
        
        controls.addChild(new UIButton("reset")
            .setText("Reset Layout")
            .setSize(100, 35)
            .setPosition(650, 45)
            .setBackground(0x8B0000)
            .onClick(() -> resetLayout(player, settings)));
        
        editor.addChild(controls);
        
        player.sendPacket(editor);
        player.sendMessage("&aHUD Editor opened - drag widgets to reposition them");
    }
    
    private static UIContainer createWidgetPreview(HUDRegistry.HUDWidget widget, PlayerSettings.HUDPosition pos, Player player) {
        UIContainer preview = new UIContainer("preview_" + widget.getId())
            .setPosition(pos.getX(), pos.getY())
            .setSize(pos.getWidth(), pos.getHeight())
            .setBackground(WIDGET_BG);
        
        preview.addChild(new UIText("preview_name_" + widget.getId())
            .setText(widget.getName())
            .setFontSize(10)
            .setColor(TEXT_COLOR)
            .setPosition(5, 5));
        
        preview.addChild(new UIText("preview_size_" + widget.getId())
            .setText(pos.getWidth() + "x" + pos.getHeight())
            .setFontSize(8)
            .setColor(0x808090)
            .setPosition(5, 20));
        
        return preview;
    }
    
    private static void toggleWidgetVisibility(String widgetId, Player player, PlayerSettings settings) {
        PlayerSettings.HUDPosition pos = settings.getHUDPosition(widgetId);
        if (pos != null) {
            pos.setVisible(!pos.isVisible());
            player.sendMessage("&7Widget " + widgetId + " " + (pos.isVisible() ? "shown" : "hidden"));
        }
    }
    
    private static void setSelectedAnchor(PlayerSettings.AnchorPoint anchor, Player player) {
        player.sendMessage("&7Selected anchor: " + anchor.name());
    }
    
    private static void saveLayout(Player player, PlayerSettings settings) {
        settings.save();
        player.sendMessage("&aHUD layout saved!");
    }
    
    private static void resetLayout(Player player, PlayerSettings settings) {
        settings.setHUDPosition("minimap", new PlayerSettings.HUDPosition(10, 10, 150, 150, PlayerSettings.AnchorPoint.TOP_RIGHT));
        settings.setHUDPosition("statistics", new PlayerSettings.HUDPosition(10, 10, 200, 80, PlayerSettings.AnchorPoint.TOP_LEFT));
        settings.setHUDPosition("voicechat", new PlayerSettings.HUDPosition(10, 200, 180, 120, PlayerSettings.AnchorPoint.TOP_LEFT));
        settings.setHUDPosition("waypoints", new PlayerSettings.HUDPosition(0, 0, 0, 0, PlayerSettings.AnchorPoint.CENTER));
        settings.save();
        player.sendMessage("&aHUD layout reset to defaults!");
    }
}
