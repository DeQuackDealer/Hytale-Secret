package rubidium.qol;

import rubidium.core.logging.RubidiumLogger;

import java.util.Collection;
import java.util.Optional;

public class QoLCommands {
    
    private final QoLManager manager;
    private final RubidiumLogger logger;
    
    public QoLCommands(QoLManager manager, RubidiumLogger logger) {
        this.manager = manager;
        this.logger = logger;
    }
    
    public String handleCommand(String sender, String[] args) {
        if (args.length == 0) {
            return showHelp();
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "list" -> listFeatures();
            case "info" -> args.length > 1 ? showFeatureInfo(args[1]) : "Usage: /qol info <feature-id>";
            case "enable" -> args.length > 1 ? enableFeature(args[1]) : "Usage: /qol enable <feature-id|all>";
            case "disable" -> args.length > 1 ? disableFeature(args[1]) : "Usage: /qol disable <feature-id|all>";
            case "toggle" -> args.length > 1 ? toggleFeature(args[1]) : "Usage: /qol toggle <feature-id>";
            case "reload" -> reloadFeatures();
            default -> showHelp();
        };
    }
    
    private String showHelp() {
        return """
            QoL Feature Commands:
            /qol list - List all features and their status
            /qol info <id> - Show details about a feature
            /qol enable <id|all> - Enable a feature or all
            /qol disable <id|all> - Disable a feature or all
            /qol toggle <id> - Toggle a feature on/off
            /qol reload - Reload all feature configs""";
    }
    
    private String listFeatures() {
        Collection<QoLFeature> features = manager.getAllFeatures();
        
        if (features.isEmpty()) {
            return "No QoL features registered.";
        }
        
        StringBuilder sb = new StringBuilder("QoL Features:\n");
        int enabled = 0;
        int disabled = 0;
        
        for (QoLFeature feature : features) {
            String status = feature.isEnabled() ? "[ON]" : "[OFF]";
            sb.append(String.format("  %s %s - %s\n", status, feature.getId(), feature.getName()));
            if (feature.isEnabled()) enabled++;
            else disabled++;
        }
        
        sb.append(String.format("\nTotal: %d enabled, %d disabled", enabled, disabled));
        return sb.toString();
    }
    
    private String showFeatureInfo(String featureId) {
        Optional<QoLFeature> feature = manager.getFeature(featureId);
        
        if (feature.isEmpty()) {
            return "Feature not found: " + featureId;
        }
        
        QoLFeature f = feature.get();
        return String.format("""
            Feature: %s
            ID: %s
            Status: %s
            Description: %s""",
            f.getName(),
            f.getId(),
            f.isEnabled() ? "Enabled" : "Disabled",
            f.getDescription());
    }
    
    private String enableFeature(String featureId) {
        if ("all".equalsIgnoreCase(featureId)) {
            manager.enableAll();
            return "All QoL features enabled.";
        }
        
        Optional<QoLFeature> feature = manager.getFeature(featureId);
        if (feature.isEmpty()) {
            return "Feature not found: " + featureId;
        }
        
        manager.enableFeature(featureId);
        return "Enabled feature: " + feature.get().getName();
    }
    
    private String disableFeature(String featureId) {
        if ("all".equalsIgnoreCase(featureId)) {
            manager.disableAll();
            return "All QoL features disabled.";
        }
        
        Optional<QoLFeature> feature = manager.getFeature(featureId);
        if (feature.isEmpty()) {
            return "Feature not found: " + featureId;
        }
        
        manager.disableFeature(featureId);
        return "Disabled feature: " + feature.get().getName();
    }
    
    private String toggleFeature(String featureId) {
        Optional<QoLFeature> feature = manager.getFeature(featureId);
        if (feature.isEmpty()) {
            return "Feature not found: " + featureId;
        }
        
        boolean newState = manager.toggleFeature(featureId);
        String status = newState ? "enabled" : "disabled";
        return String.format("%s is now %s.", feature.get().getName(), status);
    }
    
    private String reloadFeatures() {
        manager.reloadAll();
        return "All QoL features reloaded.";
    }
}
