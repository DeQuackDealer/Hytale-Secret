package rubidium.settings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SettingsRegistry {
    
    private static final SettingsRegistry INSTANCE = new SettingsRegistry();
    
    private final Map<String, SettingCategory> categories = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
    private final ServerSettings serverSettings = new ServerSettings();
    
    public static SettingsRegistry get() {
        return INSTANCE;
    }
    
    public void registerCategory(SettingCategory category) {
        categories.put(category.getId(), category);
    }
    
    public Optional<SettingCategory> getCategory(String id) {
        return Optional.ofNullable(categories.get(id));
    }
    
    public Collection<SettingCategory> getAllCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }
    
    public PlayerSettings getPlayerSettings(UUID playerId) {
        return playerSettings.computeIfAbsent(playerId, PlayerSettings::new);
    }
    
    public ServerSettings getServerSettings() {
        return serverSettings;
    }
    
    public void savePlayerSettings(UUID playerId) {
        PlayerSettings settings = playerSettings.get(playerId);
        if (settings != null) {
            settings.save();
        }
    }
    
    public void loadPlayerSettings(UUID playerId) {
        PlayerSettings settings = getPlayerSettings(playerId);
        settings.load();
    }
    
    public static class SettingCategory {
        private final String id;
        private final String name;
        private final String icon;
        private final List<Setting<?>> settings = new ArrayList<>();
        private final PermissionLevel requiredPermission;
        
        public SettingCategory(String id, String name, String icon, PermissionLevel permission) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.requiredPermission = permission;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
        public PermissionLevel getRequiredPermission() { return requiredPermission; }
        
        public SettingCategory addSetting(Setting<?> setting) {
            settings.add(setting);
            return this;
        }
        
        public List<Setting<?>> getSettings() {
            return Collections.unmodifiableList(settings);
        }
    }
    
    public enum PermissionLevel {
        PLAYER,
        ADMIN,
        OWNER
    }
    
    public static abstract class Setting<T> {
        protected final String id;
        protected final String name;
        protected final String description;
        protected final T defaultValue;
        protected final PermissionLevel requiredPermission;
        
        public Setting(String id, String name, String description, T defaultValue, PermissionLevel permission) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.defaultValue = defaultValue;
            this.requiredPermission = permission;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public T getDefaultValue() { return defaultValue; }
        public PermissionLevel getRequiredPermission() { return requiredPermission; }
        
        public abstract SettingType getType();
    }
    
    public enum SettingType {
        BOOLEAN, INTEGER, FLOAT, STRING, ENUM, KEYBIND, COLOR
    }
    
    public static class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String id, String name, String description, boolean defaultValue, PermissionLevel permission) {
            super(id, name, description, defaultValue, permission);
        }
        
        @Override
        public SettingType getType() { return SettingType.BOOLEAN; }
    }
    
    public static class IntegerSetting extends Setting<Integer> {
        private final int min, max;
        
        public IntegerSetting(String id, String name, String description, int defaultValue, int min, int max, PermissionLevel permission) {
            super(id, name, description, defaultValue, permission);
            this.min = min;
            this.max = max;
        }
        
        public int getMin() { return min; }
        public int getMax() { return max; }
        
        @Override
        public SettingType getType() { return SettingType.INTEGER; }
    }
    
    public static class FloatSetting extends Setting<Float> {
        private final float min, max;
        
        public FloatSetting(String id, String name, String description, float defaultValue, float min, float max, PermissionLevel permission) {
            super(id, name, description, defaultValue, permission);
            this.min = min;
            this.max = max;
        }
        
        public float getMin() { return min; }
        public float getMax() { return max; }
        
        @Override
        public SettingType getType() { return SettingType.FLOAT; }
    }
    
    public static class KeybindSetting extends Setting<String> {
        public KeybindSetting(String id, String name, String description, String defaultKey, PermissionLevel permission) {
            super(id, name, description, defaultKey, permission);
        }
        
        @Override
        public SettingType getType() { return SettingType.KEYBIND; }
    }
    
    public static class EnumSetting<E extends Enum<E>> extends Setting<E> {
        private final Class<E> enumClass;
        
        public EnumSetting(String id, String name, String description, E defaultValue, Class<E> enumClass, PermissionLevel permission) {
            super(id, name, description, defaultValue, permission);
            this.enumClass = enumClass;
        }
        
        public Class<E> getEnumClass() { return enumClass; }
        public E[] getValues() { return enumClass.getEnumConstants(); }
        
        @Override
        public SettingType getType() { return SettingType.ENUM; }
    }
}
