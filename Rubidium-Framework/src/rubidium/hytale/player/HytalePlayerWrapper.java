package rubidium.hytale.player;

import rubidium.api.player.*;
import rubidium.hytale.adapter.HytaleAdapter;
import rubidium.hytale.packet.HytalePacketAdapter;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class HytalePlayerWrapper implements Player {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytalePlayer");
    
    private final Object hytalePlayer;
    private final HytaleAdapter adapter;
    private final Map<String, Method> methodCache;
    
    private String displayName;
    private final HytalePlayerInventory inventory;
    private final HytalePlayerData playerData;
    
    public HytalePlayerWrapper(Object hytalePlayer, HytaleAdapter adapter) {
        this.hytalePlayer = hytalePlayer;
        this.adapter = adapter;
        this.methodCache = new HashMap<>();
        this.inventory = new HytalePlayerInventory(hytalePlayer);
        this.playerData = new HytalePlayerData(hytalePlayer);
        
        String name = getName();
        this.displayName = name != null ? name : "Unknown";
    }
    
    public Object getHandle() {
        return hytalePlayer;
    }
    
    @Override
    public UUID getUUID() {
        return invoke("getUUID", "getUniqueId", "getId");
    }
    
    @Override
    public String getName() {
        return invoke("getName", "getUsername", "getPlayerName");
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
        Boolean result = invoke("isOnline", "isConnected");
        return result != null && result;
    }
    
    @Override
    public void kick(String reason) {
        try {
            Method m = findMethod("kick", "disconnect");
            if (m != null) {
                if (m.getParameterCount() == 1) {
                    m.invoke(hytalePlayer, reason);
                } else {
                    m.invoke(hytalePlayer);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to kick player: " + e.getMessage());
        }
    }
    
    @Override
    public void teleport(double x, double y, double z) {
        teleport(x, y, z, getYaw(), getPitch());
    }
    
    @Override
    public void teleport(double x, double y, double z, float yaw, float pitch) {
        try {
            Method m = findMethod("teleport", "setPosition", "moveTo");
            if (m != null) {
                int params = m.getParameterCount();
                if (params == 5) {
                    m.invoke(hytalePlayer, x, y, z, yaw, pitch);
                } else if (params == 3) {
                    m.invoke(hytalePlayer, x, y, z);
                } else if (params == 1) {
                    Object pos = createPosition(x, y, z, yaw, pitch);
                    m.invoke(hytalePlayer, pos);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to teleport player: " + e.getMessage());
        }
    }
    
    private Object createPosition(double x, double y, double z, float yaw, float pitch) {
        try {
            Class<?> posClass = Class.forName("com.hypixel.hytale.world.Position");
            return posClass.getConstructor(double.class, double.class, double.class, float.class, float.class)
                .newInstance(x, y, z, yaw, pitch);
        } catch (Exception e) {
            try {
                Class<?> vecClass = Class.forName("com.hypixel.hytale.math.Vec3d");
                return vecClass.getConstructor(double.class, double.class, double.class)
                    .newInstance(x, y, z);
            } catch (Exception e2) {
                return null;
            }
        }
    }
    
    @Override
    public Location getLocation() {
        try {
            Object pos = invoke("getPosition", "getLocation", "getPos");
            if (pos != null) {
                double x = extractDouble(pos, "getX", "x");
                double y = extractDouble(pos, "getY", "y");
                double z = extractDouble(pos, "getZ", "z");
                float yaw = extractFloat(pos, "getYaw", "yaw");
                float pitch = extractFloat(pos, "getPitch", "pitch");
                return new Location(x, y, z, yaw, pitch);
            }
        } catch (Exception e) {
            logger.warning("Failed to get location: " + e.getMessage());
        }
        return new Location(0, 0, 0, 0, 0);
    }
    
    private double extractDouble(Object obj, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object result = m.invoke(obj);
                if (result instanceof Number) return ((Number) result).doubleValue();
            } catch (Exception ignored) {}
            
            try {
                var field = obj.getClass().getDeclaredField(name);
                field.setAccessible(true);
                Object result = field.get(obj);
                if (result instanceof Number) return ((Number) result).doubleValue();
            } catch (Exception ignored) {}
        }
        return 0;
    }
    
    private float extractFloat(Object obj, String... methodNames) {
        return (float) extractDouble(obj, methodNames);
    }
    
    @Override
    public String getWorld() {
        try {
            Object world = invoke("getWorld", "getCurrentWorld");
            if (world != null) {
                Method nameMethod = world.getClass().getMethod("getName");
                return (String) nameMethod.invoke(world);
            }
        } catch (Exception e) {
            logger.warning("Failed to get world: " + e.getMessage());
        }
        return "world";
    }
    
    @Override
    public int getPing() {
        Integer ping = invoke("getPing", "getLatency");
        return ping != null ? ping : 0;
    }
    
    @Override
    public String getAddress() {
        try {
            Object addr = invoke("getAddress", "getRemoteAddress", "getConnection");
            if (addr != null) {
                return addr.toString();
            }
        } catch (Exception e) {
            logger.warning("Failed to get address: " + e.getMessage());
        }
        return "unknown";
    }
    
    @Override
    public long getFirstPlayed() {
        Long result = invoke("getFirstPlayed", "getFirstJoinTime");
        return result != null ? result : System.currentTimeMillis();
    }
    
    @Override
    public long getLastPlayed() {
        Long result = invoke("getLastPlayed", "getLastJoinTime");
        return result != null ? result : System.currentTimeMillis();
    }
    
    @Override
    public boolean hasPlayedBefore() {
        Boolean result = invoke("hasPlayedBefore");
        return result != null ? result : false;
    }
    
    @Override
    public void setOp(boolean op) {
        try {
            Method m = findMethod("setOp", "setOperator");
            if (m != null) m.invoke(hytalePlayer, op);
        } catch (Exception e) {
            logger.warning("Failed to set op: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isOp() {
        Boolean result = invoke("isOp", "isOperator");
        return result != null && result;
    }
    
    @Override
    public void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        adapter.getPacketAdapter().sendTitle(this, title, subtitle, fadeIn, stay, fadeOut);
    }
    
    @Override
    public void showActionBar(String message) {
        adapter.getPacketAdapter().sendActionBar(this, message);
    }
    
    @Override
    public void playSound(String sound, float volume, float pitch) {
        adapter.getPacketAdapter().sendSound(this, sound, volume, pitch);
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
        adapter.sendPacketToPlayer(this, packet);
    }
    
    @Override
    public void sendMessage(String message) {
        try {
            Method m = findMethod("sendMessage", "sendChatMessage", "chat");
            if (m != null) m.invoke(hytalePlayer, message);
        } catch (Exception e) {
            logger.warning("Failed to send message: " + e.getMessage());
        }
    }
    
    @Override
    public boolean hasPermission(String permission) {
        try {
            Method m = findMethod("hasPermission");
            if (m != null) {
                Boolean result = (Boolean) m.invoke(hytalePlayer, permission);
                return result != null && result;
            }
        } catch (Exception e) {
            logger.warning("Failed to check permission: " + e.getMessage());
        }
        return isOp();
    }
    
    @SuppressWarnings("unchecked")
    private <T> T invoke(String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = getMethod(name);
                if (m != null) {
                    return (T) m.invoke(hytalePlayer);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
    
    private Method getMethod(String name) {
        return methodCache.computeIfAbsent(name, n -> {
            try {
                return hytalePlayer.getClass().getMethod(n);
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }
    
    private Method findMethod(String... names) {
        for (String name : names) {
            Method m = getMethod(name);
            if (m != null) return m;
        }
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HytalePlayerWrapper that = (HytalePlayerWrapper) o;
        return Objects.equals(getUUID(), that.getUUID());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getUUID());
    }
}
