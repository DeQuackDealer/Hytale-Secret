package rubidium.core.tier;

public enum ProductTier {
    FREE("Rubidium", "rubidium.jar"),
    PLUS("Rubidium Plus", "rubidium_plus.jar");
    
    private final String displayName;
    private final String jarName;
    
    ProductTier(String displayName, String jarName) {
        this.displayName = displayName;
        this.jarName = jarName;
    }
    
    public String getDisplayName() { return displayName; }
    public String getJarName() { return jarName; }
    
    public boolean isPremium() { return this == PLUS; }
    public boolean isFree() { return this == FREE; }
}
