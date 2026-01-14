package rubidium.animation;

/**
 * Animation keyframe with time, delta values, and interpolation.
 */
public class Keyframe {
    
    private final int time;
    private final float[] delta;
    private final InterpolationType interpolationType;
    
    public Keyframe(int time, float[] delta, InterpolationType interpolationType) {
        this.time = time;
        this.delta = delta;
        this.interpolationType = interpolationType;
    }
    
    public int getTime() { return time; }
    public float[] getDelta() { return delta; }
    public InterpolationType getInterpolationType() { return interpolationType; }
    
    public float getTimeSeconds() {
        return time / 20.0f;
    }
}
