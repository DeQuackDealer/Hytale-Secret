package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.item.ItemStack;
import rubidium.api.item.ItemRegistry;
import rubidium.api.item.ItemType;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

import java.util.*;

public class ItemBrowserPanel implements AdminPanel {
    
    private static final int ITEMS_PER_PAGE = 20;
    
    @Override
    public String getId() {
        return "items";
    }
    
    @Override
    public String getName() {
        return "Item Browser";
    }
    
    @Override
    public String getDescription() {
        return "Search and give items with property details";
    }
    
    @Override
    public String getIcon() {
        return "chest";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.items";
    }
    
    @Override
    public int getPriority() {
        return 40;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        return createUI(admin, "", 0);
    }
    
    public UIContainer createUI(Player admin, String searchQuery, int page) {
        UIContainer panel = new UIContainer("item_browser")
            .setTitle("Item Browser")
            .setSize(500, 600)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Item Browser")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UITextField searchField = new UITextField("search")
            .setPlaceholder("Search items...")
            .setValue(searchQuery)
            .setSize(360, 40)
            .setPosition(20, 55);
        panel.addChild(searchField);
        
        UIButton searchBtn = new UIButton("search_btn")
            .setText("Search")
            .setSize(90, 40)
            .setPosition(390, 55)
            .setBackground(0x4169E1)
            .onClick(() -> {
                String query = searchField.getValue();
                RubidiumUI.showUI(admin, createUI(admin, query, 0));
            });
        panel.addChild(searchBtn);
        
        UIContainer itemList = new UIContainer("item_list")
            .setPosition(20, 110)
            .setSize(460, 400)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        List<ItemType> items = searchItems(searchQuery);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());
        
        int y = 10;
        for (int i = startIndex; i < endIndex; i++) {
            ItemType itemType = items.get(i);
            UIContainer row = createItemRow(admin, itemType, y, searchQuery, page);
            itemList.addChild(row);
            y += 50;
        }
        
        if (items.isEmpty()) {
            UIText noItems = new UIText("no_items")
                .setText("No items found")
                .setColor(0x808080)
                .setPosition(180, 180);
            itemList.addChild(noItems);
        }
        
        panel.addChild(itemList);
        
        int totalPages = (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE);
        
        UIButton prevBtn = new UIButton("prev")
            .setText("<")
            .setSize(50, 35)
            .setPosition(20, 520)
            .setBackground(page > 0 ? 0x4169E1 : 0x505060)
            .onClick(() -> {
                if (page > 0) {
                    RubidiumUI.showUI(admin, createUI(admin, searchQuery, page - 1));
                }
            });
        panel.addChild(prevBtn);
        
        UIText pageText = new UIText("page")
            .setText("Page " + (page + 1) + " / " + Math.max(1, totalPages))
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(200, 530);
        panel.addChild(pageText);
        
        UIButton nextBtn = new UIButton("next")
            .setText(">")
            .setSize(50, 35)
            .setPosition(430, 520)
            .setBackground(page < totalPages - 1 ? 0x4169E1 : 0x505060)
            .onClick(() -> {
                if (page < totalPages - 1) {
                    RubidiumUI.showUI(admin, createUI(admin, searchQuery, page + 1));
                }
            });
        panel.addChild(nextBtn);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(200, 35)
            .setPosition(150, 560)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private List<ItemType> searchItems(String query) {
        List<ItemType> allItems = ItemRegistry.getAllItems();
        
        if (query == null || query.isEmpty()) {
            return allItems;
        }
        
        String lowerQuery = query.toLowerCase();
        List<ItemType> results = new ArrayList<>();
        
        for (ItemType item : allItems) {
            if (item.getId().toLowerCase().contains(lowerQuery) ||
                item.getDisplayName().toLowerCase().contains(lowerQuery)) {
                results.add(item);
            }
        }
        
        return results;
    }
    
