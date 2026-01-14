package rubidium.ui;

import java.util.*;

/**
 * UI theme definition.
 */
public class Theme {
    
    private final String id;
    private String name;
    private final Map<String, String> colors = new HashMap<>();
    private final Map<String, String> fonts = new HashMap<>();
    private final Map<String, Integer> spacing = new HashMap<>();
    private final Map<String, Object> widgetStyles = new HashMap<>();
    
    public Theme(String id, String name) {
        this.id = id;
        this.name = name;
        applyDefaults();
    }
    
    private void applyDefaults() {
        colors.put("primary", "#4A90D9");
        colors.put("secondary", "#6C757D");
        colors.put("success", "#28A745");
        colors.put("danger", "#DC3545");
        colors.put("warning", "#FFC107");
        colors.put("info", "#17A2B8");
        colors.put("background", "#1A1A2E");
        colors.put("surface", "#16213E");
        colors.put("text", "#FFFFFF");
        colors.put("textSecondary", "#A0A0A0");
        colors.put("border", "#3A3A5A");
        
        fonts.put("default", "Roboto");
        fonts.put("heading", "Montserrat");
        fonts.put("mono", "JetBrains Mono");
        
        spacing.put("xs", 4);
        spacing.put("sm", 8);
        spacing.put("md", 16);
        spacing.put("lg", 24);
        spacing.put("xl", 32);
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    
    public Theme setColor(String key, String value) {
        colors.put(key, value);
        return this;
    }
    
    public String getColor(String key) {
        return colors.getOrDefault(key, "#FFFFFF");
    }
    
    public Theme setFont(String key, String value) {
        fonts.put(key, value);
        return this;
    }
    
    public String getFont(String key) {
        return fonts.getOrDefault(key, "default");
    }
    
    public Theme setSpacing(String key, int value) {
        spacing.put(key, value);
        return this;
    }
    
    public int getSpacing(String key) {
        return spacing.getOrDefault(key, 8);
    }
    
    public Theme setWidgetStyle(String widgetType, Object style) {
        widgetStyles.put(widgetType, style);
        return this;
    }
    
    public Object getWidgetStyle(String widgetType) {
        return widgetStyles.get(widgetType);
    }
    
    public Map<String, String> getAllColors() {
        return Collections.unmodifiableMap(colors);
    }
    
    public static Theme defaultTheme() {
        return new Theme("default", "Default");
    }
    
    public static Theme darkTheme() {
        Theme theme = new Theme("dark", "Dark");
        theme.setColor("background", "#0D0D0D");
        theme.setColor("surface", "#1A1A1A");
        return theme;
    }
    
    public static Theme lightTheme() {
        Theme theme = new Theme("light", "Light");
        theme.setColor("background", "#F5F5F5");
        theme.setColor("surface", "#FFFFFF");
        theme.setColor("text", "#333333");
        theme.setColor("textSecondary", "#666666");
        return theme;
    }
}
