package rubidium.ui.components;

import java.util.LinkedHashMap;
import java.util.Map;

public class UIDropdown extends UIComponent {
    
    private final Map<String, String> options = new LinkedHashMap<>();
    private String selectedKey;
    
    public UIDropdown(String id) {
        super(id);
    }
    
    public UIDropdown addOption(String label, String value) {
        options.put(label, value);
        if (selectedKey == null) {
            selectedKey = label;
        }
        return this;
    }
    
    public UIDropdown setSelected(String key) {
        this.selectedKey = key;
        return this;
    }
    
    @Override
    public UIDropdown setPosition(int x, int y) {
        super.setPosition(x, y);
        return this;
    }
    
    @Override
    public UIDropdown setSize(int width, int height) {
        super.setSize(width, height);
        return this;
    }
    
    public Map<String, String> getOptions() { return options; }
    public String getSelectedKey() { return selectedKey; }
    public String getSelectedValue() { 
        return selectedKey != null ? options.get(selectedKey) : null; 
    }
}
