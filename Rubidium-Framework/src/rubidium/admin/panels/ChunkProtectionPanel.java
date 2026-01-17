package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.world.Chunk;
import rubidium.api.world.Location;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

public class ChunkProtectionPanel implements AdminPanel {
    
    @Override
    public String getId() {
        return "chunks";
    }
    
    @Override
    public String getName() {
        return "Chunk Protection";
    }
    
    @Override
    public String getDescription() {
        return "Claim and protect chunks from modification";
    }
    
    @Override
    public String getIcon() {
        return "shield";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.chunks";
    }
    
    @Override
    public int getPriority() {
        return 50;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        Location loc = admin.getLocation();
        Chunk currentChunk = loc.getWorld().getChunkAt(loc);
        
        UIContainer panel = new UIContainer("chunk_protection")
            .setTitle("Chunk Protection")
            .setSize(400, 450)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Chunk Protection")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UIContainer infoBox = new UIContainer("current_chunk")
            .setPosition(20, 60)
            .setSize(360, 100)
            .setBackground(0x14141A);
        
        UIText chunkLabel = new UIText("chunk_label")
            .setText("Current Chunk")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(15, 10);
        infoBox.addChild(chunkLabel);
        
        UIText coords = new UIText("coords")
            .setText("X: " + currentChunk.getX() + " | Z: " + currentChunk.getZ())
            .setFontSize(12)
            .setColor(0xF0F0F5)
            .setPosition(15, 35);
        infoBox.addChild(coords);
        
        UIText world = new UIText("world")
            .setText("World: " + loc.getWorld().getName())
            .setFontSize(12)
            .setColor(0x808090)
            .setPosition(15, 55);
        infoBox.addChild(world);
        
        boolean isProtected = currentChunk.isProtected();
        UIText status = new UIText("status")
            .setText("Status: " + (isProtected ? "PROTECTED" : "Unprotected"))
            .setFontSize(12)
            .setColor(isProtected ? 0x32CD32 : 0xFFA500)
            .setPosition(15, 75);
        infoBox.addChild(status);
        
        panel.addChild(infoBox);
        
        int y = 175;
        
        UIButton protectBtn = new UIButton("protect")
            .setText(isProtected ? "Unprotect Chunk" : "Protect Chunk")
            .setSize(360, 40)
            .setPosition(20, y)
            .setBackground(isProtected ? 0x8B0000 : 0x32CD32)
            .onClick(() -> {
                if (isProtected) {
                    currentChunk.setProtected(false);
                    admin.sendMessage("&aChunk unprotected");
                } else {
                    currentChunk.setProtected(true);
                    admin.sendMessage("&aChunk protected");
                }
                RubidiumUI.showUI(admin, createUI(admin));
            });
        panel.addChild(protectBtn);
        y += 50;
        
        UIButton protectRadiusBtn = new UIButton("protect_radius")
            .setText("Protect Radius (3x3)")
            .setSize(175, 40)
            .setPosition(20, y)
            .setBackground(0x4169E1)
            .onClick(() -> {
                protectRadius(admin, currentChunk, 1, true);
                admin.sendMessage("&aProtected 9 chunks around you");
                RubidiumUI.showUI(admin, createUI(admin));
            });
        panel.addChild(protectRadiusBtn);
        
        UIButton unprotectRadiusBtn = new UIButton("unprotect_radius")
            .setText("Unprotect Radius (3x3)")
            .setSize(175, 40)
            .setPosition(205, y)
            .setBackground(0x8B4513)
            .onClick(() -> {
                protectRadius(admin, currentChunk, 1, false);
                admin.sendMessage("&aUnprotected 9 chunks around you");
                RubidiumUI.showUI(admin, createUI(admin));
            });
        panel.addChild(unprotectRadiusBtn);
        y += 50;
        
        UIText flagsLabel = new UIText("flags_label")
            .setText("Protection Flags")
            .setFontSize(14)
            .setColor(0xA0A0AA)
            .setPosition(20, y);
        panel.addChild(flagsLabel);
        y += 25;
        
        addFlagToggle(panel, currentChunk, admin, "Block Break", "block_break", y, 20);
        addFlagToggle(panel, currentChunk, admin, "Block Place", "block_place", y, 195);
        y += 40;
        
        addFlagToggle(panel, currentChunk, admin, "PvP", "pvp", y, 20);
        addFlagToggle(panel, currentChunk, admin, "Mob Damage", "mob_damage", y, 195);
        y += 40;
        
        addFlagToggle(panel, currentChunk, admin, "Explosions", "explosions", y, 20);
        addFlagToggle(panel, currentChunk, admin, "Fire Spread", "fire_spread", y, 195);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(360, 40)
            .setPosition(20, 385)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private void addFlagToggle(UIContainer panel, Chunk chunk, Player admin, 
            String label, String flag, int y, int x) {
        boolean enabled = chunk.getFlag(flag);
        
        UIButton toggle = new UIButton("flag_" + flag)
            .setText(label + ": " + (enabled ? "ON" : "OFF"))
            .setSize(165, 35)
            .setPosition(x, y)
            .setBackground(enabled ? 0x32CD32 : 0x505060)
            .onClick(() -> {
                chunk.setFlag(flag, !enabled);
                admin.sendMessage("&a" + label + " " + (!enabled ? "enabled" : "disabled"));
                RubidiumUI.showUI(admin, createUI(admin));
            });
        panel.addChild(toggle);
    }
    
    private void protectRadius(Player admin, Chunk center, int radius, boolean protect) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = center.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz);
                chunk.setProtected(protect);
            }
        }
    }
}
