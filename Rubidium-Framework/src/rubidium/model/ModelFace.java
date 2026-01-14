package rubidium.model;

/**
 * Model face with UV mapping and texture reference.
 */
public class ModelFace {
    
    private final float[] uv;
    private final String texture;
    private final int rotation;
    
    public ModelFace(float[] uv, String texture, int rotation) {
        this.uv = uv;
        this.texture = texture;
        this.rotation = rotation;
    }
    
    public float[] getUv() { return uv; }
    public String getTexture() { return texture; }
    public int getRotation() { return rotation; }
    
    public float getU1() { return uv[0]; }
    public float getV1() { return uv[1]; }
    public float getU2() { return uv[2]; }
    public float getV2() { return uv[3]; }
}
