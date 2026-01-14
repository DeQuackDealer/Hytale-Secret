package rubidium.model;

/**
 * Model bone for skeletal animation support.
 */
public class ModelBone {
    
    private final String name;
    private final String parent;
    private final float[] pivot;
    private final float[] rotation;
    
    public ModelBone(String name, String parent, float[] pivot, float[] rotation) {
        this.name = name;
        this.parent = parent;
        this.pivot = pivot;
        this.rotation = rotation;
    }
    
    public String getName() { return name; }
    public String getParent() { return parent; }
    public float[] getPivot() { return pivot; }
    public float[] getRotation() { return rotation; }
    
    public boolean hasParent() {
        return parent != null && !parent.isEmpty();
    }
}
