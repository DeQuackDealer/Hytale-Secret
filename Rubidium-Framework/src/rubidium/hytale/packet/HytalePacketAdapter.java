package rubidium.hytale.packet;

import rubidium.api.player.Player;
import rubidium.hytale.adapter.HytaleAdapter;
import rubidium.hytale.player.HytalePlayerWrapper;
import rubidium.display.*;
import rubidium.bossbar.BossBar;
import rubidium.inventory.CustomInventory;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

public class HytalePacketAdapter {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytalePackets");
    
    private final HytaleAdapter adapter;
    
    private Class<?> titlePacketClass;
    private Class<?> actionBarPacketClass;
    private Class<?> soundPacketClass;
    private Class<?> scoreboardPacketClass;
    private Class<?> bossBarPacketClass;
    private Class<?> inventoryPacketClass;
    private Class<?> particlePacketClass;
    
    private Method sendPacketMethod;
    
    public HytalePacketAdapter(HytaleAdapter adapter) {
        this.adapter = adapter;
    }
    
    public void initialize(Object hytaleServer) {
        loadPacketClasses();
        findSendPacketMethod();
        logger.info("Hytale packet adapter initialized");
    }
    
    private void loadPacketClasses() {
        String[] basePackages = {
            "com.hypixel.hytale.protocol.packets",
            "com.hypixel.hytale.network.packets",
            "com.hypixel.hytale.packets"
        };
        
        Map<String, String[]> packetMappings = Map.of(
            "title", new String[]{"TitlePacket", "SetTitlePacket", "ClientboundTitlePacket"},
            "actionBar", new String[]{"ActionBarPacket", "SetActionBarPacket", "ClientboundActionBarPacket"},
            "sound", new String[]{"SoundPacket", "PlaySoundPacket", "ClientboundSoundPacket"},
            "scoreboard", new String[]{"ScoreboardPacket", "SetScoreboardPacket", "ClientboundScoreboardPacket"},
            "bossBar", new String[]{"BossBarPacket", "BossEventPacket", "ClientboundBossEventPacket"},
            "inventory", new String[]{"InventoryPacket", "ContainerPacket", "ClientboundContainerPacket"},
            "particle", new String[]{"ParticlePacket", "SpawnParticlePacket", "ClientboundParticlePacket"}
        );
        
        for (String basePackage : basePackages) {
            for (Map.Entry<String, String[]> entry : packetMappings.entrySet()) {
                for (String className : entry.getValue()) {
                    try {
                        Class<?> clazz = Class.forName(basePackage + "." + className);
                        setPacketClass(entry.getKey(), clazz);
                        logger.fine("Found packet class: " + clazz.getName());
                        break;
                    } catch (ClassNotFoundException ignored) {}
                }
            }
        }
    }
    
    private void setPacketClass(String type, Class<?> clazz) {
        switch (type) {
            case "title" -> titlePacketClass = clazz;
            case "actionBar" -> actionBarPacketClass = clazz;
            case "sound" -> soundPacketClass = clazz;
            case "scoreboard" -> scoreboardPacketClass = clazz;
            case "bossBar" -> bossBarPacketClass = clazz;
            case "inventory" -> inventoryPacketClass = clazz;
            case "particle" -> particlePacketClass = clazz;
        }
    }
    
