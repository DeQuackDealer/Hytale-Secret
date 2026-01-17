package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

public class ServerControlPanel implements AdminPanel {
    
    @Override
    public String getId() {
        return "server";
    }
    
    @Override
    public String getName() {
        return "Server Control";
    }
    
    @Override
    public String getDescription() {
        return "Control server settings, restart, and performance monitoring";
    }
    
    @Override
    public String getIcon() {
        return "server";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.server";
    }
    
    @Override
    public int getPriority() {
        return 5;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("server_control")
            .setTitle("Server Control")
            .setSize(450, 550)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Server Control Panel")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        int y = 60;
        
        UIContainer statsBox = new UIContainer("stats")
            .setPosition(20, y)
            .setSize(410, 120)
            .setBackground(0x14141A);
        
        addStatLine(statsBox, "Players Online", Server.getOnlinePlayers().size() + " / " + Server.getMaxPlayers(), 10);
        addStatLine(statsBox, "TPS", String.format("%.1f", Server.getTPS()), 35);
        addStatLine(statsBox, "Memory Usage", formatMemory(), 60);
        addStatLine(statsBox, "Uptime", formatUptime(Server.getUptime()), 85);
        
        panel.addChild(statsBox);
        y += 140;
        
        UIText actionsTitle = new UIText("actions_title")
            .setText("Quick Actions")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, y);
        panel.addChild(actionsTitle);
        y += 30;
        
        UIButton broadcastBtn = new UIButton("broadcast")
            .setText("Broadcast Message")
            .setSize(195, 40)
            .setPosition(20, y)
            .setBackground(0x4169E1)
            .onClick(() -> openBroadcastDialog(admin));
        panel.addChild(broadcastBtn);
        
        UIButton kickAllBtn = new UIButton("kick_all")
            .setText("Kick All Players")
            .setSize(195, 40)
            .setPosition(225, y)
            .setBackground(0xFFA500)
            .onClick(() -> openKickAllDialog(admin));
        panel.addChild(kickAllBtn);
        
        y += 50;
        
        UIButton whitelistBtn = new UIButton("whitelist")
            .setText("Toggle Whitelist")
            .setSize(195, 40)
            .setPosition(20, y)
            .setBackground(Server.isWhitelistEnabled() ? 0x32CD32 : 0x505060)
            .onClick(() -> {
                Server.setWhitelistEnabled(!Server.isWhitelistEnabled());
                admin.sendMessage("&aWhitelist " + (Server.isWhitelistEnabled() ? "enabled" : "disabled"));
                RubidiumUI.showUI(admin, createUI(admin));
            });
        panel.addChild(whitelistBtn);
        
        UIButton saveBtn = new UIButton("save")
            .setText("Save World")
            .setSize(195, 40)
            .setPosition(225, y)
            .setBackground(0x32CD32)
            .onClick(() -> {
                Server.saveAllWorlds();
                admin.sendMessage("&aAll worlds saved!");
            });
        panel.addChild(saveBtn);
        
        y += 50;
        
        UIButton gcBtn = new UIButton("gc")
            .setText("Run Garbage Collection")
            .setSize(195, 40)
            .setPosition(20, y)
            .setBackground(0x708090)
            .onClick(() -> {
                System.gc();
                admin.sendMessage("&aGarbage collection triggered");
                RubidiumUI.showUI(admin, createUI(admin));
            });
        panel.addChild(gcBtn);
        
        UIButton reloadBtn = new UIButton("reload")
            .setText("Reload Plugins")
            .setSize(195, 40)
            .setPosition(225, y)
            .setBackground(0xDAA520)
            .onClick(() -> openReloadDialog(admin));
        panel.addChild(reloadBtn);
        
        y += 60;
        
        UIButton restartBtn = new UIButton("restart")
            .setText("Restart Server")
            .setSize(410, 45)
            .setPosition(20, y)
            .setBackground(0x8B0000)
            .onClick(() -> openRestartDialog(admin));
        panel.addChild(restartBtn);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(410, 40)
            .setPosition(20, 480)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private void addStatLine(UIContainer container, String label, String value, int y) {
        UIText labelText = new UIText("label_" + label.toLowerCase().replace(" ", "_"))
            .setText(label + ":")
            .setFontSize(12)
            .setColor(0x808090)
            .setPosition(15, y);
        container.addChild(labelText);
        
        UIText valueText = new UIText("value_" + label.toLowerCase().replace(" ", "_"))
            .setText(value)
            .setFontSize(12)
            .setColor(0xF0F0F5)
            .setPosition(200, y);
        container.addChild(valueText);
    }
    
