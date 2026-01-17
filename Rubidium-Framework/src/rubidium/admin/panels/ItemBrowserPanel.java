package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.admin.AdminUIModule;
import rubidium.api.player.Player;
import rubidium.api.item.ItemRegistry;
import rubidium.api.item.ItemType;
import rubidium.inventory.ItemStack;
import rubidium.ui.components.*;

import java.util.*;

public class ItemBrowserPanel implements AdminPanel {
    
    @Override
    public String getId() { return "items"; }
    
    @Override
    public String getName() { return "Item Browser"; }
    
    @Override
    public String getDescription() { return "Search and give items with property details"; }
    
    @Override
    public String getIcon() { return "chest"; }
    
    @Override
    public String getPermission() { return "rubidium.admin.items"; }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("item_browser")
            .setTitle("Item Browser")
            .setSize(500, 450)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        panel.addChild(new UIText("title").setText("Item Browser").setFontSize(20).setColor(0x8A2BE2).setPosition(20, 20));
        
        UITextField searchField = new UITextField("search")
            .setPlaceholder("Search items...")
            .setSize(460, 35)
            .setPosition(20, 55);
        panel.addChild(searchField);
        
        UIContainer itemList = new UIContainer("items")
            .setPosition(20, 100)
            .setSize(460, 280)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        Collection<ItemType> items = ItemRegistry.getAll();
        int y = 10;
        for (ItemType item : items) {
            UIButton itemBtn = new UIButton("item_" + item.getId())
                .setText(item.getName())
                .setSize(440, 35)
                .setPosition(10, y)
                .setBackground(0x2D2D35)
                .onClick(() -> {
                    ItemStack stack = new ItemStack(item.getId());
                    admin.getInventory().addItem(stack);
                    admin.sendMessage("&aGave 1x " + item.getName());
                });
            itemList.addChild(itemBtn);
            y += 40;
        }
        
        if (items.isEmpty()) {
            itemList.addChild(new UIText("no_items").setText("No items registered").setFontSize(14).setColor(0x808090).setPosition(150, 130));
        }
        
        panel.addChild(itemList);
        panel.addChild(new UIButton("back").setText("Back").setSize(460, 40).setPosition(20, 390).setBackground(0x505060).onClick(() -> AdminUIModule.getInstance().openMainMenu(admin)));
        
        return panel;
    }
}
