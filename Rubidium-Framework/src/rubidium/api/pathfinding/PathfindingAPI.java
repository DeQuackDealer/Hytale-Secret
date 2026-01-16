package rubidium.api.pathfinding;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PathfindingAPI {
    
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Map<String, PathfindingProfile> profiles = new ConcurrentHashMap<>();
    
    private PathfindingAPI() {}
    
    public static PathResult findPath(Vec3i start, Vec3i goal, PathfindingContext context) {
        return findPath(start, goal, context, PathfindingOptions.DEFAULT);
    }
    
    public static PathResult findPath(Vec3i start, Vec3i goal, PathfindingContext context, PathfindingOptions options) {
        AStar astar = new AStar(context, options);
        return astar.findPath(start, goal);
    }
    
    public static CompletableFuture<PathResult> findPathAsync(Vec3i start, Vec3i goal, PathfindingContext context) {
        return findPathAsync(start, goal, context, PathfindingOptions.DEFAULT);
    }
    
    public static CompletableFuture<PathResult> findPathAsync(Vec3i start, Vec3i goal, PathfindingContext context, PathfindingOptions options) {
        return CompletableFuture.supplyAsync(() -> findPath(start, goal, context, options), executor);
    }
    
    public static PathfindingProfile.Builder createProfile(String id) {
        return new PathfindingProfile.Builder(id);
    }
    
    public static void registerProfile(PathfindingProfile profile) {
        profiles.put(profile.id(), profile);
    }
    
    public static Optional<PathfindingProfile> getProfile(String id) {
        return Optional.ofNullable(profiles.get(id));
    }
    
    public static PathfindingContext createContext(Predicate<Vec3i> passable) {
        return new PathfindingContext(passable, (from, to) -> 1.0);
    }
    
    public static PathfindingContext createContext(Predicate<Vec3i> passable, BiFunction<Vec3i, Vec3i, Double> costFunction) {
        return new PathfindingContext(passable, costFunction);
    }
    
    public static NavigationGrid createGrid(int width, int height, int depth) {
        return new NavigationGrid(width, height, depth);
    }
    
    public static double distance(Vec3i a, Vec3i b) {
        int dx = a.x() - b.x();
        int dy = a.y() - b.y();
        int dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public static double manhattanDistance(Vec3i a, Vec3i b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()) + Math.abs(a.z() - b.z());
    }
    
    public record Vec3i(int x, int y, int z) {
        public Vec3i add(int dx, int dy, int dz) {
            return new Vec3i(x + dx, y + dy, z + dz);
        }
        
        public Vec3i add(Vec3i other) {
            return new Vec3i(x + other.x, y + other.y, z + other.z);
        }
        
        public double distanceTo(Vec3i other) {
            return distance(this, other);
        }
        
        public static Vec3i of(int x, int y, int z) {
            return new Vec3i(x, y, z);
        }
        
        public static Vec3i ZERO = new Vec3i(0, 0, 0);
    }
    
    public record PathResult(
        List<Vec3i> path,
        double totalCost,
        int nodesExplored,
        boolean success,
        long computeTimeMs
    ) {
        public static PathResult failure(int nodesExplored, long computeTimeMs) {
            return new PathResult(List.of(), 0, nodesExplored, false, computeTimeMs);
        }
        
        public static PathResult success(List<Vec3i> path, double cost, int nodesExplored, long computeTimeMs) {
            return new PathResult(path, cost, nodesExplored, true, computeTimeMs);
        }
        
        public boolean isEmpty() {
            return path.isEmpty();
        }
        
        public int length() {
            return path.size();
        }
        
        public Optional<Vec3i> getFirst() {
            return path.isEmpty() ? Optional.empty() : Optional.of(path.get(0));
        }
        
        public Optional<Vec3i> getLast() {
            return path.isEmpty() ? Optional.empty() : Optional.of(path.get(path.size() - 1));
        }
        
        public Optional<Vec3i> getNext(int currentIndex) {
            int next = currentIndex + 1;
            return next < path.size() ? Optional.of(path.get(next)) : Optional.empty();
        }
    }
    
    public record PathfindingContext(
        Predicate<Vec3i> passable,
        BiFunction<Vec3i, Vec3i, Double> costFunction
    ) {
        public boolean isPassable(Vec3i pos) {
            return passable.test(pos);
        }
        
        public double getCost(Vec3i from, Vec3i to) {
            return costFunction.apply(from, to);
        }
    }
    
    public record PathfindingOptions(
        int maxIterations,
        boolean allowDiagonal,
        boolean allowVertical,
        int maxJumpHeight,
        int maxFallDistance,
        double heuristicWeight,
        Function<Vec3i, Double> customHeuristic
    ) {
        public static final PathfindingOptions DEFAULT = new PathfindingOptions(
            10000, true, true, 1, 3, 1.0, null
        );
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private int maxIterations = 10000;
            private boolean allowDiagonal = true;
            private boolean allowVertical = true;
            private int maxJumpHeight = 1;
            private int maxFallDistance = 3;
            private double heuristicWeight = 1.0;
            private Function<Vec3i, Double> customHeuristic = null;
            
            public Builder maxIterations(int max) { this.maxIterations = max; return this; }
            public Builder allowDiagonal(boolean allow) { this.allowDiagonal = allow; return this; }
            public Builder allowVertical(boolean allow) { this.allowVertical = allow; return this; }
            public Builder maxJumpHeight(int height) { this.maxJumpHeight = height; return this; }
            public Builder maxFallDistance(int dist) { this.maxFallDistance = dist; return this; }
            public Builder heuristicWeight(double weight) { this.heuristicWeight = weight; return this; }
            public Builder customHeuristic(Function<Vec3i, Double> h) { this.customHeuristic = h; return this; }
            
            public PathfindingOptions build() {
                return new PathfindingOptions(maxIterations, allowDiagonal, allowVertical, 
                    maxJumpHeight, maxFallDistance, heuristicWeight, customHeuristic);
            }
        }
    }
    
    public record PathfindingProfile(
        String id,
        double baseCost,
        double diagonalCostMultiplier,
        double verticalCostMultiplier,
        Set<String> allowedBlocks,
        Set<String> avoidBlocks,
        Map<String, Double> blockCosts,
        PathfindingOptions options
    ) {
        public static class Builder {
            private final String id;
            private double baseCost = 1.0;
            private double diagonalCostMultiplier = 1.414;
            private double verticalCostMultiplier = 1.5;
            private Set<String> allowedBlocks = new HashSet<>();
            private Set<String> avoidBlocks = new HashSet<>();
            private Map<String, Double> blockCosts = new HashMap<>();
            private PathfindingOptions options = PathfindingOptions.DEFAULT;
            
            public Builder(String id) { this.id = id; }
            
            public Builder baseCost(double cost) { this.baseCost = cost; return this; }
            public Builder diagonalMultiplier(double mult) { this.diagonalCostMultiplier = mult; return this; }
            public Builder verticalMultiplier(double mult) { this.verticalCostMultiplier = mult; return this; }
            public Builder allowBlock(String block) { this.allowedBlocks.add(block); return this; }
            public Builder avoidBlock(String block) { this.avoidBlocks.add(block); return this; }
            public Builder blockCost(String block, double cost) { this.blockCosts.put(block, cost); return this; }
            public Builder options(PathfindingOptions opts) { this.options = opts; return this; }
            
            public PathfindingProfile build() {
                return new PathfindingProfile(id, baseCost, diagonalCostMultiplier, verticalCostMultiplier,
                    Set.copyOf(allowedBlocks), Set.copyOf(avoidBlocks), Map.copyOf(blockCosts), options);
            }
        }
    }
    
    public static class NavigationGrid {
        private final int width, height, depth;
        private final boolean[][][] passable;
        private final double[][][] costs;
        
        public NavigationGrid(int width, int height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.passable = new boolean[width][height][depth];
            this.costs = new double[width][height][depth];
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < depth; z++) {
                        passable[x][y][z] = true;
                        costs[x][y][z] = 1.0;
                    }
                }
            }
        }
        
        public void setPassable(int x, int y, int z, boolean value) {
            if (inBounds(x, y, z)) passable[x][y][z] = value;
        }
        
        public void setCost(int x, int y, int z, double cost) {
            if (inBounds(x, y, z)) costs[x][y][z] = cost;
        }
        
        public boolean isPassable(int x, int y, int z) {
            return inBounds(x, y, z) && passable[x][y][z];
        }
        
        public boolean isPassable(Vec3i pos) {
            return isPassable(pos.x(), pos.y(), pos.z());
        }
        
        public double getCost(int x, int y, int z) {
            return inBounds(x, y, z) ? costs[x][y][z] : Double.MAX_VALUE;
        }
        
        public boolean inBounds(int x, int y, int z) {
            return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
        }
        
        public PathfindingContext toContext() {
            return new PathfindingContext(
                this::isPassable,
                (from, to) -> getCost(to.x(), to.y(), to.z())
            );
        }
        
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getDepth() { return depth; }
    }
    
    private static class AStar {
        private final PathfindingContext context;
        private final PathfindingOptions options;
        
        private static final int[][] CARDINAL = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
        private static final int[][] DIAGONAL = {{1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1}};
        private static final int[][] VERTICAL_UP = {{0,1,0},{1,1,0},{-1,1,0},{0,1,1},{0,1,-1}};
        private static final int[][] VERTICAL_DOWN = {{0,-1,0},{1,-1,0},{-1,-1,0},{0,-1,1},{0,-1,-1}};
        
        public AStar(PathfindingContext context, PathfindingOptions options) {
            this.context = context;
            this.options = options;
        }
        
        public PathResult findPath(Vec3i start, Vec3i goal) {
            long startTime = System.currentTimeMillis();
            
            if (!context.isPassable(start) || !context.isPassable(goal)) {
                return PathResult.failure(0, System.currentTimeMillis() - startTime);
            }
            
            PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
            Map<Vec3i, Node> allNodes = new HashMap<>();
            Set<Vec3i> closedSet = new HashSet<>();
            
            Node startNode = new Node(start, 0, heuristic(start, goal), null);
            openSet.add(startNode);
            allNodes.put(start, startNode);
            
            int iterations = 0;
            
            while (!openSet.isEmpty() && iterations < options.maxIterations()) {
                iterations++;
                Node current = openSet.poll();
                
                if (current.pos.equals(goal)) {
                    List<Vec3i> path = reconstructPath(current);
                    return PathResult.success(path, current.g, iterations, System.currentTimeMillis() - startTime);
                }
                
                closedSet.add(current.pos);
                
                for (Vec3i neighbor : getNeighbors(current.pos)) {
                    if (closedSet.contains(neighbor) || !context.isPassable(neighbor)) {
                        continue;
                    }
                    
                    double tentativeG = current.g + context.getCost(current.pos, neighbor);
                    Node neighborNode = allNodes.get(neighbor);
                    
                    if (neighborNode == null) {
                        neighborNode = new Node(neighbor, tentativeG, heuristic(neighbor, goal), current);
                        allNodes.put(neighbor, neighborNode);
                        openSet.add(neighborNode);
                    } else if (tentativeG < neighborNode.g) {
                        openSet.remove(neighborNode);
                        neighborNode.g = tentativeG;
                        neighborNode.f = tentativeG + heuristic(neighbor, goal);
                        neighborNode.parent = current;
                        openSet.add(neighborNode);
                    }
                }
            }
            
            return PathResult.failure(iterations, System.currentTimeMillis() - startTime);
        }
        
        private double heuristic(Vec3i from, Vec3i goal) {
            if (options.customHeuristic() != null) {
                return options.customHeuristic().apply(from) * options.heuristicWeight();
            }
            return distance(from, goal) * options.heuristicWeight();
        }
        
        private List<Vec3i> getNeighbors(Vec3i pos) {
            List<Vec3i> neighbors = new ArrayList<>();
            
            for (int[] dir : CARDINAL) {
                neighbors.add(pos.add(dir[0], dir[1], dir[2]));
            }
            
            if (options.allowDiagonal()) {
                for (int[] dir : DIAGONAL) {
                    neighbors.add(pos.add(dir[0], dir[1], dir[2]));
                }
            }
            
            if (options.allowVertical()) {
                for (int i = 1; i <= options.maxJumpHeight(); i++) {
                    neighbors.add(pos.add(0, i, 0));
                }
                for (int i = 1; i <= options.maxFallDistance(); i++) {
                    neighbors.add(pos.add(0, -i, 0));
                }
                for (int[] dir : VERTICAL_UP) {
                    neighbors.add(pos.add(dir[0], dir[1], dir[2]));
                }
                for (int[] dir : VERTICAL_DOWN) {
                    neighbors.add(pos.add(dir[0], dir[1], dir[2]));
                }
            }
            
            return neighbors;
        }
        
        private List<Vec3i> reconstructPath(Node node) {
            List<Vec3i> path = new ArrayList<>();
            while (node != null) {
                path.add(node.pos);
                node = node.parent;
            }
            Collections.reverse(path);
            return path;
        }
        
        private static class Node {
            final Vec3i pos;
            double g;
            double f;
            Node parent;
            
            Node(Vec3i pos, double g, double h, Node parent) {
                this.pos = pos;
                this.g = g;
                this.f = g + h;
                this.parent = parent;
            }
        }
    }
}
