package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

import java.util.Collection;

public class PlayerManagementPanel implements AdminPanel {
    
    @Override
    public String getId() {
        return "players";
    }
    
    @Override
    public String getName() {
        return "Player Management";
    }
    
    @Override
    public String getDescription() {
        return "Manage online players - kick, ban, teleport, and more";
    }
    
    @Override
    public String getIcon() {
        return "player_head";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.players";
    }
    
    @Override
    public int getPriority() {
        return 10;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("player_management")
            .setTitle("Player Management")
            .setSize(500, 600)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Online Players")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UIContainer playerList = new UIContainer("player_list")
            .setPosition(20, 60)
            .setSize(460, 450)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        Collection<Player> players = Server.getOnlinePlayers();
        int y = 10;
        
        for (Player player : players) {
            UIContainer playerRow = createPlayerRow(admin, player, y);
            playerList.addChild(playerRow);
            y += 55;
        }
        
        if (players.isEmpty()) {
            UIText noPlayers = new UIText("no_players")
                .setText("No players online")
                .setColor(0x808080)
                .setPosition(180, 200);
            playerList.addChild(noPlayers);
        }
        
        panel.addChild(playerList);
        
        UIButton refreshBtn = new UIButton("refresh")
            .setText("Refresh")
            .setSize(100, 35)
            .setPosition(20, 520)
            .setBackground(0x2D2D35)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        panel.addChild(refreshBtn);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(120, 35)
            .setPosition(360, 520)
            .setBackground(0x8B0000)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private UIContainer createPlayerRow(Player admin, Player target, int yOffset) {
        UIContainer row = new UIContainer("player_" + target.getUniqueId())
            .setPosition(10, yOffset)
            .setSize(440, 50)
            .setBackground(0x2D2D35);
        
        UIText nameText = new UIText("name")
            .setText(target.getName())
            .setFontSize(14)
            .setColor(0xF0F0F5)
            .setPosition(10, 8);
        row.addChild(nameText);
        
        UIText infoText = new UIText("info")
            .setText("Health: " + (int)target.getHealth() + " | Ping: " + target.getPing() + "ms")
            .setFontSize(10)
            .setColor(0x808090)
            .setPosition(10, 28);
        row.addChild(infoText);
        
        UIButton tpBtn = new UIButton("tp")
            .setText("TP")
            .setSize(40, 30)
            .setPosition(250, 10)
            .setBackground(0x4169E1)
            .onClick(() -> {
                admin.teleport(target.getLocation());
                admin.sendMessage("&aTeleported to " + target.getName());
            });
        row.addChild(tpBtn);
        
        UIButton kickBtn = new UIButton("kick")
            .setText("Kick")
            .setSize(50, 30)
            .setPosition(295, 10)
            .setBackground(0xFFA500)
            .onClick(() -> openKickDialog(admin, target));
        row.addChild(kickBtn);
        
        UIButton banBtn = new UIButton("ban")
            .setText("Ban")
            .setSize(45, 30)
            .setPosition(350, 10)
            .setBackground(0x8B0000)
            .onClick(() -> openBanDialog(admin, target));
        row.addChild(banBtn);
        
        UIButton moreBtn = new UIButton("more")
            .setText("...")
            .setSize(35, 30)
            .setPosition(400, 10)
            .setBackground(0x505060)
            .onClick(() -> openPlayerDetails(admin, target));
        row.addChild(moreBtn);
        
        return row;
    }
    
    private void openKickDialog(Player admin, Player target) {
        UIContainer dialog = new UIContainer("kick_dialog")
            .setTitle("Kick Player")
            .setSize(350, 200)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText prompt = new UIText("prompt")
            .setText("Kick " + target.getName() + "?")
            .setFontSize(16)
            .setColor(0xF0F0F5)
            .setPosition(20, 30);
        dialog.addChild(prompt);
        
        UITextField reasonField = new UITextField("reason")
            .setPlaceholder("Reason (optional)")
            .setSize(310, 35)
            .setPosition(20, 70);
        dialog.addChild(reasonField);
        
        UIButton confirmBtn = new UIButton("confirm")
            .setText("Kick")
            .setSize(145, 40)
            .setPosition(20, 130)
            .setBackground(0xFFA500)
            .onClick(() -> {
                String reason = reasonField.getValue();
                target.kick(reason.isEmpty() ? "Kicked by admin" : reason);
                admin.sendMessage("&aKicked " + target.getName());
                RubidiumUI.showUI(admin, createUI(admin));
            });
        dialog.addChild(confirmBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(145, 40)
            .setPosition(185, 130)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private void openBanDialog(Player admin, Player target) {
        UIContainer dialog = new UIContainer("ban_dialog")
            .setTitle("Ban Player")
            .setSize(350, 280)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText prompt = new UIText("prompt")
            .setText("Ban " + target.getName() + "?")
            .setFontSize(16)
            .setColor(0xF0F0F5)
            .setPosition(20, 30);
        dialog.addChild(prompt);
        
        UITextField reasonField = new UITextField("reason")
            .setPlaceholder("Reason")
            .setSize(310, 35)
            .setPosition(20, 70);
        dialog.addChild(reasonField);
        
        UIDropdown durationDropdown = new UIDropdown("duration")
            .addOption("1 Hour", "1h")
            .addOption("1 Day", "1d")
            .addOption("7 Days", "7d")
            .addOption("30 Days", "30d")
            .addOption("Permanent", "permanent")
            .setSize(310, 35)
            .setPosition(20, 120);
        dialog.addChild(durationDropdown);
        
        UIButton confirmBtn = new UIButton("confirm")
            .setText("Ban")
            .setSize(145, 40)
            .setPosition(20, 200)
            .setBackground(0x8B0000)
            .onClick(() -> {
                String reason = reasonField.getValue();
                String duration = durationDropdown.getSelectedValue();
                Server.banPlayer(target.getUniqueId(), reason, duration);
                target.kick("Banned: " + reason);
                admin.sendMessage("&aBanned " + target.getName() + " for " + duration);
                RubidiumUI.showUI(admin, createUI(admin));
            });
        dialog.addChild(confirmBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(145, 40)
            .setPosition(185, 200)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private void openPlayerDetails(Player admin, Player target) {
        UIContainer details = new UIContainer("player_details")
            .setTitle("Player Details: " + target.getName())
            .setSize(400, 450)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        int y = 30;
        int lineHeight = 25;
        
        addDetailLine(details, "UUID", target.getUniqueId().toString(), y); y += lineHeight;
        addDetailLine(details, "Health", String.format("%.1f / %.1f", target.getHealth(), target.getMaxHealth()), y); y += lineHeight;
        addDetailLine(details, "Location", formatLocation(target.getLocation()), y); y += lineHeight;
        addDetailLine(details, "Game Mode", target.getGameMode().toString(), y); y += lineHeight;
        addDetailLine(details, "Ping", target.getPing() + " ms", y); y += lineHeight;
        addDetailLine(details, "Is Flying", String.valueOf(target.isFlying()), y); y += lineHeight;
        addDetailLine(details, "Is Op", String.valueOf(target.isOp()), y); y += lineHeight;
        
        y += 20;
        
        UIButton healBtn = new UIButton("heal")
            .setText("Heal")
            .setSize(110, 35)
            .setPosition(20, y)
            .setBackground(0x32CD32)
            .onClick(() -> {
                target.setHealth(target.getMaxHealth());
                admin.sendMessage("&aHealed " + target.getName());
            });
        details.addChild(healBtn);
        
        UIButton feedBtn = new UIButton("feed")
            .setText("Feed")
            .setSize(110, 35)
            .setPosition(140, y)
            .setBackground(0xDAA520)
            .onClick(() -> {
                target.setFoodLevel(20);
                admin.sendMessage("&aFed " + target.getName());
            });
        details.addChild(feedBtn);
        
        UIButton flyBtn = new UIButton("fly")
            .setText("Toggle Fly")
            .setSize(110, 35)
            .setPosition(260, y)
            .setBackground(0x4169E1)
            .onClick(() -> {
                target.setFlying(!target.isFlying());
                admin.sendMessage("&aToggled flight for " + target.getName());
            });
        details.addChild(flyBtn);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back")
            .setSize(360, 40)
            .setPosition(20, 380)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        details.addChild(backBtn);
        
        RubidiumUI.showUI(admin, details);
    }
    
    private void addDetailLine(UIContainer container, String label, String value, int y) {
        UIText labelText = new UIText("label_" + label.toLowerCase())
            .setText(label + ":")
            .setFontSize(12)
            .setColor(0x808090)
            .setPosition(20, y);
        container.addChild(labelText);
        
        UIText valueText = new UIText("value_" + label.toLowerCase())
            .setText(value)
            .setFontSize(12)
            .setColor(0xF0F0F5)
            .setPosition(150, y);
        container.addChild(valueText);
    }
    
    private String formatLocation(rubidium.api.world.Location loc) {
        return String.format("%.1f, %.1f, %.1f (%s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}
