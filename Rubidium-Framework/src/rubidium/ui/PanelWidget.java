package rubidium.ui;

public class PanelWidget extends Widget {
    private String backgroundColor = "#16213E";
    private String borderColor = "#3A3A5A";
    private int borderWidth = 1;
    private int borderRadius = 4;
    private int padding = 8;
    private LayoutDirection layoutDirection = LayoutDirection.VERTICAL;
    private int gap = 4;
    
    public PanelWidget(String id) { super(id); }
    
    @Override public String getType() { return "panel"; }
    
    public String getBackgroundColor() { return backgroundColor; }
    public PanelWidget setBackgroundColor(String color) { this.backgroundColor = color; setProperty("backgroundColor", color); return this; }
    
    public String getBorderColor() { return borderColor; }
    public PanelWidget setBorderColor(String color) { this.borderColor = color; setProperty("borderColor", color); return this; }
    
    public int getBorderWidth() { return borderWidth; }
    public PanelWidget setBorderWidth(int width) { this.borderWidth = width; setProperty("borderWidth", width); return this; }
    
    public int getBorderRadius() { return borderRadius; }
    public PanelWidget setBorderRadius(int radius) { this.borderRadius = radius; setProperty("borderRadius", radius); return this; }
    
    public int getPadding() { return padding; }
    public PanelWidget setPadding(int padding) { this.padding = padding; setProperty("padding", padding); return this; }
    
    public LayoutDirection getLayoutDirection() { return layoutDirection; }
    public PanelWidget setLayoutDirection(LayoutDirection dir) { this.layoutDirection = dir; setProperty("layoutDirection", dir.name()); return this; }
    
    public int getGap() { return gap; }
    public PanelWidget setGap(int gap) { this.gap = gap; setProperty("gap", gap); return this; }
    
    public enum LayoutDirection { HORIZONTAL, VERTICAL }
}
