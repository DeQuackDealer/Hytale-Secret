package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UIImage extends UIComponent {
    
    private String texture;
    private int u, v;
    private int textureWidth = 256;
    private int textureHeight = 256;
    private int tint = 0xFFFFFFFF;
    
    public UIImage(String id, String texture) {
        super(id);
        this.texture = texture;
        this.width = 16;
        this.height = 16;
    }
    
    public String getTexture() { return texture; }
    public int getU() { return u; }
    public int getV() { return v; }
    public int getTextureWidth() { return textureWidth; }
    public int getTextureHeight() { return textureHeight; }
    public int getTint() { return tint; }
    
    public UIImage texture(String tex) { this.texture = tex; return this; }
    public UIImage uv(int u, int v) { this.u = u; this.v = v; return this; }
    public UIImage textureSize(int w, int h) { this.textureWidth = w; this.textureHeight = h; return this; }
    public UIImage tint(int color) { this.tint = color; return this; }
    
    @Override
    public UIImage setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    @Override
    public UIImage setSize(int w, int h) { super.setSize(w, h); return this; }
}
