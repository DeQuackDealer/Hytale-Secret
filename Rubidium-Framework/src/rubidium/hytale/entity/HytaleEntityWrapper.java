package rubidium.hytale.entity;

import rubidium.hytale.adapter.HytaleAdapter;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class HytaleEntityWrapper {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytaleEntity");
    
    private final Object hytaleEntity;
    private final HytaleAdapter adapter;
    private final Map<String, Method> methodCache;
    private final Map<Class<?>, Object> componentCache;
    
    public HytaleEntityWrapper(Object hytaleEntity, HytaleAdapter adapter) {
        this.hytaleEntity = hytaleEntity;
        this.adapter = adapter;
        this.methodCache = new HashMap<>();
        this.componentCache = new HashMap<>();
    }
    
    public Object getHandle() {
        return hytaleEntity;
    }
    
    public long getEntityId() {
        Long id = invoke("getEntityId", "getId");
        return id != null ? id : -1;
    }
    
    public String getEntityType() {
        String type = invoke("getType", "getEntityType");
        if (type != null) return type;
        
        try {
            Object typeObj = invoke("getEntityTypeObject");
            if (typeObj != null) {
                Method nameMethod = typeObj.getClass().getMethod("getName");
                return (String) nameMethod.invoke(typeObj);
            }
        } catch (Exception ignored) {}
        
        return hytaleEntity.getClass().getSimpleName();
    }
    
    public double getX() {
        return getPositionComponent("getX", "x");
    }
    
    public double getY() {
        return getPositionComponent("getY", "y");
    }
    
    public double getZ() {
        return getPositionComponent("getZ", "z");
    }
    
    public float getYaw() {
        return (float) getPositionComponent("getYaw", "yaw");
    }
    
    public float getPitch() {
        return (float) getPositionComponent("getPitch", "pitch");
    }
    
    private double getPositionComponent(String... methodNames) {
        try {
            Object pos = invoke("getPosition", "getPos", "getLocation");
            if (pos != null) {
                for (String name : methodNames) {
                    try {
                        Method m = pos.getClass().getMethod(name);
                        Object result = m.invoke(pos);
                        if (result instanceof Number) return ((Number) result).doubleValue();
                    } catch (Exception ignored) {}
                    
                    try {
                        var field = pos.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        Object result = field.get(pos);
                        if (result instanceof Number) return ((Number) result).doubleValue();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to get position component: " + e.getMessage());
        }
        return 0;
    }
    
    public void setPosition(double x, double y, double z) {
        try {
            Method m = findMethod("setPosition", "teleport", "moveTo");
            if (m != null) {
                int params = m.getParameterCount();
                if (params == 3) {
                    m.invoke(hytaleEntity, x, y, z);
                } else if (params == 1) {
                    Object pos = createVector(x, y, z);
                    m.invoke(hytaleEntity, pos);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to set position: " + e.getMessage());
        }
    }
    
    private Object createVector(double x, double y, double z) {
        try {
            Class<?> vecClass = Class.forName("com.hypixel.hytale.math.Vec3d");
            return vecClass.getConstructor(double.class, double.class, double.class)
                .newInstance(x, y, z);
        } catch (Exception e) {
            try {
                Class<?> posClass = Class.forName("com.hypixel.hytale.world.Position");
                return posClass.getConstructor(double.class, double.class, double.class)
                    .newInstance(x, y, z);
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    public void setVelocity(double vx, double vy, double vz) {
        try {
            Method m = findMethod("setVelocity", "setMotion", "addVelocity");
            if (m != null) {
                int params = m.getParameterCount();
                if (params == 3) {
                    m.invoke(hytaleEntity, vx, vy, vz);
                } else if (params == 1) {
                    Object vel = createVector(vx, vy, vz);
                    m.invoke(hytaleEntity, vel);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to set velocity: " + e.getMessage());
        }
    }
    
    public String getWorld() {
        try {
            Object world = invoke("getWorld", "getLevel");
            if (world != null) {
                Method nameMethod = world.getClass().getMethod("getName");
                return (String) nameMethod.invoke(world);
            }
        } catch (Exception e) {
            logger.warning("Failed to get world: " + e.getMessage());
        }
        return "world";
    }
    
    public boolean isAlive() {
        Boolean result = invoke("isAlive", "isValid", "isActive");
        return result != null && result;
    }
    
    public boolean isRemoved() {
        Boolean result = invoke("isRemoved", "isDead", "isDestroyed");
        return result != null && result;
    }
    
    public void remove() {
        try {
            Method m = findMethod("remove", "kill", "destroy", "despawn");
            if (m != null) m.invoke(hytaleEntity);
        } catch (Exception e) {
            logger.warning("Failed to remove entity: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> componentClass) {
        T cached = (T) componentCache.get(componentClass);
        if (cached != null) return cached;
        
        try {
            Method m = findMethod("getComponent", "get");
            if (m != null) {
                T component = (T) m.invoke(hytaleEntity, componentClass);
                if (component != null) {
                    componentCache.put(componentClass, component);
                }
                return component;
            }
        } catch (Exception e) {
            logger.warning("Failed to get component " + componentClass.getSimpleName() + ": " + e.getMessage());
        }
        return null;
    }
    
    public boolean hasComponent(Class<?> componentClass) {
        try {
            Method m = findMethod("hasComponent", "has");
            if (m != null) {
                Boolean result = (Boolean) m.invoke(hytaleEntity, componentClass);
                return result != null && result;
            }
        } catch (Exception e) {
            return getComponent(componentClass) != null;
        }
        return false;
    }
    
    public <T> void setComponent(T component) {
        try {
            Method m = findMethod("setComponent", "addComponent", "set");
            if (m != null) {
                m.invoke(hytaleEntity, component);
                componentCache.put(component.getClass(), component);
            }
        } catch (Exception e) {
            logger.warning("Failed to set component: " + e.getMessage());
        }
    }
    
    public void removeComponent(Class<?> componentClass) {
        try {
            Method m = findMethod("removeComponent", "remove");
            if (m != null) {
                m.invoke(hytaleEntity, componentClass);
                componentCache.remove(componentClass);
            }
        } catch (Exception e) {
            logger.warning("Failed to remove component: " + e.getMessage());
        }
    }
    
    public void sendMetadataUpdate() {
        try {
            Method m = findMethod("updateMetadata", "syncMetadata", "resyncData");
            if (m != null) m.invoke(hytaleEntity);
        } catch (Exception e) {
            logger.fine("Could not send metadata update");
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T invoke(String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = getMethod(name);
                if (m != null) {
                    return (T) m.invoke(hytaleEntity);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
    
    private Method getMethod(String name) {
        return methodCache.computeIfAbsent(name, n -> {
            try {
                return hytaleEntity.getClass().getMethod(n);
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
        HytaleEntityWrapper that = (HytaleEntityWrapper) o;
        return getEntityId() == that.getEntityId();
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(getEntityId());
    }
}
