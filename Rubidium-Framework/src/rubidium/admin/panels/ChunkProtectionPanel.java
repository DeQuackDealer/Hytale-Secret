package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.admin.AdminUIModule;
import rubidium.api.player.Player;
import rubidium.ui.components.*;

public class ChunkProtectionPanel implements AdminPanel {
    
    @Override
    public String getId() { return "chunks"; }
    
    @Override
    public String getName() { return "Chunk Protection"; }
    
    @Override
    public String getDescription() { return "Manage protected chunks and regions"; }
    
    @Override
    public String getIcon() { return "shield"; }
    
    @Override
    public String getPermission() { return "rubidium.admin.chunks"; }
    
    @Override
    public UIContainer createUI(Player player) {
        UIContainer panel = new UIContainer("chunk_protection")
            .setTitle("Chunk Protection")
            .setSize(400, 300)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        panel.addChild(new UIText("title").setText("Chunk Protection").setFontSize(20).setColor(0x8A2BE2).setPosition(20, 20));
        panel.addChild(new UIText("info").setText("Chunk protection features - Coming soon").setFontSize(14).setColor(0x808090).setPosition(20, 60));
        panel.addChild(new UIButton("back").setText("Back").setSize(360, 40).setPosition(20, 240).setBackground(0x505060).onClick(() -> AdminUIModule.getInstance().openMainMenu(player)));
        
        return panel;
    }
}
