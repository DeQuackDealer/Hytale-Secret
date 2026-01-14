package rubidium.ui;

import java.util.function.Consumer;

public class InputWidget extends Widget {
    private String value = "";
    private String placeholder = "";
    private InputType inputType = InputType.TEXT;
    private int maxLength = 256;
    private String backgroundColor = "#1A1A2E";
    private String textColor = "#FFFFFF";
    private String borderColor = "#3A3A5A";
    private String focusBorderColor = "#4A90D9";
    private Consumer<String> onChange;
    private Consumer<String> onSubmit;
    
    public InputWidget(String id) { super(id); setSize(200, 32); }
    
    @Override public String getType() { return "input"; }
    
    public String getValue() { return value; }
    public InputWidget setValue(String value) { this.value = value != null ? value : ""; setProperty("value", this.value); return this; }
    
    public String getPlaceholder() { return placeholder; }
    public InputWidget setPlaceholder(String placeholder) { this.placeholder = placeholder; setProperty("placeholder", placeholder); return this; }
    
    public InputType getInputType() { return inputType; }
    public InputWidget setInputType(InputType type) { this.inputType = type; setProperty("inputType", type.name()); return this; }
    
    public int getMaxLength() { return maxLength; }
    public InputWidget setMaxLength(int max) { this.maxLength = max; setProperty("maxLength", max); return this; }
    
    public String getBackgroundColor() { return backgroundColor; }
    public InputWidget setBackgroundColor(String color) { this.backgroundColor = color; setProperty("backgroundColor", color); return this; }
    
    public String getTextColor() { return textColor; }
    public InputWidget setTextColor(String color) { this.textColor = color; setProperty("textColor", color); return this; }
    
    public String getBorderColor() { return borderColor; }
    public InputWidget setBorderColor(String color) { this.borderColor = color; setProperty("borderColor", color); return this; }
    
    public String getFocusBorderColor() { return focusBorderColor; }
    public InputWidget setFocusBorderColor(String color) { this.focusBorderColor = color; setProperty("focusBorderColor", color); return this; }
    
    public InputWidget onChange(Consumer<String> handler) { this.onChange = handler; on("value_change", e -> handler.accept(e.value())); return this; }
    public InputWidget onSubmit(Consumer<String> handler) { this.onSubmit = handler; on("submit", e -> handler.accept(value)); return this; }
    
    public enum InputType { TEXT, PASSWORD, NUMBER, EMAIL }
}
