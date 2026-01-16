package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UIButton extends UIComponent {
    
    private String text;
    private String tooltip;
    private String texture;
    private int textColor = 0xFFFFFFFF;
    private int hoverColor = 0xFFFFFFA0;
    
    public UIButton(String id) {
        super(id);
        this.text = id;
        this.width = 100;
        this.height = 20;
    }
    
    public String getText() { return text; }
    public String getTooltip() { return tooltip; }
    public String getTexture() { return texture; }
    public int getTextColor() { return textColor; }
    public int getHoverColor() { return hoverColor; }
    
    public UIButton text(String text) { this.text = text; return this; }
    public UIButton tooltip(String tip) { this.tooltip = tip; return this; }
    public UIButton texture(String tex) { this.texture = tex; return this; }
    public UIButton textColor(int color) { this.textColor = color; return this; }
    public UIButton hoverColor(int color) { this.hoverColor = color; return this; }
    
    @Override
    public UIButton setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    @Override
    public UIButton setSize(int w, int h) { super.setSize(w, h); return this; }
    
    @Override
    public UIButton on(String event, java.util.function.Consumer<UIEvent> handler) {
        super.on(event, handler);
        return this;
    }
}
