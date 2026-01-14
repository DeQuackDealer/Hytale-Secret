package rubidium.ui;

public class ImageWidget extends Widget {
    private String source;
    private ScaleMode scaleMode = ScaleMode.FIT;
    private float opacity = 1.0f;
    
    public ImageWidget(String id) { super(id); }
    
    @Override public String getType() { return "image"; }
    
    public String getSource() { return source; }
    public ImageWidget setSource(String source) { this.source = source; setProperty("source", source); return this; }
    
    public ScaleMode getScaleMode() { return scaleMode; }
    public ImageWidget setScaleMode(ScaleMode mode) { this.scaleMode = mode; setProperty("scaleMode", mode.name()); return this; }
    
    public float getOpacity() { return opacity; }
    public ImageWidget setOpacity(float opacity) { this.opacity = Math.max(0f, Math.min(1f, opacity)); setProperty("opacity", this.opacity); return this; }
    
    public enum ScaleMode { FIT, FILL, STRETCH, NONE }
}
