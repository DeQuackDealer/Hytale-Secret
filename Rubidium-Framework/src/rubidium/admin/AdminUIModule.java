package rubidium.admin;

import rubidium.api.RubidiumModule;
import rubidium.api.command.Command;
import rubidium.api.command.CommandContext;
import rubidium.api.event.EventHandler;
import rubidium.api.event.player.PlayerInteractEvent;
import rubidium.api.player.Player;
import rubidium.inventory.ItemStack;
import rubidium.ui.components.*;
import rubidium.admin.panels.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AdminUIModule implements RubidiumModule {
    
    private static AdminUIModule instance;
    private final Map<UUID, AdminConfig> playerConfigs = new ConcurrentHashMap<>();
    private final Map<String, AdminPanel> registeredPanels = new ConcurrentHashMap<>();
    private final Set<UUID> admins = ConcurrentHashMap.newKeySet();
    
    public static final String ADMIN_STICK_ID = "rubidium:admin_stick";
    
    @Override
    public String getId() {
        return "rubidium-admin-ui";
    }
    
    @Override
    public String getName() {
        return "Rubidium AdminUI";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public void onEnable() {
        instance = this;
        registerDefaultPanels();
        log("AdminUI module enabled - GUI-based server administration ready");
    }
    
    @Override
    public void onDisable() {
        playerConfigs.clear();
        registeredPanels.clear();
        admins.clear();
        instance = null;
    }
    
    public static AdminUIModule getInstance() {
        return instance;
    }
    
    private void registerDefaultPanels() {
        registerPanel(new PlayerManagementPanel());
        registerPanel(new WorldSettingsPanel());
        registerPanel(new PermissionsPanel());
        registerPanel(new ServerControlPanel());
        registerPanel(new ChunkProtectionPanel());
        registerPanel(new ItemBrowserPanel());
        registerPanel(new TeleportPanel());
        registerPanel(new BanManagementPanel());
    }
    
    public void registerPanel(AdminPanel panel) {
        registeredPanels.put(panel.getId(), panel);
        log("Registered admin panel: " + panel.getName());
    }
    
    public void unregisterPanel(String panelId) {
        registeredPanels.remove(panelId);
    }
    
    public Collection<AdminPanel> getPanels() {
        return Collections.unmodifiableCollection(registeredPanels.values());
    }
    
    public Optional<AdminPanel> getPanel(String id) {
        return Optional.ofNullable(registeredPanels.get(id));
    }
    
    public boolean isAdmin(Player player) {
        return admins.contains(player.getUniqueId()) || player.hasPermission("rubidium.admin");
    }
    
    public void setAdmin(UUID playerId, boolean admin) {
        if (admin) {
            admins.add(playerId);
        } else {
            admins.remove(playerId);
        }
    }
    
    public AdminConfig getConfig(Player player) {
        return playerConfigs.computeIfAbsent(player.getUniqueId(), k -> new AdminConfig());
    }
    
    @Command(name = "admin", description = "Opens the admin UI panel", permission = "rubidium.admin")
    public void adminCommand(CommandContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            ctx.sendError("This command can only be used by players");
            return;
        }
        
        if (!isAdmin(player)) {
            ctx.sendError("You do not have permission to access the admin panel");
            return;
        }
        
        String[] args = ctx.getArgs();
        if (args.length > 0) {
            String panelId = args[0].toLowerCase();
            getPanel(panelId).ifPresentOrElse(
                panel -> openPanel(player, panel),
                () -> ctx.sendError("Unknown panel: " + panelId)
            );
        } else {
            openMainMenu(player);
        }
    }
    
    @Command(name = "adminstick", description = "Gives you an admin stick", permission = "rubidium.admin")
    public void adminStickCommand(CommandContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            ctx.sendError("This command can only be used by players");
            return;
        }
        
        if (!isAdmin(player)) {
            ctx.sendError("You do not have permission to use the admin stick");
            return;
        }
        
        giveAdminStick(player);
        ctx.sendSuccess("You have been given an Admin Stick!");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        if (item != null && ADMIN_STICK_ID.equals(item.getType()) && isAdmin(player)) {
            event.setCancelled(true);
            
            AdminConfig config = getConfig(player);
            String action = config.getStickAction(event.getAction());
            
            if (action != null) {
                getPanel(action).ifPresent(panel -> openPanel(player, panel));
            } else {
                openMainMenu(player);
            }
        }
    }
    
    public void openMainMenu(Player player) {
        UIContainer menu = new UIContainer("admin_main_menu")
            .setTitle("Admin Panel")
            .setSize(400, 500)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("admin_title")
            .setText("Rubidium Admin Panel")
            .setFontSize(24)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        menu.addChild(title);
        
        UIText subtitle = new UIText("admin_subtitle")
            .setText("Server Administration Interface")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 50);
        menu.addChild(subtitle);
        
        int y = 90;
        int buttonHeight = 45;
        int spacing = 10;
        
        for (AdminPanel panel : registeredPanels.values()) {
            UIButton panelButton = new UIButton("panel_" + panel.getId())
                .setText(panel.getName())
                .setIcon(panel.getIcon())
                .setSize(360, buttonHeight)
                .setPosition(20, y)
                .setBackground(0x2D2D35)
                .setHoverBackground(0x3D3D48)
                .setTextColor(0xF0F0F5)
                .onClick(() -> openPanel(player, panel));
            
            menu.addChild(panelButton);
            y += buttonHeight + spacing;
        }
        
        UIButton closeButton = new UIButton("close_btn")
            .setText("Close")
            .setSize(360, 40)
            .setPosition(20, y + 10)
            .setBackground(0x8B0000)
            .setHoverBackground(0xA52A2A)
            .onClick(() -> closeUI(player));
        menu.addChild(closeButton);
        
        showUI(player, menu);
    }
    
    public void openPanel(Player player, AdminPanel panel) {
        UIContainer ui = panel.createUI(player);
        if (ui != null) {
            showUI(player, ui);
        }
    }
    
    public void giveAdminStick(Player player) {
        ItemStack stick = ItemStack.builder(ADMIN_STICK_ID)
            .displayName("&6Admin Stick")
            .lore(
                "&7Right-click: Open Admin Menu",
                "&7Left-click: Quick Action",
                "&7Shift+Right: Configure Shortcuts"
            )
            .build();
        
        player.getInventory().addItem(stick);
    }
    
    private void showUI(Player player, UIContainer ui) {
        player.sendPacket(ui);
    }
    
    private void closeUI(Player player) {
        player.sendPacket("CLOSE_UI");
    }
    
    @Override
    public void log(String message) {
        System.out.println("[AdminUI] " + message);
    }
    
    public static class AdminConfig {
        private final Map<String, String> stickActions = new HashMap<>();
        private final Set<String> favoritesPanels = new HashSet<>();
        private boolean compactMode = false;
        
        public AdminConfig() {
            stickActions.put("RIGHT_CLICK", null);
            stickActions.put("LEFT_CLICK", "players");
            stickActions.put("SHIFT_RIGHT_CLICK", "settings");
        }
        
        public String getStickAction(String actionType) {
            return stickActions.get(actionType);
        }
        
        public void setStickAction(String actionType, String panelId) {
            stickActions.put(actionType, panelId);
        }
        
        public Set<String> getFavorites() {
            return Collections.unmodifiableSet(favoritesPanels);
        }
        
        public void addFavorite(String panelId) {
            favoritesPanels.add(panelId);
        }
        
        public void removeFavorite(String panelId) {
            favoritesPanels.remove(panelId);
        }
        
        public boolean isCompactMode() {
            return compactMode;
        }
        
        public void setCompactMode(boolean compact) {
            this.compactMode = compact;
        }
    }
}
