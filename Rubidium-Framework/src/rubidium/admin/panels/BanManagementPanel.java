package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.api.server.BanEntry;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class BanManagementPanel implements AdminPanel {
    
    @Override
    public String getId() {
        return "bans";
    }
    
    @Override
    public String getName() {
        return "Ban Management";
    }
    
    @Override
    public String getDescription() {
        return "Manage player bans and IP bans";
    }
    
    @Override
    public String getIcon() {
        return "barrier";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.bans";
    }
    
    @Override
    public int getPriority() {
        return 35;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("ban_management")
            .setTitle("Ban Management")
            .setSize(500, 550)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Ban Management")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UIButton playerBansTab = new UIButton("player_bans")
            .setText("Player Bans")
            .setSize(145, 35)
            .setPosition(20, 55)
            .setBackground(0x4169E1)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        panel.addChild(playerBansTab);
        
        UIButton ipBansTab = new UIButton("ip_bans")
            .setText("IP Bans")
            .setSize(145, 35)
            .setPosition(175, 55)
            .setBackground(0x2D2D35)
            .onClick(() -> showIPBans(admin));
        panel.addChild(ipBansTab);
        
        UIButton addBanBtn = new UIButton("add_ban")
            .setText("+ Add Ban")
            .setSize(145, 35)
            .setPosition(330, 55)
            .setBackground(0x8B0000)
            .onClick(() -> openAddBanDialog(admin));
        panel.addChild(addBanBtn);
        
        UIContainer banList = new UIContainer("ban_list")
            .setPosition(20, 100)
            .setSize(460, 360)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        Collection<BanEntry> bans = Server.getBannedPlayers();
        int y = 10;
        
        for (BanEntry ban : bans) {
            UIContainer row = createBanRow(admin, ban, y);
            banList.addChild(row);
            y += 65;
        }
        
        if (bans.isEmpty()) {
            UIText noBans = new UIText("no_bans")
                .setText("No active bans")
                .setFontSize(12)
                .setColor(0x808090)
                .setPosition(180, 160);
            banList.addChild(noBans);
        }
        
        panel.addChild(banList);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(460, 40)
            .setPosition(20, 480)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private UIContainer createBanRow(Player admin, BanEntry ban, int y) {
        UIContainer row = new UIContainer("ban_" + ban.getTarget().hashCode())
            .setPosition(10, y)
            .setSize(440, 60)
            .setBackground(0x2D2D35);
        
        UIText nameText = new UIText("name")
            .setText(ban.getTarget())
            .setFontSize(14)
            .setColor(0xF0F0F5)
            .setPosition(10, 8);
        row.addChild(nameText);
        
        UIText reasonText = new UIText("reason")
            .setText("Reason: " + (ban.getReason() != null ? ban.getReason() : "No reason"))
            .setFontSize(10)
            .setColor(0x808090)
            .setPosition(10, 27);
        row.addChild(reasonText);
        
        String expiryText;
        if (ban.getExpiry() == null) {
            expiryText = "Permanent";
        } else {
            long remaining = ban.getExpiry().toEpochMilli() - System.currentTimeMillis();
            if (remaining <= 0) {
                expiryText = "Expired";
            } else {
                expiryText = "Expires: " + formatDuration(remaining);
            }
        }
        
        UIText expiry = new UIText("expiry")
            .setText(expiryText)
            .setFontSize(10)
            .setColor(ban.getExpiry() == null ? 0xFF4500 : 0xA0A0AA)
            .setPosition(10, 43);
        row.addChild(expiry);
        
        UIButton unbanBtn = new UIButton("unban")
            .setText("Unban")
            .setSize(70, 30)
            .setPosition(360, 15)
            .setBackground(0x32CD32)
            .onClick(() -> {
                Server.unbanPlayer(ban.getTarget());
                admin.sendMessage("&aUnbanned " + ban.getTarget());
                RubidiumUI.showUI(admin, createUI(admin));
            });
        row.addChild(unbanBtn);
        
        return row;
    }
    
    private void showIPBans(Player admin) {
        UIContainer panel = new UIContainer("ip_bans")
            .setTitle("IP Bans")
            .setSize(500, 550)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("IP Ban Management")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UIButton playerBansTab = new UIButton("player_bans")
            .setText("Player Bans")
            .setSize(145, 35)
            .setPosition(20, 55)
            .setBackground(0x2D2D35)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        panel.addChild(playerBansTab);
        
        UIButton ipBansTab = new UIButton("ip_bans")
            .setText("IP Bans")
            .setSize(145, 35)
            .setPosition(175, 55)
            .setBackground(0x4169E1)
            .onClick(() -> {});
        panel.addChild(ipBansTab);
        
        UIButton addIPBanBtn = new UIButton("add_ip_ban")
            .setText("+ Add IP Ban")
            .setSize(145, 35)
            .setPosition(330, 55)
            .setBackground(0x8B0000)
            .onClick(() -> openAddIPBanDialog(admin));
        panel.addChild(addIPBanBtn);
        
        UIContainer banList = new UIContainer("ban_list")
            .setPosition(20, 100)
            .setSize(460, 360)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        Collection<BanEntry> ipBans = Server.getBannedIPs();
        int y = 10;
        
        for (BanEntry ban : ipBans) {
            UIContainer row = createIPBanRow(admin, ban, y);
            banList.addChild(row);
            y += 55;
        }
        
        if (ipBans.isEmpty()) {
            UIText noBans = new UIText("no_bans")
                .setText("No active IP bans")
                .setFontSize(12)
                .setColor(0x808090)
                .setPosition(170, 160);
            banList.addChild(noBans);
        }
        
        panel.addChild(banList);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(460, 40)
            .setPosition(20, 480)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        RubidiumUI.showUI(admin, panel);
    }
    
    private UIContainer createIPBanRow(Player admin, BanEntry ban, int y) {
        UIContainer row = new UIContainer("ipban_" + ban.getTarget().hashCode())
            .setPosition(10, y)
            .setSize(440, 50)
            .setBackground(0x2D2D35);
        
        UIText ipText = new UIText("ip")
            .setText(ban.getTarget())
            .setFontSize(14)
            .setColor(0xF0F0F5)
            .setPosition(10, 8);
        row.addChild(ipText);
        
        UIText reasonText = new UIText("reason")
            .setText("Reason: " + (ban.getReason() != null ? ban.getReason() : "No reason"))
            .setFontSize(10)
            .setColor(0x808090)
            .setPosition(10, 28);
        row.addChild(reasonText);
        
        UIButton unbanBtn = new UIButton("unban")
            .setText("Unban")
            .setSize(70, 30)
            .setPosition(360, 10)
            .setBackground(0x32CD32)
            .onClick(() -> {
                Server.unbanIP(ban.getTarget());
                admin.sendMessage("&aUnbanned IP: " + ban.getTarget());
                showIPBans(admin);
            });
        row.addChild(unbanBtn);
        
        return row;
    }
    
    private void openAddBanDialog(Player admin) {
        UIContainer dialog = new UIContainer("add_ban")
            .setTitle("Add Ban")
            .setSize(400, 300)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Ban Player")
            .setFontSize(16)
            .setColor(0xF0F0F5)
            .setPosition(20, 25);
        dialog.addChild(title);
        
        UIText nameLabel = new UIText("name_label")
            .setText("Player Name:")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 55);
        dialog.addChild(nameLabel);
        
        UITextField nameField = new UITextField("name")
            .setPlaceholder("Enter player name")
            .setSize(360, 35)
            .setPosition(20, 75);
        dialog.addChild(nameField);
        
        UIText reasonLabel = new UIText("reason_label")
            .setText("Reason:")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 120);
        dialog.addChild(reasonLabel);
        
        UITextField reasonField = new UITextField("reason")
            .setPlaceholder("Ban reason")
            .setSize(360, 35)
            .setPosition(20, 140);
        dialog.addChild(reasonField);
        
        UIText durationLabel = new UIText("duration_label")
            .setText("Duration:")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 185);
        dialog.addChild(durationLabel);
        
        UIDropdown durationDropdown = new UIDropdown("duration")
            .addOption("1 Hour", "1h")
            .addOption("1 Day", "1d")
            .addOption("7 Days", "7d")
            .addOption("30 Days", "30d")
            .addOption("Permanent", "permanent")
            .setSize(360, 35)
            .setPosition(20, 205);
        dialog.addChild(durationDropdown);
        
        UIButton banBtn = new UIButton("ban")
            .setText("Ban")
            .setSize(170, 40)
            .setPosition(20, 255)
            .setBackground(0x8B0000)
            .onClick(() -> {
                String name = nameField.getValue();
                String reason = reasonField.getValue();
                String duration = durationDropdown.getSelectedValue();
                
                if (!name.isEmpty()) {
                    Server.banPlayer(name, reason.isEmpty() ? "Banned by admin" : reason, duration);
                    admin.sendMessage("&aBanned " + name + " for " + duration);
                    RubidiumUI.showUI(admin, createUI(admin));
                }
            });
        dialog.addChild(banBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(170, 40)
            .setPosition(210, 255)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private void openAddIPBanDialog(Player admin) {
        UIContainer dialog = new UIContainer("add_ip_ban")
            .setTitle("Add IP Ban")
            .setSize(400, 250)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Ban IP Address")
            .setFontSize(16)
            .setColor(0xF0F0F5)
            .setPosition(20, 25);
        dialog.addChild(title);
        
        UIText ipLabel = new UIText("ip_label")
            .setText("IP Address:")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 55);
        dialog.addChild(ipLabel);
        
        UITextField ipField = new UITextField("ip")
            .setPlaceholder("e.g. 192.168.1.1")
            .setSize(360, 35)
            .setPosition(20, 75);
        dialog.addChild(ipField);
        
        UIText reasonLabel = new UIText("reason_label")
            .setText("Reason:");
        reasonLabel.setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 120);
        dialog.addChild(reasonLabel);
        
        UITextField reasonField = new UITextField("reason")
            .setPlaceholder("Ban reason")
            .setSize(360, 35)
            .setPosition(20, 140);
        dialog.addChild(reasonField);
        
        UIButton banBtn = new UIButton("ban")
            .setText("Ban IP")
            .setSize(170, 40)
            .setPosition(20, 195)
            .setBackground(0x8B0000)
            .onClick(() -> {
                String ip = ipField.getValue();
                String reason = reasonField.getValue();
                
                if (!ip.isEmpty()) {
                    Server.banIP(ip, reason.isEmpty() ? "IP banned by admin" : reason);
                    admin.sendMessage("&aBanned IP: " + ip);
                    showIPBans(admin);
                }
            });
        dialog.addChild(banBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(170, 40)
            .setPosition(210, 195)
            .setBackground(0x505060)
            .onClick(() -> showIPBans(admin));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        long days = hours / 24;
        return days + "d";
    }
}