    private String formatMemory() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long max = rt.maxMemory() / 1024 / 1024;
        return used + " MB / " + max + " MB";
    }
    
    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    private void openBroadcastDialog(Player admin) {
        UIContainer dialog = new UIContainer("broadcast_dialog")
            .setTitle("Broadcast Message")
            .setSize(400, 180)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText prompt = new UIText("prompt")
            .setText("Enter message to broadcast:")
            .setFontSize(14)
            .setColor(0xF0F0F5)
            .setPosition(20, 30);
        dialog.addChild(prompt);
        
        UITextField messageField = new UITextField("message")
            .setPlaceholder("Your message here...")
            .setSize(360, 40)
            .setPosition(20, 60);
        dialog.addChild(messageField);
        
        UIButton sendBtn = new UIButton("send")
            .setText("Broadcast")
            .setSize(170, 40)
            .setPosition(20, 115)
            .setBackground(0x4169E1)
            .onClick(() -> {
                String message = messageField.getValue();
                if (!message.isEmpty()) {
                    Server.broadcast("&6[Broadcast] &f" + message);
                    admin.sendMessage("&aMessage broadcasted!");
                }
                RubidiumUI.showUI(admin, createUI(admin));
            });
        dialog.addChild(sendBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(170, 40)
            .setPosition(210, 115)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private void openKickAllDialog(Player admin) {
        UIContainer dialog = new UIContainer("kickall_dialog")
            .setTitle("Kick All Players")
            .setSize(350, 180)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText warning = new UIText("warning")
            .setText("This will kick ALL players!")
            .setFontSize(14)
            .setColor(0xFFA500)
            .setPosition(20, 30);
        dialog.addChild(warning);
        
        UITextField reasonField = new UITextField("reason")
            .setPlaceholder("Reason (optional)")
            .setSize(310, 40)
            .setPosition(20, 60);
        dialog.addChild(reasonField);
        
        UIButton confirmBtn = new UIButton("confirm")
            .setText("Kick All")
            .setSize(145, 40)
            .setPosition(20, 115)
            .setBackground(0xFFA500)
            .onClick(() -> {
                String reason = reasonField.getValue();
                for (Player p : Server.getOnlinePlayers()) {
                    if (!p.equals(admin)) {
                        p.kick(reason.isEmpty() ? "Server maintenance" : reason);
                    }
                }
                admin.sendMessage("&aAll players kicked!");
                RubidiumUI.showUI(admin, createUI(admin));
            });
        dialog.addChild(confirmBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(145, 40)
            .setPosition(185, 115)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private void openReloadDialog(Player admin) {
        UIContainer dialog = new UIContainer("reload_dialog")
            .setTitle("Reload Plugins")
            .setSize(350, 150)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText warning = new UIText("warning")
            .setText("Reload all plugins?")
            .setFontSize(14)
            .setColor(0xF0F0F5)
            .setPosition(20, 30);
        dialog.addChild(warning);
        
        UIText note = new UIText("note")
            .setText("This may cause temporary lag.")
            .setFontSize(11)
            .setColor(0x808090)
            .setPosition(20, 55);
        dialog.addChild(note);
        
        UIButton confirmBtn = new UIButton("confirm")
            .setText("Reload")
            .setSize(145, 40)
            .setPosition(20, 90)
            .setBackground(0xDAA520)
            .onClick(() -> {
                Server.reloadPlugins();
                admin.sendMessage("&aPlugins reloaded!");
                RubidiumUI.closeUI(admin);
            });
        dialog.addChild(confirmBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(145, 40)
            .setPosition(185, 90)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private void openRestartDialog(Player admin) {
        UIContainer dialog = new UIContainer("restart_dialog")
            .setTitle("Restart Server")
            .setSize(400, 200)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText warning = new UIText("warning")
            .setText("WARNING: This will restart the server!")
            .setFontSize(14)
            .setColor(0xFF4500)
            .setPosition(20, 30);
        dialog.addChild(warning);
        
        UIText note = new UIText("note")
            .setText("All players will be disconnected.")
            .setFontSize(12)
            .setColor(0xF0F0F5)
            .setPosition(20, 55);
        dialog.addChild(note);
        
        UIDropdown delayDropdown = new UIDropdown("delay")
            .addOption("Immediately", "0")
            .addOption("30 seconds", "30")
            .addOption("1 minute", "60")
            .addOption("5 minutes", "300")
            .setSize(360, 35)
            .setPosition(20, 85);
        dialog.addChild(delayDropdown);
        
        UIButton confirmBtn = new UIButton("confirm")
            .setText("Restart")
            .setSize(170, 40)
            .setPosition(20, 140)
            .setBackground(0x8B0000)
            .onClick(() -> {
                int delay = Integer.parseInt(delayDropdown.getSelectedValue());
                if (delay > 0) {
                    Server.broadcast("&c[Server] Restarting in " + delay + " seconds!");
                }
                Server.scheduleRestart(delay);
                admin.sendMessage("&aServer restart scheduled!");
                RubidiumUI.closeUI(admin);
            });
        dialog.addChild(confirmBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(170, 40)
            .setPosition(210, 140)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
}