    private void findSendPacketMethod() {
        try {
            Class<?> playerClass = Class.forName("com.hypixel.hytale.entity.Player");
            String[] methodNames = {"sendPacket", "send", "connection.send", "getConnection().send"};
            
            for (String name : methodNames) {
                try {
                    sendPacketMethod = playerClass.getMethod(name, Object.class);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            logger.warning("Could not find send packet method");
        }
    }
    
    public void sendPacket(Player player, Object packet) {
        if (!(player instanceof HytalePlayerWrapper wrapper)) {
            logger.warning("Cannot send packet to non-Hytale player");
            return;
        }
        
        Object hytalePlayer = wrapper.getHandle();
        
        if (packet instanceof ScoreboardManager.ScoreboardPacket sp) {
            sendScoreboardPacket(hytalePlayer, sp);
        } else if (packet instanceof BossBar.BossBarPacket bp) {
            sendBossBarPacket(hytalePlayer, bp);
        } else if (packet instanceof CustomInventory.OpenInventoryPacket ip) {
            sendInventoryPacket(hytalePlayer, ip);
        } else if (packet instanceof CustomInventory.CloseInventoryPacket cp) {
            sendCloseInventoryPacket(hytalePlayer, cp);
        } else if (packet instanceof CustomInventory.UpdateSlotPacket up) {
            sendUpdateSlotPacket(hytalePlayer, up);
        } else {
            sendRawPacket(hytalePlayer, packet);
        }
    }
    
    public void broadcastPacket(Object packet) {
        for (Player player : adapter.getOnlinePlayers()) {
            sendPacket(player, packet);
        }
    }
    
    private void sendRawPacket(Object hytalePlayer, Object packet) {
        try {
            if (sendPacketMethod != null) {
                sendPacketMethod.invoke(hytalePlayer, packet);
            } else {
                Method m = hytalePlayer.getClass().getMethod("sendPacket", Object.class);
                m.invoke(hytalePlayer, packet);
            }
        } catch (Exception e) {
            try {
                Object connection = hytalePlayer.getClass().getMethod("getConnection").invoke(hytalePlayer);
                Method sendMethod = connection.getClass().getMethod("send", Object.class);
                sendMethod.invoke(connection, packet);
            } catch (Exception ex) {
                logger.warning("Failed to send packet: " + ex.getMessage());
            }
        }
    }
    
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!(player instanceof HytalePlayerWrapper wrapper)) return;
        
        Object hytalePlayer = wrapper.getHandle();
        
        try {
            if (titlePacketClass != null) {
                Object packet = titlePacketClass.getConstructor(
                    String.class, String.class, int.class, int.class, int.class
                ).newInstance(title, subtitle, fadeIn, stay, fadeOut);
                sendRawPacket(hytalePlayer, packet);
            } else {
                Method m = hytalePlayer.getClass().getMethod("showTitle", 
                    String.class, String.class, int.class, int.class, int.class);
                m.invoke(hytalePlayer, title, subtitle, fadeIn, stay, fadeOut);
            }
        } catch (Exception e) {
            logger.warning("Failed to send title: " + e.getMessage());
        }
    }
    
    public void sendActionBar(Player player, String message) {
        if (!(player instanceof HytalePlayerWrapper wrapper)) return;
        
        Object hytalePlayer = wrapper.getHandle();
        
        try {
            if (actionBarPacketClass != null) {
                Object packet = actionBarPacketClass.getConstructor(String.class).newInstance(message);
                sendRawPacket(hytalePlayer, packet);
            } else {
                Method m = hytalePlayer.getClass().getMethod("sendActionBar", String.class);
                m.invoke(hytalePlayer, message);
            }
        } catch (Exception e) {
            logger.warning("Failed to send action bar: " + e.getMessage());
        }
    }
    
    public void sendSound(Player player, String sound, float volume, float pitch) {
        if (!(player instanceof HytalePlayerWrapper wrapper)) return;
        
        Object hytalePlayer = wrapper.getHandle();
        
        try {
            if (soundPacketClass != null) {
                Object packet = soundPacketClass.getConstructor(
                    String.class, float.class, float.class
                ).newInstance(sound, volume, pitch);
                sendRawPacket(hytalePlayer, packet);
            } else {
                Method m = hytalePlayer.getClass().getMethod("playSound", String.class, float.class, float.class);
                m.invoke(hytalePlayer, sound, volume, pitch);
            }
        } catch (Exception e) {
            logger.warning("Failed to send sound: " + e.getMessage());
        }
    }
    
