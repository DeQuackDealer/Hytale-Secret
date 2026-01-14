package rubidium.ui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages UI themes.
 */
public class ThemeManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-ThemeManager");
    
    private final Map<String, Theme> themes = new ConcurrentHashMap<>();
    private Theme currentTheme;
    private final Map<UUID, String> playerThemes = new ConcurrentHashMap<>();
    
    public void loadThemes() {
        registerTheme(Theme.defaultTheme());
        registerTheme(Theme.darkTheme());
        registerTheme(Theme.lightTheme());
        
        currentTheme = themes.get("default");
        logger.info("Loaded " + themes.size() + " themes");
    }
    
    public void registerTheme(Theme theme) {
        themes.put(theme.getId(), theme);
    }
    
    public void unregisterTheme(String id) {
        if (!"default".equals(id)) {
            themes.remove(id);
        }
    }
    
    public Optional<Theme> getTheme(String id) {
        return Optional.ofNullable(themes.get(id));
    }
    
    public Collection<Theme> getAllThemes() {
        return Collections.unmodifiableCollection(themes.values());
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    public void setCurrentTheme(String themeId) {
        Theme theme = themes.get(themeId);
        if (theme != null) {
            currentTheme = theme;
        }
    }
    
    public void setPlayerTheme(UUID playerId, String themeId) {
        if (themes.containsKey(themeId)) {
            playerThemes.put(playerId, themeId);
        }
    }
    
    public Theme getPlayerTheme(UUID playerId) {
        String themeId = playerThemes.get(playerId);
        return themeId != null ? themes.getOrDefault(themeId, currentTheme) : currentTheme;
    }
    
    public void removePlayerTheme(UUID playerId) {
        playerThemes.remove(playerId);
    }
}
