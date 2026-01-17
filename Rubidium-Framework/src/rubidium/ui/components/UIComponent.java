package rubidium.ui.components;

public abstract class UIComponent {
    
    protected final String id;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible = true;
    
    public UIComponent(String id) {
        this.id = id;
    }
    
    public String getId() { return id; }
    
    public UIComponent setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    public UIComponent setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    public UIComponent setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isVisible() { return visible; }
}
