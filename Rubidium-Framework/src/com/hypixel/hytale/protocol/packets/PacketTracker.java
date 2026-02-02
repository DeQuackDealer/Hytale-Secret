package com.hypixel.hytale.protocol.packets;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class PacketTracker {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Packets");
    private static final PacketTracker INSTANCE = new PacketTracker();
    
    private final Map<UUID, List<PacketRecord>> playerPackets = new ConcurrentHashMap<>();
    private final List<PacketRecord> allPackets = Collections.synchronizedList(new ArrayList<>());
    
    private PacketTracker() {}
    
    public static PacketTracker get() {
        return INSTANCE;
    }
    
    public void trackPageOpen(UUID playerId, String pageId, String uiPath, CustomPageLifetime lifetime, int commandCount) {
        PacketRecord record = new PacketRecord(
            playerId, 
            PacketType.PAGE_OPEN, 
            pageId, 
            Map.of("uiPath", uiPath, "lifetime", lifetime.name(), "commandCount", commandCount)
        );
        
        playerPackets.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>())).add(record);
        allPackets.add(record);
        
        LOGGER.info("[PACKET] PAGE_OPEN: player=" + playerId + ", pageId=" + pageId + ", uiPath=" + uiPath + ", lifetime=" + lifetime + ", commands=" + commandCount);
    }
    
    public void trackPageClose(UUID playerId, String pageId) {
        PacketRecord record = new PacketRecord(
            playerId, 
            PacketType.PAGE_CLOSE, 
            pageId, 
            Map.of()
        );
        
        playerPackets.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>())).add(record);
        allPackets.add(record);
        
        LOGGER.info("[PACKET] PAGE_CLOSE: player=" + playerId + ", pageId=" + pageId);
    }
    
    public void trackUICommands(UUID playerId, String pageId, int commandCount) {
        PacketRecord record = new PacketRecord(
            playerId, 
            PacketType.UI_COMMANDS, 
            pageId, 
            Map.of("commandCount", commandCount)
        );
        
        playerPackets.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>())).add(record);
        allPackets.add(record);
        
        LOGGER.fine("[PACKET] UI_COMMANDS: player=" + playerId + ", pageId=" + pageId + ", commands=" + commandCount);
    }
    
    public List<PacketRecord> getPacketsForPlayer(UUID playerId) {
        return playerPackets.getOrDefault(playerId, Collections.emptyList());
    }
    
    public List<PacketRecord> getAllPackets() {
        return Collections.unmodifiableList(allPackets);
    }
    
    public int getPacketCount() {
        return allPackets.size();
    }
    
    public int getPacketCount(UUID playerId) {
        return playerPackets.getOrDefault(playerId, Collections.emptyList()).size();
    }
    
    public int getPacketCount(UUID playerId, PacketType type) {
        return (int) playerPackets.getOrDefault(playerId, Collections.emptyList())
            .stream()
            .filter(p -> p.type() == type)
            .count();
    }
    
    public boolean hasPacket(UUID playerId, PacketType type, String pageId) {
        return playerPackets.getOrDefault(playerId, Collections.emptyList())
            .stream()
            .anyMatch(p -> p.type() == type && p.pageId().equals(pageId));
    }
    
    public void clearPlayer(UUID playerId) {
        playerPackets.remove(playerId);
    }
    
    public void clear() {
        playerPackets.clear();
        allPackets.clear();
    }
    
    public enum PacketType {
        PAGE_OPEN, PAGE_CLOSE, UI_COMMANDS
    }
    
    public record PacketRecord(UUID playerId, PacketType type, String pageId, Map<String, Object> data) {
        public long timestamp() {
            return System.currentTimeMillis();
        }
    }
}
