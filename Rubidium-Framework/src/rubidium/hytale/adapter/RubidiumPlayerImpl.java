package rubidium.hytale.adapter;

import rubidium.api.player.Player;
import rubidium.api.player.PlayerData;
import rubidium.api.player.PlayerInventory;
import rubidium.core.HytaleRuntimeBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RubidiumPlayerImpl implements Player {
    
    private final HytalePlayerImpl hytalePlayer;
    private String displayName;
    private boolean op = false;
    private final long firstPlayed;
    private long lastPlayed;
    private final PlayerDataImpl playerData = new PlayerDataImpl();
    private final PlayerInventoryImpl inventory = new PlayerInventoryImpl();
    
    public RubidiumPlayerImpl(HytalePlayerImpl hytalePlayer) {
        this.hytalePlayer = hytalePlayer;
        this.displayName = hytalePlayer.getName();
        this.firstPlayed = System.currentTimeMillis();
        this.lastPlayed = System.currentTimeMillis();
    }
    
    @Override
    public UUID getUUID() {
        return hytalePlayer.getUUID();
    }
    
    @Override
    public String getName() {
        return hytalePlayer.getName();
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public boolean isOnline() {
        return hytalePlayer.isOnline();
    }
    
    @Override
    public void kick(String reason) {
        hytalePlayer.kick(reason);
    }
    
    @Override
    public void teleport(double x, double y, double z) {
        hytalePlayer.teleport(x, y, z);
    }
    
    @Override
    public void teleport(double x, double y, double z, float yaw, float pitch) {
        hytalePlayer.teleport(x, y, z);
    }
    
    @Override
    public void teleport(Location location) {
        teleport(location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }
    
    @Override
    public Location getLocation() {
        return new Location(hytalePlayer.getX(), hytalePlayer.getY(), hytalePlayer.getZ(), 0, 0);
    }
    
    @Override
    public String getWorld() {
        return hytalePlayer.getWorld();
    }
    
    @Override
    public int getPing() {
        com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer sp = getServerPlayer();
        return sp != null ? sp.getPing() : 0;
    }
    
    @Override
    public double getHealth() {
        return HytaleRuntimeBridge.get().getPlayerHealth(getUUID());
    }
    
    @Override
    public void setHealth(double health) {
        HytaleRuntimeBridge.get().setPlayerHealth(getUUID(), (float) health);
    }
    
    @Override
    public double getMaxHealth() {
        com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer sp = getServerPlayer();
        return sp != null ? sp.getMaxHealth() : 20.0;
    }
    
    @Override
    public String getAddress() {
        return "127.0.0.1";
    }
    
    @Override
    public long getFirstPlayed() {
        return firstPlayed;
    }
    
    @Override
    public long getLastPlayed() {
        return lastPlayed;
    }
    
    @Override
    public boolean hasPlayedBefore() {
        return false;
    }
    
    @Override
    public void setOp(boolean op) {
        this.op = op;
    }
    
    @Override
    public boolean isOp() {
        return op;
    }
    
    @Override
    public void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer sp = getServerPlayer();
        if (sp != null) {
            sp.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }
    
    @Override
    public void showActionBar(String message) {
        com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer sp = getServerPlayer();
        if (sp != null) {
            sp.sendActionBar(message);
        }
    }
    
    @Override
    public void playSound(String sound, float volume, float pitch) {
        com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer sp = getServerPlayer();
        if (sp != null) {
            sp.playSound(sound, volume, pitch);
        }
    }
    
    @Override
    public PlayerInventory getInventory() {
        return inventory;
    }
    
    @Override
    public PlayerData getData() {
        return playerData;
    }
    
    @Override
    public void sendPacket(Object packet) {
        com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer sp = getServerPlayer();
        if (sp != null) {
            sp.sendPacket(packet);
        }
    }
    
    private com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer getServerPlayer() {
        return hytalePlayer != null ? hytalePlayer.getServerPlayer() : null;
    }
    
    @Override
    public void sendMessage(String message) {
        hytalePlayer.sendMessage(message);
    }
    
    @Override
    public void sendMessage(String... messages) {
        for (String message : messages) {
            hytalePlayer.sendMessage(message);
        }
    }
    
    @Override
    public boolean hasPermission(String permission) {
        return hytalePlayer.hasPermission(permission) || op;
    }
    
    @Override
    public boolean isPlayer() {
        return true;
    }
    
    @Override
    public boolean isConsole() {
        return false;
    }
    
    public HytalePlayerImpl getHytalePlayer() {
        return hytalePlayer;
    }
    
    private static class PlayerDataImpl implements PlayerData {
        private final Map<String, Object> data = new HashMap<>();
        
        @Override
        public void set(String key, Object value) {
            data.put(key, value);
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key, Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        }
        
        @Override
        public String getString(String key) {
            return getString(key, null);
        }
        
        @Override
        public String getString(String key, String defaultValue) {
            Object value = data.get(key);
            return value instanceof String ? (String) value : defaultValue;
        }
        
        @Override
        public int getInt(String key) {
            return getInt(key, 0);
        }
        
        @Override
        public int getInt(String key, int defaultValue) {
            Object value = data.get(key);
            return value instanceof Number ? ((Number) value).intValue() : defaultValue;
        }
        
        @Override
        public long getLong(String key) {
            return getLong(key, 0L);
        }
        
        @Override
        public long getLong(String key, long defaultValue) {
            Object value = data.get(key);
            return value instanceof Number ? ((Number) value).longValue() : defaultValue;
        }
        
        @Override
        public double getDouble(String key) {
            return getDouble(key, 0.0);
        }
        
        @Override
        public double getDouble(String key, double defaultValue) {
            Object value = data.get(key);
            return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
        }
        
        @Override
        public boolean getBoolean(String key) {
            return getBoolean(key, false);
        }
        
        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            Object value = data.get(key);
            return value instanceof Boolean ? (Boolean) value : defaultValue;
        }
        
        @Override
        public boolean has(String key) {
            return data.containsKey(key);
        }
        
        @Override
        public void remove(String key) {
            data.remove(key);
        }
        
        @Override
        public void save() {
        }
        
        @Override
        public void reload() {
        }
    }
    
    private static class PlayerInventoryImpl implements PlayerInventory {
        private final Object[] items = new Object[36];
        private Object mainHand;
        private Object offHand;
        
        @Override
        public Object getItem(int slot) {
            return slot >= 0 && slot < items.length ? items[slot] : null;
        }
        
        @Override
        public void setItem(int slot, Object item) {
            if (slot >= 0 && slot < items.length) {
                items[slot] = item;
            }
        }
        
        @Override
        public Object getItemInMainHand() {
            return mainHand;
        }
        
        @Override
        public void setItemInMainHand(Object item) {
            this.mainHand = item;
        }
        
        @Override
        public Object getItemInOffHand() {
            return offHand;
        }
        
        @Override
        public void setItemInOffHand(Object item) {
            this.offHand = item;
        }
        
        @Override
        public Object[] getContents() {
            return items.clone();
        }
        
        @Override
        public void setContents(Object[] items) {
            System.arraycopy(items, 0, this.items, 0, Math.min(items.length, this.items.length));
        }
        
        @Override
        public int getSize() {
            return items.length;
        }
        
        @Override
        public void clear() {
            java.util.Arrays.fill(items, null);
            mainHand = null;
            offHand = null;
        }
        
        @Override
        public void clear(int slot) {
            if (slot >= 0 && slot < items.length) {
                items[slot] = null;
            }
        }
        
        @Override
        public int firstEmpty() {
            for (int i = 0; i < items.length; i++) {
                if (items[i] == null) return i;
            }
            return -1;
        }
        
        @Override
        public boolean contains(Object item) {
            for (Object i : items) {
                if (i != null && i.equals(item)) return true;
            }
            return false;
        }
        
        @Override
        public void addItem(Object... itemsToAdd) {
            for (Object item : itemsToAdd) {
                int slot = firstEmpty();
                if (slot >= 0) {
                    items[slot] = item;
                }
            }
        }
        
        @Override
        public void removeItem(Object... itemsToRemove) {
            for (Object item : itemsToRemove) {
                for (int i = 0; i < items.length; i++) {
                    if (items[i] != null && items[i].equals(item)) {
                        items[i] = null;
                        break;
                    }
                }
            }
        }
    }
}
