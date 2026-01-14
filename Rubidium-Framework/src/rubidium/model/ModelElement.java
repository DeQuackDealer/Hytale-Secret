package rubidium.model;

import java.util.Map;

/**
 * Model element (cube/quad) with position, rotation, and UV mapping.
 */
public class ModelElement {
    
    private final String name;
    private final String parent;
    private final float[] from;
    private final float[] to;
    private final Map<String, ModelFace> faces;
    private final float[] rotationOrigin;
    private final String rotationAxis;
    private final float rotationAngle;
    
    public ModelElement(String name, String parent, float[] from, float[] to,
                        Map<String, ModelFace> faces, float[] rotationOrigin,
                        String rotationAxis, float rotationAngle) {
        this.name = name;
        this.parent = parent;
        this.from = from;
        this.to = to;
        this.faces = faces;
        this.rotationOrigin = rotationOrigin;
        this.rotationAxis = rotationAxis;
        this.rotationAngle = rotationAngle;
    }
    
    public String getName() { return name; }
    public String getParent() { return parent; }
    public float[] getFrom() { return from; }
    public float[] getTo() { return to; }
    public Map<String, ModelFace> getFaces() { return faces; }
    public float[] getRotationOrigin() { return rotationOrigin; }
    public String getRotationAxis() { return rotationAxis; }
    public float getRotationAngle() { return rotationAngle; }
    
    public boolean hasRotation() {
        return rotationAxis != null && rotationAngle != 0;
    }
    
    public float[] getSize() {
        return new float[]{
            Math.abs(to[0] - from[0]),
            Math.abs(to[1] - from[1]),
            Math.abs(to[2] - from[2])
        };
    }
    
    public float[] getCenter() {
        return new float[]{
            (from[0] + to[0]) / 2,
            (from[1] + to[1]) / 2,
            (from[2] + to[2]) / 2
        };
    }
}
