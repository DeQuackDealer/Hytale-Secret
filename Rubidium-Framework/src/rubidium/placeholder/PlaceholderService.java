package rubidium.placeholder;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/**
 * PlaceholderAPI-style extensible placeholder system.
 * Supports both %placeholder% and {placeholder} formats.
 */
public class PlaceholderService {
    
    private static PlaceholderService instance;
    
    private final Map<String, PlaceholderProvider> providers;
    private final Map<String, PlaceholderExpansion> expansions;
    
    private static final Pattern PERCENT_PATTERN = Pattern.compile("%([^%]+)%");
    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    private PlaceholderService() {
        this.providers = new ConcurrentHashMap<>();
        this.expansions = new ConcurrentHashMap<>();
        
        registerBuiltInProviders();
    }
    
    public static PlaceholderService getInstance() {
        if (instance == null) {
            instance = new PlaceholderService();
        }
        return instance;
    }
    
    private void registerBuiltInProviders() {
        registerProvider(new PlayerNamePlaceholder());
        registerProvider(new PlayerUuidPlaceholder());
        registerProvider(new PlayerHealthPlaceholder());
        registerProvider(new PlayerWorldPlaceholder());
        registerProvider(new PlayerPingPlaceholder());
        registerProvider(new ServerOnlinePlaceholder());
        registerProvider(new ServerTpsPlaceholder());
        registerProvider(new TimePlaceholder());
        registerProvider(new DatePlaceholder());
    }
    
    public void registerProvider(PlaceholderProvider provider) {
        providers.put(provider.getIdentifier().toLowerCase(), provider);
    }
    
    public void unregisterProvider(String identifier) {
        providers.remove(identifier.toLowerCase());
    }
    
    public void registerExpansion(PlaceholderExpansion expansion) {
        expansions.put(expansion.getPrefix().toLowerCase(), expansion);
    }
    
    public void unregisterExpansion(String prefix) {
        expansions.remove(prefix.toLowerCase());
    }
    
    public String parse(Player player, String input) {
        if (input == null || input.isEmpty()) return input;
        
        String result = input;
        
        result = parsePercent(player, result);
        result = parseBrace(player, result);
        
        return result;
    }
    
    private String parsePercent(Player player, String input) {
        Matcher matcher = PERCENT_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolvePlaceholder(player, placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String parseBrace(Player player, String input) {
        Matcher matcher = BRACE_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolvePlaceholder(player, placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String resolvePlaceholder(Player player, String placeholder) {
        String lowerPlaceholder = placeholder.toLowerCase();
        
        PlaceholderProvider provider = providers.get(lowerPlaceholder);
        if (provider != null) {
            try {
                return provider.parse(player, placeholder);
            } catch (Exception e) {
                return "%" + placeholder + "%";
            }
        }
        
        int underscoreIndex = placeholder.indexOf('_');
        if (underscoreIndex > 0) {
            String prefix = placeholder.substring(0, underscoreIndex).toLowerCase();
            String params = placeholder.substring(underscoreIndex + 1);
            
            PlaceholderExpansion expansion = expansions.get(prefix);
            if (expansion != null) {
                try {
                    return expansion.parse(player, params);
                } catch (Exception e) {
                    return "%" + placeholder + "%";
                }
            }
        }
        
        return "%" + placeholder + "%";
    }
    
    public Collection<PlaceholderProvider> getProviders() {
        return providers.values();
    }
    
    public Collection<PlaceholderExpansion> getExpansions() {
        return expansions.values();
    }
    
    public int getProviderCount() {
        return providers.size();
    }
}
