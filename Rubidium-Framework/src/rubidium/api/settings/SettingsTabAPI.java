package rubidium.api.settings;

import rubidium.settings.SettingsRegistry;
import rubidium.settings.SettingsRegistry.SettingCategory;
import rubidium.settings.SettingsRegistry.Setting;
import rubidium.settings.SettingsRegistry.BooleanSetting;
import rubidium.settings.SettingsRegistry.IntegerSetting;
import rubidium.settings.SettingsRegistry.FloatSetting;
import rubidium.settings.SettingsRegistry.KeybindSetting;
import rubidium.settings.SettingsRegistry.EnumSetting;
import rubidium.settings.SettingsRegistry.PermissionLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class SettingsTabAPI {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-SettingsTabAPI");
    private static final Map<String, SettingsTab> tabs = new ConcurrentHashMap<>();
    private static final List<Consumer<SettingsTab>> tabListeners = new ArrayList<>();
    private static boolean initialized = false;
    
    private SettingsTabAPI() {}
    
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("[SettingsTabAPI] Settings Tab API initialized");
        
        registerRubidiumDefaultTabs();
    }
    
    private static void registerRubidiumDefaultTabs() {
        register(SettingsTab.builder("rubidium")
            .name("Rubidium")
            .icon("rubidium_logo")
            .description("Configure Rubidium framework settings")
            .permission(PermissionLevel.PLAYER)
            .category(CategoryBuilder.create("general")
                .name("General")
                .setting(new BooleanSetting("enabled", "Enable Rubidium", "Enable or disable Rubidium features", true, PermissionLevel.PLAYER))
                .setting(new BooleanSetting("notifications", "Show Notifications", "Display Rubidium notifications", true, PermissionLevel.PLAYER))
                .build())
            .category(CategoryBuilder.create("display")
                .name("Display")
                .setting(new BooleanSetting("minimap_enabled", "Enable Minimap", "Show the minimap on screen", true, PermissionLevel.PLAYER))
                .setting(new IntegerSetting("minimap_size", "Minimap Size", "Size of the minimap in pixels", 150, 50, 300, PermissionLevel.PLAYER))
                .setting(new FloatSetting("minimap_zoom", "Minimap Zoom", "Zoom level of the minimap", 1.0f, 0.5f, 3.0f, PermissionLevel.PLAYER))
                .setting(new BooleanSetting("minimap_rotate", "Rotate Minimap", "Rotate minimap with player view", true, PermissionLevel.PLAYER))
                .setting(new BooleanSetting("waypoints_enabled", "Show Waypoints", "Display waypoints on minimap", true, PermissionLevel.PLAYER))
                .build())
            .category(CategoryBuilder.create("performance")
                .name("Performance")
                .setting(new BooleanSetting("stats_enabled", "Show Statistics", "Display FPS, TPS and memory stats", false, PermissionLevel.PLAYER))
                .setting(new BooleanSetting("optimizations", "Enable Optimizations", "Enable performance optimizations", true, PermissionLevel.ADMIN))
                .build())
            .category(CategoryBuilder.create("voice")
                .name("Voice Chat")
                .setting(new BooleanSetting("voice_enabled", "Enable Voice Chat", "Enable proximity voice chat", true, PermissionLevel.PLAYER))
                .setting(new FloatSetting("voice_volume", "Voice Volume", "Volume of voice chat", 1.0f, 0.0f, 2.0f, PermissionLevel.PLAYER))
                .setting(new KeybindSetting("ptt_key", "Push-to-Talk Key", "Key to hold for push-to-talk", "V", PermissionLevel.PLAYER))
                .setting(new BooleanSetting("ptt_mode", "Push-to-Talk Mode", "Use push-to-talk instead of voice activation", true, PermissionLevel.PLAYER))
                .build())
            .build());
        
        LOGGER.info("[SettingsTabAPI] Registered Rubidium default settings tab");
    }
    
    public static SettingsTab register(SettingsTab tab) {
        tabs.put(tab.getId(), tab);
        
        for (SettingCategory category : tab.getCategories()) {
            SettingsRegistry.get().registerCategory(category);
        }
        
        for (Consumer<SettingsTab> listener : tabListeners) {
            listener.accept(tab);
        }
        
        LOGGER.info("[SettingsTabAPI] Registered settings tab: " + tab.getName() + " (id=" + tab.getId() + ")");
        return tab;
    }
    
    public static SettingsTab register(SettingsTab.Builder builder) {
        return register(builder.build());
    }
    
    public static Optional<SettingsTab> get(String id) {
        return Optional.ofNullable(tabs.get(id));
    }
    
    public static Collection<SettingsTab> all() {
        return Collections.unmodifiableCollection(tabs.values());
    }
    
    public static void unregister(String id) {
        tabs.remove(id);
        LOGGER.info("[SettingsTabAPI] Unregistered settings tab: " + id);
    }
    
    public static void onTabRegistered(Consumer<SettingsTab> listener) {
        tabListeners.add(listener);
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static class SettingsTab {
        private final String id;
        private final String name;
        private final String icon;
        private final String description;
        private final PermissionLevel permission;
        private final List<SettingCategory> categories;
        private final int order;
        
        private SettingsTab(Builder builder) {
            this.id = builder.id;
            this.name = builder.name;
            this.icon = builder.icon;
            this.description = builder.description;
            this.permission = builder.permission;
            this.categories = List.copyOf(builder.categories);
            this.order = builder.order;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public PermissionLevel getPermission() { return permission; }
        public List<SettingCategory> getCategories() { return categories; }
        public int getOrder() { return order; }
        
        public static Builder builder(String id) {
            return new Builder(id);
        }
        
        public static class Builder {
            private final String id;
            private String name;
            private String icon = "settings";
            private String description = "";
            private PermissionLevel permission = PermissionLevel.PLAYER;
            private List<SettingCategory> categories = new ArrayList<>();
            private int order = 100;
            
            public Builder(String id) {
                this.id = id;
                this.name = id;
            }
            
            public Builder name(String name) { this.name = name; return this; }
            public Builder icon(String icon) { this.icon = icon; return this; }
            public Builder description(String desc) { this.description = desc; return this; }
            public Builder permission(PermissionLevel perm) { this.permission = perm; return this; }
            public Builder order(int order) { this.order = order; return this; }
            
            public Builder category(SettingCategory category) {
                this.categories.add(category);
                return this;
            }
            
            public SettingsTab build() {
                return new SettingsTab(this);
            }
        }
    }
    
    public static class CategoryBuilder {
        private final String id;
        private String name;
        private String icon = "folder";
        private PermissionLevel permission = PermissionLevel.PLAYER;
        private List<Setting<?>> settings = new ArrayList<>();
        
        private CategoryBuilder(String id) {
            this.id = id;
            this.name = id;
        }
        
        public static CategoryBuilder create(String id) {
            return new CategoryBuilder(id);
        }
        
        public CategoryBuilder name(String name) { this.name = name; return this; }
        public CategoryBuilder icon(String icon) { this.icon = icon; return this; }
        public CategoryBuilder permission(PermissionLevel perm) { this.permission = perm; return this; }
        
        public CategoryBuilder setting(Setting<?> setting) {
            this.settings.add(setting);
            return this;
        }
        
        public SettingCategory build() {
            SettingCategory category = new SettingCategory(id, name, icon, permission);
            for (Setting<?> setting : settings) {
                category.addSetting(setting);
            }
            return category;
        }
    }
}
