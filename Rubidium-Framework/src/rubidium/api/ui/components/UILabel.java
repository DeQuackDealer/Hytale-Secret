package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UILabel extends UIComponent {
    
    private String text;
    private int color = 0xFFFFFFFF;
    private boolean shadow = true;
    private float scale = 1.0f;
    private Alignment alignment = Alignment.LEFT;
    
    public UILabel(String id, String text) {
        super(id);
        this.text = text;
        this.width = text.length() * 6;
        this.height = 9;
    }
    
    public String getText() { return text; }
    public int getColor() { return color; }
    public boolean hasShadow() { return shadow; }
    public float getScale() { return scale; }
    public Alignment getAlignment() { return alignment; }
    
    public UILabel text(String text) { this.text = text; return this; }
    public UILabel color(int color) { this.color = color; return this; }
    public UILabel shadow(boolean s) { this.shadow = s; return this; }
    public UILabel scale(float s) { this.scale = s; return this; }
    public UILabel align(Alignment a) { this.alignment = a; return this; }
    public UILabel centered() { this.alignment = Alignment.CENTER; return this; }
    
    @Override
    public UILabel setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    public enum Alignment { LEFT, CENTER, RIGHT }
}
