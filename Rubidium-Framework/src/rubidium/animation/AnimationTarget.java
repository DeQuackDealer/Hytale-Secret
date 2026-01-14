package rubidium.animation;

/**
 * Interface for objects that can be animated.
 */
public interface AnimationTarget {
    
    String getId();
    
    void setPosition(String nodeName, float[] position);
    
    void setOrientation(String nodeName, float[] quaternion);
    
    void setScale(String nodeName, float[] scale);
    
    void setVisible(String nodeName, boolean visible);
    
    void setUvOffset(String nodeName, float[] offset);
}
