package rubidium;

import rubidium.core.RubidiumBootstrap;
import rubidium.core.tier.FeatureRegistry;
import rubidium.ui.RubidiumOverlay;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class RubidiumLauncher {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Rubidium Framework v1.0");
        System.out.println("  Standalone UI Launcher");
        System.out.println("========================================");
        System.out.println();
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Using default look and feel");
        }
        
        RubidiumBootstrap.initialize(RubidiumLauncher.class, true);
        
        System.out.println();
        System.out.println("[Launcher] Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
        System.out.println("[Launcher] Features: " + FeatureRegistry.getAllFeatures().stream()
            .filter(FeatureRegistry.Feature::isEnabled).count() + " enabled");
        System.out.println();
        System.out.println("[Launcher] Opening UI...");
        
        SwingUtilities.invokeLater(() -> {
            RubidiumOverlay overlay = RubidiumOverlay.getInstance();
            
            overlay.showMinimap();
            
            overlay.showSettingsPanel();
            
            System.out.println("[Launcher] UI opened successfully!");
            System.out.println("[Launcher] - Settings panel: visible");
            System.out.println("[Launcher] - Minimap overlay: visible (draggable)");
            System.out.println();
            System.out.println("[Launcher] Try the following:");
            System.out.println("  - Toggle settings in the Settings panel");
            System.out.println("  - Drag the minimap around the screen");
            System.out.println("  - Click 'Open Admin Panel' for admin controls");
            System.out.println("  - Click 'Edit HUD Layout' for HUD customization");
        });
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Launcher] Shutting down...");
            RubidiumBootstrap.shutdown();
        }));
    }
}
