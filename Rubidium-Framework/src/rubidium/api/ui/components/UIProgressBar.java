package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UIProgressBar extends UIComponent {
    
    private float progress = 0.0f;
    private int backgroundColor = 0xFF404040;
    private int fillColor = 0xFF00FF00;
    private boolean vertical = false;
    private boolean showText = false;
    
    public UIProgressBar(String id) {
        super(id);
        this.width = 100;
        this.height = 10;
    }
    
    public float getProgress() { return progress; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getFillColor() { return fillColor; }
    public boolean isVertical() { return vertical; }
    public boolean showsText() { return showText; }
    
    public UIProgressBar progress(float p) { this.progress = Math.max(0, Math.min(1, p)); return this; }
    public UIProgressBar background(int color) { this.backgroundColor = color; return this; }
    public UIProgressBar fill(int color) { this.fillColor = color; return this; }
    public UIProgressBar vertical() { this.vertical = true; return this; }
    public UIProgressBar showText() { this.showText = true; return this; }
    
    @Override
    public UIProgressBar setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    @Override
    public UIProgressBar setSize(int w, int h) { super.setSize(w, h); return this; }
}
