package rubidium.animation;

import java.util.*;

/**
 * Animation data for a specific node/bone.
 */
public class NodeAnimation {
    
    private final String nodeName;
    private final List<Keyframe> position;
    private final List<Keyframe> orientation;
    private final List<Keyframe> shapeStretch;
    private final List<Keyframe> shapeVisible;
    private final List<Keyframe> shapeUvOffset;
    
    public NodeAnimation(String nodeName, List<Keyframe> position, List<Keyframe> orientation,
                         List<Keyframe> shapeStretch, List<Keyframe> shapeVisible,
                         List<Keyframe> shapeUvOffset) {
        this.nodeName = nodeName;
        this.position = position != null ? position : new ArrayList<>();
        this.orientation = orientation != null ? orientation : new ArrayList<>();
        this.shapeStretch = shapeStretch != null ? shapeStretch : new ArrayList<>();
        this.shapeVisible = shapeVisible != null ? shapeVisible : new ArrayList<>();
        this.shapeUvOffset = shapeUvOffset != null ? shapeUvOffset : new ArrayList<>();
    }
    
    public static Builder builder(String nodeName) {
        return new Builder(nodeName);
    }
    
    public String getNodeName() { return nodeName; }
    public List<Keyframe> getPosition() { return position; }
    public List<Keyframe> getOrientation() { return orientation; }
    public List<Keyframe> getShapeStretch() { return shapeStretch; }
    public List<Keyframe> getShapeVisible() { return shapeVisible; }
    public List<Keyframe> getShapeUvOffset() { return shapeUvOffset; }
    
    public static class Builder {
        private final String nodeName;
        private final List<Keyframe> position = new ArrayList<>();
        private final List<Keyframe> orientation = new ArrayList<>();
        private final List<Keyframe> shapeStretch = new ArrayList<>();
        private final List<Keyframe> shapeVisible = new ArrayList<>();
        private final List<Keyframe> shapeUvOffset = new ArrayList<>();
        
        public Builder(String nodeName) { this.nodeName = nodeName; }
        
        public Builder addPosition(int time, float x, float y, float z) {
            position.add(new Keyframe(time, new float[]{x, y, z}, InterpolationType.SMOOTH));
            return this;
        }
        
        public Builder addOrientation(int time, float x, float y, float z, float w) {
            orientation.add(new Keyframe(time, new float[]{x, y, z, w}, InterpolationType.SMOOTH));
            return this;
        }
        
        public Builder addScale(int time, float x, float y, float z) {
            shapeStretch.add(new Keyframe(time, new float[]{x, y, z}, InterpolationType.SMOOTH));
            return this;
        }
        
        public Builder addVisibility(int time, boolean visible) {
            shapeVisible.add(new Keyframe(time, new float[]{visible ? 1 : 0}, InterpolationType.STEP));
            return this;
        }
        
        public NodeAnimation build() {
            return new NodeAnimation(nodeName, position, orientation, shapeStretch, shapeVisible, shapeUvOffset);
        }
    }
}
