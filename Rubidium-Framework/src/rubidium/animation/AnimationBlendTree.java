package rubidium.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AnimationBlendTree {
    
    private final String id;
    private final BlendNode rootNode;
    private final Map<String, Float> parameters;
    
    public AnimationBlendTree(String id, BlendNode rootNode) {
        this.id = id;
        this.rootNode = rootNode;
        this.parameters = new HashMap<>();
    }
    
    public String getId() { return id; }
    public BlendNode getRootNode() { return rootNode; }
    
    public void setParameter(String name, float value) {
        parameters.put(name, value);
    }
    
    public float getParameter(String name) {
        return parameters.getOrDefault(name, 0f);
    }
    
    public BlendResult evaluate() {
        return rootNode.evaluate(parameters);
    }
    
    public static abstract class BlendNode {
        protected final String name;
        
        protected BlendNode(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
        
        public abstract BlendResult evaluate(Map<String, Float> parameters);
    }
    
    public static class AnimationNode extends BlendNode {
        private final String animationId;
        
        public AnimationNode(String name, String animationId) {
            super(name);
            this.animationId = animationId;
        }
        
        public String getAnimationId() { return animationId; }
        
        @Override
        public BlendResult evaluate(Map<String, Float> parameters) {
            return new BlendResult(List.of(new BlendResult.Entry(animationId, 1.0f)));
        }
    }
    
    public static class Blend1DNode extends BlendNode {
        private final String blendParameter;
        private final List<BlendPoint> points;
        
        public Blend1DNode(String name, String blendParameter) {
            super(name);
            this.blendParameter = blendParameter;
            this.points = new ArrayList<>();
        }
        
        public void addPoint(float threshold, BlendNode node) {
            points.add(new BlendPoint(threshold, node));
            points.sort((a, b) -> Float.compare(a.threshold, b.threshold));
        }
        
        @Override
        public BlendResult evaluate(Map<String, Float> parameters) {
            float value = parameters.getOrDefault(blendParameter, 0f);
            
            if (points.isEmpty()) {
                return new BlendResult(List.of());
            }
            
            if (points.size() == 1) {
                return points.get(0).node.evaluate(parameters);
            }
            
            BlendPoint lower = null;
            BlendPoint upper = null;
            
            for (BlendPoint point : points) {
                if (point.threshold <= value) {
                    lower = point;
                }
                if (point.threshold >= value && upper == null) {
                    upper = point;
                }
            }
            
            if (lower == null) return points.get(0).node.evaluate(parameters);
            if (upper == null) return points.get(points.size() - 1).node.evaluate(parameters);
            if (lower == upper) return lower.node.evaluate(parameters);
            
            float t = (value - lower.threshold) / (upper.threshold - lower.threshold);
            BlendResult lowerResult = lower.node.evaluate(parameters);
            BlendResult upperResult = upper.node.evaluate(parameters);
            
            return lowerResult.blend(upperResult, t);
        }
        
        private record BlendPoint(float threshold, BlendNode node) {}
    }
    
    public static class Blend2DNode extends BlendNode {
        private final String parameterX;
        private final String parameterY;
        private final List<Blend2DPoint> points;
        
        public Blend2DNode(String name, String parameterX, String parameterY) {
            super(name);
            this.parameterX = parameterX;
            this.parameterY = parameterY;
            this.points = new ArrayList<>();
        }
        
        public void addPoint(float x, float y, BlendNode node) {
            points.add(new Blend2DPoint(x, y, node));
        }
        
        @Override
        public BlendResult evaluate(Map<String, Float> parameters) {
            float x = parameters.getOrDefault(parameterX, 0f);
            float y = parameters.getOrDefault(parameterY, 0f);
            
            if (points.isEmpty()) {
                return new BlendResult(List.of());
            }
            
            float totalWeight = 0;
            List<BlendResult.Entry> entries = new ArrayList<>();
            
            for (Blend2DPoint point : points) {
                float dist = (float) Math.sqrt(
                    Math.pow(point.x - x, 2) + Math.pow(point.y - y, 2)
                );
                float weight = 1.0f / (dist + 0.0001f);
                
                BlendResult result = point.node.evaluate(parameters);
                for (BlendResult.Entry entry : result.entries()) {
                    entries.add(new BlendResult.Entry(entry.animationId(), entry.weight() * weight));
                }
                totalWeight += weight;
            }
            
            final float finalTotalWeight = totalWeight;
            List<BlendResult.Entry> normalized = entries.stream()
                .map(e -> new BlendResult.Entry(e.animationId(), e.weight() / finalTotalWeight))
                .toList();
            
            return new BlendResult(normalized);
        }
        
        private record Blend2DPoint(float x, float y, BlendNode node) {}
    }
    
    public record BlendResult(List<Entry> entries) {
        
        public BlendResult blend(BlendResult other, float t) {
            List<Entry> combined = new ArrayList<>();
            
            for (Entry e : this.entries) {
                combined.add(new Entry(e.animationId, e.weight * (1 - t)));
            }
            for (Entry e : other.entries) {
                combined.add(new Entry(e.animationId, e.weight * t));
            }
            
            return new BlendResult(combined);
        }
        
        public record Entry(String animationId, float weight) {}
    }
}
