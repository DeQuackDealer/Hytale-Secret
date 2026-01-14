package rubidium.ui;

import java.util.function.Consumer;

public class ButtonWidget extends Widget {
    private String label = "";
    private String icon;
    private String backgroundColor = "#4A90D9";
    private String textColor = "#FFFFFF";
    private String hoverColor = "#5AA0E9";
    private String disabledColor = "#666666";
    private Consumer<RubidiumUI.UIInputEvent> onClick;
    
    public ButtonWidget(String id) { super(id); setSize(120, 32); }
    
    @Override public String getType() { return "button"; }
    
    public String getLabel() { return label; }
    public ButtonWidget setLabel(String label) { this.label = label; setProperty("label", label); return this; }
    
    public String getIcon() { return icon; }
    public ButtonWidget setIcon(String icon) { this.icon = icon; setProperty("icon", icon); return this; }
    
    public String getBackgroundColor() { return backgroundColor; }
    public ButtonWidget setBackgroundColor(String color) { this.backgroundColor = color; setProperty("backgroundColor", color); return this; }
    
    public String getTextColor() { return textColor; }
    public ButtonWidget setTextColor(String color) { this.textColor = color; setProperty("textColor", color); return this; }
    
    public String getHoverColor() { return hoverColor; }
    public ButtonWidget setHoverColor(String color) { this.hoverColor = color; setProperty("hoverColor", color); return this; }
    
    public String getDisabledColor() { return disabledColor; }
    public ButtonWidget setDisabledColor(String color) { this.disabledColor = color; setProperty("disabledColor", color); return this; }
    
    public ButtonWidget onClick(Consumer<RubidiumUI.UIInputEvent> handler) {
        this.onClick = handler;
        on("click", handler);
        return this;
    }
}
