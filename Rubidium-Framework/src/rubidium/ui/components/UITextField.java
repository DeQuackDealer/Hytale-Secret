package rubidium.ui.components;

public class UITextField extends UIComponent {
    
    private String placeholder;
    private String value = "";
    private int maxLength = 256;
    
    public UITextField(String id) {
        super(id);
    }
    
    public UITextField setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }
    
    public UITextField setValue(String value) {
        this.value = value;
        return this;
    }
    
    public UITextField setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }
    
    @Override
    public UITextField setPosition(int x, int y) {
        super.setPosition(x, y);
        return this;
    }
    
    @Override
    public UITextField setSize(int width, int height) {
        super.setSize(width, height);
        return this;
    }
    
    public String getPlaceholder() { return placeholder; }
    public String getValue() { return value != null ? value : ""; }
    public int getMaxLength() { return maxLength; }
}
