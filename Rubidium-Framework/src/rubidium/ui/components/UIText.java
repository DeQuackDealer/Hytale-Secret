package rubidium.ui.components;

public class UIText extends UIComponent {
    
    private String text;
    private int fontSize = 14;
    private int color = 0xFFFFFF;
    
    public UIText(String id) {
        super(id);
    }
    
    public UIText setText(String text) {
        this.text = text;
        return this;
    }
    
    public UIText setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }
    
    public UIText setColor(int color) {
        this.color = color;
        return this;
    }
    
    @Override
    public UIText setPosition(int x, int y) {
        super.setPosition(x, y);
        return this;
    }
    
    public String getText() { return text; }
    public int getFontSize() { return fontSize; }
    public int getColor() { return color; }
}
