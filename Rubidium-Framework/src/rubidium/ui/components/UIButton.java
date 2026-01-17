package rubidium.ui.components;

public class UIButton extends UIComponent {
    
    private String text;
    private String icon;
    private int background = 0x2D2D35;
    private int hoverBackground = 0x3D3D48;
    private int textColor = 0xFFFFFF;
    private Runnable onClick;
    
    public UIButton(String id) {
        super(id);
    }
    
    public UIButton setText(String text) {
        this.text = text;
        return this;
    }
    
    public UIButton setIcon(String icon) {
        this.icon = icon;
        return this;
    }
    
    public UIButton setBackground(int color) {
        this.background = color;
        return this;
    }
    
    public UIButton setHoverBackground(int color) {
        this.hoverBackground = color;
        return this;
    }
    
    public UIButton setTextColor(int color) {
        this.textColor = color;
        return this;
    }
    
    public UIButton onClick(Runnable handler) {
        this.onClick = handler;
        return this;
    }
    
    @Override
    public UIButton setPosition(int x, int y) {
        super.setPosition(x, y);
        return this;
    }
    
    @Override
    public UIButton setSize(int width, int height) {
        super.setSize(width, height);
        return this;
    }
    
    public String getText() { return text; }
    public String getIcon() { return icon; }
    public int getBackground() { return background; }
    public int getHoverBackground() { return hoverBackground; }
    public int getTextColor() { return textColor; }
    public Runnable getOnClick() { return onClick; }
    
    public void click() {
        if (onClick != null) {
            onClick.run();
        }
    }
}
