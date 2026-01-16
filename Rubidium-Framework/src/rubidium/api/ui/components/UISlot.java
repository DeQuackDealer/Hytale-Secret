package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UISlot extends UIComponent {
    
    private int slotIndex;
    private String containerType = "player";
    private boolean output = false;
    private boolean locked = false;
    
    public UISlot(String id, int index) {
        super(id);
        this.slotIndex = index;
        this.width = 18;
        this.height = 18;
    }
    
    public int getSlotIndex() { return slotIndex; }
    public String getContainerType() { return containerType; }
    public boolean isOutput() { return output; }
    public boolean isLocked() { return locked; }
    
    public UISlot index(int i) { this.slotIndex = i; return this; }
    public UISlot container(String type) { this.containerType = type; return this; }
    public UISlot output() { this.output = true; return this; }
    public UISlot locked() { this.locked = true; return this; }
    
    @Override
    public UISlot setPosition(int x, int y) { super.setPosition(x, y); return this; }
}
