package rubidium.ui;

public class TextWidget extends Widget {
    private String text = "";
    private String color = "#FFFFFF";
    private int fontSize = 14;
    private String fontFamily = "default";
    private TextAlign align = TextAlign.LEFT;
    private boolean shadow = true;
    
    public TextWidget(String id) { super(id); }
    
    @Override public String getType() { return "text"; }
    
    public String getText() { return text; }
    public TextWidget setText(String text) { this.text = text; setProperty("text", text); return this; }
    
    public String getColor() { return color; }
    public TextWidget setColor(String color) { this.color = color; setProperty("color", color); return this; }
    
    public int getFontSize() { return fontSize; }
    public TextWidget setFontSize(int size) { this.fontSize = size; setProperty("fontSize", size); return this; }
    
    public String getFontFamily() { return fontFamily; }
    public TextWidget setFontFamily(String font) { this.fontFamily = font; setProperty("fontFamily", font); return this; }
    
    public TextAlign getAlign() { return align; }
    public TextWidget setAlign(TextAlign align) { this.align = align; setProperty("align", align.name()); return this; }
    
    public boolean hasShadow() { return shadow; }
    public TextWidget setShadow(boolean shadow) { this.shadow = shadow; setProperty("shadow", shadow); return this; }
    
    public enum TextAlign { LEFT, CENTER, RIGHT }
}