    private void sendScoreboardPacket(Object hytalePlayer, ScoreboardManager.ScoreboardPacket packet) {
        try {
            if (scoreboardPacketClass != null) {
                Object nativePacket = scoreboardPacketClass.getConstructor(String.class, List.class)
                    .newInstance(packet.title(), packet.lines());
                sendRawPacket(hytalePlayer, nativePacket);
            } else {
                Method m = hytalePlayer.getClass().getMethod("setScoreboard", String.class, List.class);
                m.invoke(hytalePlayer, packet.title(), packet.lines());
            }
        } catch (Exception e) {
            logger.fine("Scoreboard packet not sent: " + e.getMessage());
        }
    }
    
    private void sendBossBarPacket(Object hytalePlayer, BossBar.BossBarPacket packet) {
        try {
            if (bossBarPacketClass != null) {
                Object nativePacket = bossBarPacketClass.getConstructor(
                    String.class, String.class, float.class
                ).newInstance(packet.id(), packet.title(), packet.progress());
                sendRawPacket(hytalePlayer, nativePacket);
            } else {
                Method m = hytalePlayer.getClass().getMethod("showBossBar", 
                    String.class, String.class, float.class);
                m.invoke(hytalePlayer, packet.id(), packet.title(), packet.progress());
            }
        } catch (Exception e) {
            logger.fine("BossBar packet not sent: " + e.getMessage());
        }
    }
    
    private void sendInventoryPacket(Object hytalePlayer, CustomInventory.OpenInventoryPacket packet) {
        try {
            if (inventoryPacketClass != null) {
                Object nativePacket = inventoryPacketClass.getConstructor(
                    String.class, String.class, int.class, List.class
                ).newInstance(packet.id(), packet.title(), packet.rows(), packet.items());
                sendRawPacket(hytalePlayer, nativePacket);
            } else {
                Method m = hytalePlayer.getClass().getMethod("openInventory", 
                    String.class, String.class, int.class);
                m.invoke(hytalePlayer, packet.id(), packet.title(), packet.rows());
            }
        } catch (Exception e) {
            logger.fine("Inventory packet not sent: " + e.getMessage());
        }
    }
    
    private void sendCloseInventoryPacket(Object hytalePlayer, CustomInventory.CloseInventoryPacket packet) {
        try {
            Method m = hytalePlayer.getClass().getMethod("closeInventory");
            m.invoke(hytalePlayer);
        } catch (Exception e) {
            logger.fine("Close inventory not sent: " + e.getMessage());
        }
    }
    
    private void sendUpdateSlotPacket(Object hytalePlayer, CustomInventory.UpdateSlotPacket packet) {
        try {
            Method m = hytalePlayer.getClass().getMethod("updateInventorySlot", int.class, Object.class);
            m.invoke(hytalePlayer, packet.slot(), packet.item());
        } catch (Exception e) {
            logger.fine("Update slot not sent: " + e.getMessage());
        }
    }
    
    public void sendParticles(Player player, String particleType, double x, double y, double z, 
                               int count, double offsetX, double offsetY, double offsetZ, double speed) {
        if (!(player instanceof HytalePlayerWrapper wrapper)) return;
        
        Object hytalePlayer = wrapper.getHandle();
        
        try {
            if (particlePacketClass != null) {
                Object packet = particlePacketClass.getConstructor(
                    String.class, double.class, double.class, double.class,
                    int.class, double.class, double.class, double.class, double.class
                ).newInstance(particleType, x, y, z, count, offsetX, offsetY, offsetZ, speed);
                sendRawPacket(hytalePlayer, packet);
            } else {
                Method m = hytalePlayer.getClass().getMethod("spawnParticle",
                    String.class, double.class, double.class, double.class, int.class);
                m.invoke(hytalePlayer, particleType, x, y, z, count);
            }
        } catch (Exception e) {
            logger.fine("Particle packet not sent: " + e.getMessage());
        }
    }
}
