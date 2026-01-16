package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UITextField extends UIComponent {
    
    private String text = "";
    private String placeholder = "";
    private int maxLength = 100;
    private int textColor = 0xFFFFFFFF;
    private int backgroundColor = 0xFF000000;
    private int borderColor = 0xFFA0A0A0;
    private boolean editable = true;
    
    public UITextField(String id) {
        super(id);
        this.width = 150;
        this.height = 20;
    }
    
    public String getText() { return text; }
    public String getPlaceholder() { return placeholder; }
    public int getMaxLength() { return maxLength; }
    public int getTextColor() { return textColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public boolean isEditable() { return editable; }
    
    public UITextField text(String t) { this.text = t; return this; }
    public UITextField placeholder(String p) { this.placeholder = p; return this; }
    public UITextField maxLength(int max) { this.maxLength = max; return this; }
    public UITextField textColor(int color) { this.textColor = color; return this; }
    public UITextField background(int color) { this.backgroundColor = color; return this; }
    public UITextField border(int color) { this.borderColor = color; return this; }
    public UITextField editable(boolean e) { this.editable = e; return this; }
    
    @Override
    public UITextField setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    @Override
    public UITextField setSize(int w, int h) { super.setSize(w, h); return this; }
    
    @Override
    public UITextField on(String event, java.util.function.Consumer<UIEvent> handler) {
        super.on(event, handler);
        return this;
    }
}
