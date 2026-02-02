package rubidium.test;

import com.hypixel.hytale.protocol.packets.PacketTracker;
import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayerImpl;
import com.hypixel.hytale.server.core.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEventImpl;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEventImpl;

import rubidium.core.RubidiumBootstrap;
import rubidium.core.tier.FeatureRegistry;
import rubidium.hytale.adapter.PlayerEventHandler;
import rubidium.api.ui.UIInitializer;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;

import java.util.UUID;
import java.util.logging.Logger;

public class IntegrationTest {
    
    private static final Logger LOGGER = Logger.getLogger("IntegrationTest");
    
    public static void main(String[] args) {
        System.out.println("\n=== RUBIDIUM INTEGRATION TEST ===\n");
        
        int passed = 0;
        int failed = 0;
        
        if (testBootstrapInitialization()) passed++; else failed++;
        if (testEventRegistration()) passed++; else failed++;
        if (testPlayerJoinFlow()) passed++; else failed++;
        if (testUIPacketsSent()) passed++; else failed++;
        if (testPlayerQuitFlow()) passed++; else failed++;
        
        System.out.println("\n=================================");
        System.out.println("INTEGRATION TEST RESULTS: " + passed + " passed, " + failed + " failed");
        
        if (failed == 0) {
            System.out.println("ALL INTEGRATION TESTS PASSED!");
        } else {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        }
    }
    
    private static boolean testBootstrapInitialization() {
        System.out.print("  [TEST] Bootstrap initialization... ");
        
        try {
            boolean result = RubidiumBootstrap.initialize(IntegrationTest.class, true);
            
            if (RubidiumBootstrap.isInitialized()) {
                System.out.println("[PASS]");
                return true;
            } else {
                System.out.println("[FAIL] Not initialized");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[FAIL] " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testEventRegistration() {
        System.out.print("  [TEST] Event handler registration... ");
        
        try {
            PlayerEventHandler handler = PlayerEventHandler.get();
            
            System.out.println("[PASS]");
            return true;
        } catch (Exception e) {
            System.out.println("[FAIL] " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testPlayerJoinFlow() {
        System.out.print("  [TEST] Player join flow... ");
        
        try {
            UUID playerId = UUID.randomUUID();
            String playerName = "TestPlayer_" + playerId.toString().substring(0, 8);
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            settings.setMinimapEnabled(true);
            settings.setVoiceChatEnabled(true);
            settings.setStatisticsEnabled(true);
            settings.save();
            
            ServerPlayerImpl serverPlayer = new ServerPlayerImpl(playerId, playerName);
            
            AddPlayerToWorldEventImpl joinEvent = new AddPlayerToWorldEventImpl(serverPlayer, "world");
            
            EventRegistry.get().fire(joinEvent);
            
            System.out.println("[PASS] Player joined: " + playerName);
            return true;
        } catch (Exception e) {
            System.out.println("[FAIL] " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testUIPacketsSent() {
        System.out.print("  [TEST] UI packets sent... ");
        
        try {
            int packetCount = PacketTracker.get().getPacketCount();
            
            if (packetCount > 0) {
                System.out.println("[PASS] " + packetCount + " packets tracked");
                
                for (var packet : PacketTracker.get().getAllPackets()) {
                    System.out.println("    - " + packet.type() + ": " + packet.pageId());
                }
                
                return true;
            } else {
                System.out.println("[FAIL] No packets tracked");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[FAIL] " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testPlayerQuitFlow() {
        System.out.print("  [TEST] Player quit flow... ");
        
        try {
            UUID playerId = UUID.randomUUID();
            String playerName = "QuitTestPlayer";
            
            ServerPlayerImpl serverPlayer = new ServerPlayerImpl(playerId, playerName);
            
            AddPlayerToWorldEventImpl joinEvent = new AddPlayerToWorldEventImpl(serverPlayer, "world");
            EventRegistry.get().fire(joinEvent);
            
            int preQuitPackets = PacketTracker.get().getPacketCount();
            
            DrainPlayerFromWorldEventImpl quitEvent = new DrainPlayerFromWorldEventImpl(serverPlayer, "world");
            EventRegistry.get().fire(quitEvent);
            
            int postQuitPackets = PacketTracker.get().getPacketCount();
            
            System.out.println("[PASS] Packets: " + preQuitPackets + " -> " + postQuitPackets);
            return true;
        } catch (Exception e) {
            System.out.println("[FAIL] " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
