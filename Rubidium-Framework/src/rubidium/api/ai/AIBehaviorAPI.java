package rubidium.api.ai;

import rubidium.api.pathfinding.PathfindingAPI;
import rubidium.api.pathfinding.PathfindingAPI.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class AIBehaviorAPI {
    
    private static final Map<String, BehaviorTree> behaviorTrees = new ConcurrentHashMap<>();
    private static final Map<String, GoalSelector> goalSelectors = new ConcurrentHashMap<>();
    
    private AIBehaviorAPI() {}
    
    public static BehaviorTree.Builder createTree(String id) {
        return new BehaviorTree.Builder(id);
    }
    
    public static BehaviorTree registerTree(BehaviorTree tree) {
        behaviorTrees.put(tree.getId(), tree);
        return tree;
    }
    
    public static Optional<BehaviorTree> getTree(String id) {
        return Optional.ofNullable(behaviorTrees.get(id));
    }
    
    public static GoalSelector createGoalSelector(String id) {
        GoalSelector selector = new GoalSelector(id);
        goalSelectors.put(id, selector);
        return selector;
    }
    
    public static Optional<GoalSelector> getGoalSelector(String id) {
        return Optional.ofNullable(goalSelectors.get(id));
    }
    
    public static BehaviorNode sequence(BehaviorNode... children) {
        return new SequenceNode(Arrays.asList(children));
    }
    
    public static BehaviorNode selector(BehaviorNode... children) {
        return new SelectorNode(Arrays.asList(children));
    }
    
    public static BehaviorNode parallel(BehaviorNode... children) {
        return new ParallelNode(Arrays.asList(children));
    }
    
    public static BehaviorNode condition(Predicate<AIContext> condition, BehaviorNode child) {
        return new ConditionalNode(condition, child);
    }
    
    public static BehaviorNode inverter(BehaviorNode child) {
        return new InverterNode(child);
    }
    
    public static BehaviorNode repeater(BehaviorNode child, int times) {
        return new RepeaterNode(child, times);
    }
    
    public static BehaviorNode action(Consumer<AIContext> action) {
        return new ActionNode(action);
    }
    
    public static BehaviorNode wait(int ticks) {
        return new WaitNode(ticks);
    }
    
    public static BehaviorNode moveTo(Vec3i target) {
        return new MoveToNode(target);
    }
    
    public static BehaviorNode moveToTarget() {
        return new MoveToTargetNode();
    }
    
    public static BehaviorNode lookAt(Vec3i target) {
        return new LookAtNode(target);
    }
    
    public static BehaviorNode lookAtTarget() {
        return new LookAtTargetNode();
    }
    
    public static BehaviorNode attack() {
        return new AttackNode();
    }
    
    public static BehaviorNode flee(double distance) {
        return new FleeNode(distance);
    }
    
    public static BehaviorNode wander(int radius) {
        return new WanderNode(radius);
    }
    
    public static BehaviorNode patrol(List<Vec3i> points) {
        return new PatrolNode(points);
    }
    
    public enum NodeStatus {
        SUCCESS, FAILURE, RUNNING
    }
    
    public interface BehaviorNode {
        NodeStatus tick(AIContext context);
        default void reset() {}
    }
    
    public record AIContext(
        Object entity,
        Vec3i position,
        Object target,
        Map<String, Object> blackboard,
        PathfindingContext pathContext
    ) {
        public static AIContext create(Object entity, Vec3i position) {
            return new AIContext(entity, position, null, new HashMap<>(), null);
        }
        
        public AIContext withTarget(Object target) {
            return new AIContext(entity, position, target, blackboard, pathContext);
        }
        
        public AIContext withPathContext(PathfindingContext ctx) {
            return new AIContext(entity, position, target, blackboard, ctx);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            return (T) blackboard.get(key);
        }
        
        public <T> T get(String key, T defaultValue) {
            Object val = blackboard.get(key);
            return val != null ? (T) val : defaultValue;
        }
        
        public void set(String key, Object value) {
            blackboard.put(key, value);
        }
    }
    
    public static class BehaviorTree {
        private final String id;
        private final BehaviorNode root;
        
        private BehaviorTree(String id, BehaviorNode root) {
            this.id = id;
            this.root = root;
        }
        
        public String getId() { return id; }
        
        public NodeStatus tick(AIContext context) {
            return root.tick(context);
        }
        
        public void reset() {
            root.reset();
        }
        
        public static class Builder {
            private final String id;
            private BehaviorNode root;
            
            public Builder(String id) { this.id = id; }
            
            public Builder root(BehaviorNode node) { this.root = node; return this; }
            
            public BehaviorTree build() {
                if (root == null) {
                    throw new IllegalStateException("Behavior tree must have a root node");
                }
                return new BehaviorTree(id, root);
            }
        }
    }
    
    public static class GoalSelector {
        private final String id;
        private final List<PrioritizedGoal> goals = new ArrayList<>();
        private Goal activeGoal;
        
        public GoalSelector(String id) {
            this.id = id;
        }
        
        public String getId() { return id; }
        
        public void addGoal(int priority, Goal goal) {
            goals.add(new PrioritizedGoal(priority, goal));
            goals.sort(Comparator.comparingInt(PrioritizedGoal::priority));
        }
        
        public void removeGoal(Goal goal) {
            goals.removeIf(pg -> pg.goal() == goal);
            if (activeGoal == goal) {
                activeGoal.stop();
                activeGoal = null;
            }
        }
        
        public void tick(AIContext context) {
            Goal selected = null;
            
            for (PrioritizedGoal pg : goals) {
                if (pg.goal().canStart(context)) {
                    selected = pg.goal();
                    break;
                }
            }
            
            if (selected != activeGoal) {
                if (activeGoal != null) {
                    activeGoal.stop();
                }
                activeGoal = selected;
                if (activeGoal != null) {
                    activeGoal.start();
                }
            }
            
            if (activeGoal != null) {
                if (activeGoal.canContinue(context)) {
                    activeGoal.tick(context);
                } else {
                    activeGoal.stop();
                    activeGoal = null;
                }
            }
        }
        
        public Optional<Goal> getActiveGoal() {
            return Optional.ofNullable(activeGoal);
        }
        
        private record PrioritizedGoal(int priority, Goal goal) {}
    }
    
    public interface Goal {
        boolean canStart(AIContext context);
        default boolean canContinue(AIContext context) { return canStart(context); }
        default void start() {}
        void tick(AIContext context);
        default void stop() {}
    }
    
    private static class SequenceNode implements BehaviorNode {
        private final List<BehaviorNode> children;
        private int currentIndex = 0;
        
        public SequenceNode(List<BehaviorNode> children) {
            this.children = children;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            while (currentIndex < children.size()) {
                NodeStatus status = children.get(currentIndex).tick(context);
                if (status == NodeStatus.RUNNING) return NodeStatus.RUNNING;
                if (status == NodeStatus.FAILURE) {
                    reset();
                    return NodeStatus.FAILURE;
                }
                currentIndex++;
            }
            reset();
            return NodeStatus.SUCCESS;
        }
        
        @Override
        public void reset() {
            currentIndex = 0;
            children.forEach(BehaviorNode::reset);
        }
    }
    
    private static class SelectorNode implements BehaviorNode {
        private final List<BehaviorNode> children;
        private int currentIndex = 0;
        
        public SelectorNode(List<BehaviorNode> children) {
            this.children = children;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            while (currentIndex < children.size()) {
                NodeStatus status = children.get(currentIndex).tick(context);
                if (status == NodeStatus.RUNNING) return NodeStatus.RUNNING;
                if (status == NodeStatus.SUCCESS) {
                    reset();
                    return NodeStatus.SUCCESS;
                }
                currentIndex++;
            }
            reset();
            return NodeStatus.FAILURE;
        }
        
        @Override
        public void reset() {
            currentIndex = 0;
            children.forEach(BehaviorNode::reset);
        }
    }
    
    private static class ParallelNode implements BehaviorNode {
        private final List<BehaviorNode> children;
        
        public ParallelNode(List<BehaviorNode> children) {
            this.children = children;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            boolean anyRunning = false;
            boolean anyFailed = false;
            
            for (BehaviorNode child : children) {
                NodeStatus status = child.tick(context);
                if (status == NodeStatus.RUNNING) anyRunning = true;
                if (status == NodeStatus.FAILURE) anyFailed = true;
            }
            
            if (anyFailed) return NodeStatus.FAILURE;
            if (anyRunning) return NodeStatus.RUNNING;
            return NodeStatus.SUCCESS;
        }
        
        @Override
        public void reset() {
            children.forEach(BehaviorNode::reset);
        }
    }
    
    private static class ConditionalNode implements BehaviorNode {
        private final Predicate<AIContext> condition;
        private final BehaviorNode child;
        
        public ConditionalNode(Predicate<AIContext> condition, BehaviorNode child) {
            this.condition = condition;
            this.child = child;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            if (condition.test(context)) {
                return child.tick(context);
            }
            return NodeStatus.FAILURE;
        }
        
        @Override
        public void reset() { child.reset(); }
    }
    
    private static class InverterNode implements BehaviorNode {
        private final BehaviorNode child;
        
        public InverterNode(BehaviorNode child) {
            this.child = child;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            NodeStatus status = child.tick(context);
            return switch (status) {
                case SUCCESS -> NodeStatus.FAILURE;
                case FAILURE -> NodeStatus.SUCCESS;
                case RUNNING -> NodeStatus.RUNNING;
            };
        }
        
        @Override
        public void reset() { child.reset(); }
    }
    
    private static class RepeaterNode implements BehaviorNode {
        private final BehaviorNode child;
        private final int times;
        private int count = 0;
        
        public RepeaterNode(BehaviorNode child, int times) {
            this.child = child;
            this.times = times;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            while (count < times) {
                NodeStatus status = child.tick(context);
                if (status == NodeStatus.RUNNING) return NodeStatus.RUNNING;
                child.reset();
                count++;
            }
            reset();
            return NodeStatus.SUCCESS;
        }
        
        @Override
        public void reset() { count = 0; child.reset(); }
    }
    
    private static class ActionNode implements BehaviorNode {
        private final Consumer<AIContext> action;
        
        public ActionNode(Consumer<AIContext> action) {
            this.action = action;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            action.accept(context);
            return NodeStatus.SUCCESS;
        }
    }
    
    private static class WaitNode implements BehaviorNode {
        private final int ticks;
        private int remaining;
        
        public WaitNode(int ticks) {
            this.ticks = ticks;
            this.remaining = ticks;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            if (remaining > 0) {
                remaining--;
                return NodeStatus.RUNNING;
            }
            reset();
            return NodeStatus.SUCCESS;
        }
        
        @Override
        public void reset() { remaining = ticks; }
    }
    
    private static class MoveToNode implements BehaviorNode {
        private final Vec3i target;
        private PathResult path;
        private int pathIndex = 0;
        
        public MoveToNode(Vec3i target) {
            this.target = target;
        }
        
        @Override
        public NodeStatus tick(AIContext context) {
            if (context.position().equals(target)) {
                return NodeStatus.SUCCESS;
            }
            
            if (path == null && context.pathContext() != null) {
                path = PathfindingAPI.findPath(context.position(), target, context.pathContext());
            }
            
            if (path == null || !path.success()) {
                return NodeStatus.FAILURE;
            }
            
            return NodeStatus.RUNNING;
        }
        
        @Override
        public void reset() { path = null; pathIndex = 0; }
    }
    
    private static class MoveToTargetNode implements BehaviorNode {
        @Override
        public NodeStatus tick(AIContext context) {
            if (context.target() == null) return NodeStatus.FAILURE;
            return NodeStatus.RUNNING;
        }
    }
    
    private static class LookAtNode implements BehaviorNode {
        private final Vec3i target;
        
        public LookAtNode(Vec3i target) { this.target = target; }
        
        @Override
        public NodeStatus tick(AIContext context) {
            return NodeStatus.SUCCESS;
        }
    }
    
    private static class LookAtTargetNode implements BehaviorNode {
        @Override
        public NodeStatus tick(AIContext context) {
            if (context.target() == null) return NodeStatus.FAILURE;
            return NodeStatus.SUCCESS;
        }
    }
    
    private static class AttackNode implements BehaviorNode {
        @Override
        public NodeStatus tick(AIContext context) {
            if (context.target() == null) return NodeStatus.FAILURE;
            return NodeStatus.SUCCESS;
        }
    }
    
    private static class FleeNode implements BehaviorNode {
        private final double distance;
        
        public FleeNode(double distance) { this.distance = distance; }
        
        @Override
        public NodeStatus tick(AIContext context) {
            return NodeStatus.RUNNING;
        }
    }
    
    private static class WanderNode implements BehaviorNode {
        private final int radius;
        private final Random random = new Random();
        
        public WanderNode(int radius) { this.radius = radius; }
        
        @Override
        public NodeStatus tick(AIContext context) {
            return NodeStatus.RUNNING;
        }
    }
    
    private static class PatrolNode implements BehaviorNode {
        private final List<Vec3i> points;
        private int currentPoint = 0;
        
        public PatrolNode(List<Vec3i> points) { this.points = new ArrayList<>(points); }
        
        @Override
        public NodeStatus tick(AIContext context) {
            if (points.isEmpty()) return NodeStatus.FAILURE;
            
            Vec3i target = points.get(currentPoint);
            if (context.position().equals(target)) {
                currentPoint = (currentPoint + 1) % points.size();
            }
            
            return NodeStatus.RUNNING;
        }
        
        @Override
        public void reset() { currentPoint = 0; }
    }
}