    private UIContainer createItemRow(Player admin, ItemType itemType, int y, String searchQuery, int page) {
        UIContainer row = new UIContainer("item_" + itemType.getId().hashCode())
            .setPosition(10, y)
            .setSize(440, 45)
            .setBackground(0x2D2D35);
        
        UIText nameText = new UIText("name")
            .setText(itemType.getDisplayName())
            .setFontSize(13)
            .setColor(0xF0F0F5)
            .setPosition(10, 6);
        row.addChild(nameText);
        
        UIText idText = new UIText("id")
            .setText(itemType.getId())
            .setFontSize(10)
            .setColor(0x808090)
            .setPosition(10, 25);
        row.addChild(idText);
        
        UIButton infoBtn = new UIButton("info")
            .setText("Info")
            .setSize(50, 30)
            .setPosition(280, 7)
            .setBackground(0x505060)
            .onClick(() -> openItemInfo(admin, itemType, searchQuery, page));
        row.addChild(infoBtn);
        
        UIButton giveBtn = new UIButton("give")
            .setText("Give")
            .setSize(50, 30)
            .setPosition(340, 7)
            .setBackground(0x32CD32)
            .onClick(() -> {
                ItemStack stack = ItemStack.of(itemType, 1);
                admin.getInventory().addItem(stack);
                admin.sendMessage("&aGiven 1x " + itemType.getDisplayName());
            });
        row.addChild(giveBtn);
        
        UIButton stackBtn = new UIButton("stack")
            .setText("64")
            .setSize(40, 30)
            .setPosition(395, 7)
            .setBackground(0x4169E1)
            .onClick(() -> {
                ItemStack stack = ItemStack.of(itemType, 64);
                admin.getInventory().addItem(stack);
                admin.sendMessage("&aGiven 64x " + itemType.getDisplayName());
            });
        row.addChild(stackBtn);
        
        return row;
    }
    
    private void openItemInfo(Player admin, ItemType itemType, String searchQuery, int page) {
        UIContainer info = new UIContainer("item_info")
            .setTitle("Item Details")
            .setSize(400, 400)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText(itemType.getDisplayName())
            .setFontSize(18)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        info.addChild(title);
        
        int y = 55;
        int lineHeight = 22;
        
        addInfoLine(info, "ID", itemType.getId(), y); y += lineHeight;
        addInfoLine(info, "Category", itemType.getCategory(), y); y += lineHeight;
        addInfoLine(info, "Max Stack", String.valueOf(itemType.getMaxStackSize()), y); y += lineHeight;
        addInfoLine(info, "Rarity", itemType.getRarity().toString(), y); y += lineHeight;
        
        if (itemType.isEquippable()) {
            addInfoLine(info, "Equip Slot", itemType.getEquipSlot(), y); y += lineHeight;
        }
        
        if (itemType.getDurability() > 0) {
            addInfoLine(info, "Durability", String.valueOf(itemType.getDurability()), y); y += lineHeight;
        }
        
        if (itemType.getDamage() > 0) {
            addInfoLine(info, "Damage", String.valueOf(itemType.getDamage()), y); y += lineHeight;
        }
        
        if (itemType.getArmor() > 0) {
            addInfoLine(info, "Armor", String.valueOf(itemType.getArmor()), y); y += lineHeight;
        }
        
        y += 20;
        
        UITextField amountField = new UITextField("amount")
            .setPlaceholder("Amount")
            .setValue("1")
            .setSize(100, 35)
            .setPosition(20, y);
        info.addChild(amountField);
        
        UIButton giveCustomBtn = new UIButton("give_custom")
            .setText("Give")
            .setSize(120, 35)
            .setPosition(130, y)
            .setBackground(0x32CD32)
            .onClick(() -> {
                try {
                    int amount = Integer.parseInt(amountField.getValue());
                    amount = Math.max(1, Math.min(amount, 1000));
                    ItemStack stack = ItemStack.of(itemType, amount);
                    admin.getInventory().addItem(stack);
                    admin.sendMessage("&aGiven " + amount + "x " + itemType.getDisplayName());
                } catch (NumberFormatException e) {
                    admin.sendMessage("&cInvalid amount");
                }
            });
        info.addChild(giveCustomBtn);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back")
            .setSize(360, 40)
            .setPosition(20, 340)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin, searchQuery, page)));
        info.addChild(backBtn);
        
        RubidiumUI.showUI(admin, info);
    }
    
    private void addInfoLine(UIContainer container, String label, String value, int y) {
        UIText labelText = new UIText("label_" + label.toLowerCase())
            .setText(label + ":")
            .setFontSize(11)
            .setColor(0x808090)
            .setPosition(20, y);
        container.addChild(labelText);
        
        UIText valueText = new UIText("value_" + label.toLowerCase())
            .setText(value)
            .setFontSize(11)
            .setColor(0xF0F0F5)
            .setPosition(150, y);
        container.addChild(valueText);
    }
}
