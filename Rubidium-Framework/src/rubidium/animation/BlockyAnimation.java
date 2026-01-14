package rubidium.animation;

import java.util.*;

/**
 * Represents a .blockyanim animation file.
 */
public class BlockyAnimation {
    
    private final String id;
    private final int formatVersion;
    private final int duration;
    private final boolean holdLastKeyframe;
    private final Map<String, NodeAnimation> nodeAnimations;
    
    public BlockyAnimation(String id, int formatVersion, int duration, 
                           boolean holdLastKeyframe, Map<String, NodeAnimation> nodeAnimations) {
        this.id = id;
        this.formatVersion = formatVersion;
        this.duration = duration;
        this.holdLastKeyframe = holdLastKeyframe;
        this.nodeAnimations = nodeAnimations;
    }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public String getId() { return id; }
    public int getFormatVersion() { return formatVersion; }
    public int getDuration() { return duration; }
    public boolean isHoldLastKeyframe() { return holdLastKeyframe; }
    public Map<String, NodeAnimation> getNodeAnimations() { return nodeAnimations; }
    
    public float getDurationSeconds() {
        return duration / 20.0f;
    }
    
    public static class Builder {
        private final String id;
        private int formatVersion = 1;
        private int duration = 20;
        private boolean holdLastKeyframe = true;
        private final Map<String, NodeAnimation> nodeAnimations = new HashMap<>();
        
        public Builder(String id) { this.id = id; }
        
        public Builder formatVersion(int v) { this.formatVersion = v; return this; }
        public Builder duration(int ticks) { this.duration = ticks; return this; }
        public Builder holdLastKeyframe(boolean hold) { this.holdLastKeyframe = hold; return this; }
        
        public Builder addNode(NodeAnimation node) {
            nodeAnimations.put(node.getNodeName(), node);
            return this;
        }
        
        public BlockyAnimation build() {
            return new BlockyAnimation(id, formatVersion, duration, holdLastKeyframe, nodeAnimations);
        }
    }
}
