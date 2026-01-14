package rubidium.ui;

import java.util.function.Consumer;

public class ToggleWidget extends Widget {
    private boolean checked = false;
    private String label;
    private String onColor = "#4A90D9";
    private String offColor = "#666666";
    private Consumer<Boolean> onChange;
    
    public ToggleWidget(String id) { super(id); setSize(48, 24); }
    
    @Override public String getType() { return "toggle"; }
    
    public boolean isChecked() { return checked; }
    public ToggleWidget setChecked(boolean checked) { this.checked = checked; setProperty("checked", checked); return this; }
    public ToggleWidget toggle() { return setChecked(!checked); }
    
    public String getLabel() { return label; }
    public ToggleWidget setLabel(String label) { this.label = label; setProperty("label", label); return this; }
    
    public String getOnColor() { return onColor; }
    public ToggleWidget setOnColor(String color) { this.onColor = color; setProperty("onColor", color); return this; }
    
    public String getOffColor() { return offColor; }
    public ToggleWidget setOffColor(String color) { this.offColor = color; setProperty("offColor", color); return this; }
    
    public ToggleWidget onChange(Consumer<Boolean> handler) { this.onChange = handler; on("click", e -> { toggle(); handler.accept(checked); }); return this; }
}
