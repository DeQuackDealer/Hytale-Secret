package com.yellowtale.rubidium.api;

public class PluginDependency {
    
    private final String pluginId;
    private final String versionRange;
    private final boolean required;
    
    public PluginDependency(String pluginId) {
        this(pluginId, "*", true);
    }
    
    public PluginDependency(String pluginId, String versionRange) {
        this(pluginId, versionRange, true);
    }
    
    public PluginDependency(String pluginId, String versionRange, boolean required) {
        this.pluginId = pluginId;
        this.versionRange = versionRange;
        this.required = required;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public String getVersionRange() {
        return versionRange;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public boolean matchesVersion(String version) {
        if ("*".equals(versionRange)) {
            return true;
        }
        
        if (versionRange.startsWith(">=")) {
            return compareVersions(version, versionRange.substring(2).trim()) >= 0;
        }
        if (versionRange.startsWith("<=")) {
            return compareVersions(version, versionRange.substring(2).trim()) <= 0;
        }
        if (versionRange.startsWith(">")) {
            return compareVersions(version, versionRange.substring(1).trim()) > 0;
        }
        if (versionRange.startsWith("<")) {
            return compareVersions(version, versionRange.substring(1).trim()) < 0;
        }
        if (versionRange.startsWith("^")) {
            String minVersion = versionRange.substring(1).trim();
            String[] parts = minVersion.split("\\.");
            String maxMajor = String.valueOf(Integer.parseInt(parts[0]) + 1);
            return compareVersions(version, minVersion) >= 0 && 
                   compareVersions(version, maxMajor + ".0.0") < 0;
        }
        
        return version.equals(versionRange);
    }
    
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }
    
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public static PluginDependency required(String pluginId) {
        return new PluginDependency(pluginId, "*", true);
    }
    
    public static PluginDependency optional(String pluginId) {
        return new PluginDependency(pluginId, "*", false);
    }
}
