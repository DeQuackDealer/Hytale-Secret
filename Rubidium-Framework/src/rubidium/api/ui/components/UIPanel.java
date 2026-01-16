package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UIPanel extends UIComponent {
    
    private int backgroundColor = 0x80000000;
    private int borderColor = 0xFF404040;
    private int borderWidth = 1;
    private int padding = 4;
    
    public UIPanel(String id) {
        super(id);
        this.width = 100;
        this.height = 100;
    }
    
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public int getBorderWidth() { return borderWidth; }
    public int getPadding() { return padding; }
    
    public UIPanel background(int color) { this.backgroundColor = color; return this; }
    public UIPanel border(int color, int width) { this.borderColor = color; this.borderWidth = width; return this; }
    public UIPanel noBorder() { this.borderWidth = 0; return this; }
    public UIPanel padding(int p) { this.padding = p; return this; }
    
    @Override
    public UIPanel setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    @Override
    public UIPanel setSize(int w, int h) { super.setSize(w, h); return this; }
    
    @Override
    public UIPanel add(UIComponent child) { super.add(child); return this; }
}
