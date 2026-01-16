package rubidium.api.world;

import rubidium.api.pathfinding.PathfindingAPI.Vec3i;

import java.util.*;
import java.util.function.Predicate;

public final class WorldQueryAPI {
    
    private WorldQueryAPI() {}
    
    public static boolean hasLineOfSight(Vec3i from, Vec3i to, Predicate<Vec3i> isTransparent) {
        int dx = Math.abs(to.x() - from.x());
        int dy = Math.abs(to.y() - from.y());
        int dz = Math.abs(to.z() - from.z());
        
        int sx = from.x() < to.x() ? 1 : -1;
        int sy = from.y() < to.y() ? 1 : -1;
        int sz = from.z() < to.z() ? 1 : -1;
        
        int steps = Math.max(dx, Math.max(dy, dz));
        if (steps == 0) return true;
        
        double stepX = (double)(to.x() - from.x()) / steps;
        double stepY = (double)(to.y() - from.y()) / steps;
        double stepZ = (double)(to.z() - from.z()) / steps;
        
        double x = from.x() + 0.5;
        double y = from.y() + 0.5;
        double z = from.z() + 0.5;
        
        for (int i = 0; i <= steps; i++) {
            Vec3i current = Vec3i.of((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
            if (!current.equals(from) && !current.equals(to) && !isTransparent.test(current)) {
                return false;
            }
            x += stepX;
            y += stepY;
            z += stepZ;
        }
        
        return true;
    }
    
    public static List<Vec3i> raycast(Vec3i from, Vec3i direction, int maxDistance, Predicate<Vec3i> hitTest) {
        List<Vec3i> hits = new ArrayList<>();
        
        double dx = direction.x();
        double dy = direction.y();
        double dz = direction.z();
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return hits;
        
        dx /= len; dy /= len; dz /= len;
        
        double x = from.x() + 0.5;
        double y = from.y() + 0.5;
        double z = from.z() + 0.5;
        
        for (int i = 0; i < maxDistance; i++) {
            Vec3i current = Vec3i.of((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
            if (hitTest.test(current)) {
                hits.add(current);
            }
            x += dx; y += dy; z += dz;
        }
        
        return hits;
    }
    
    public static Optional<Vec3i> raycastFirst(Vec3i from, Vec3i direction, int maxDistance, Predicate<Vec3i> hitTest) {
        double dx = direction.x();
        double dy = direction.y();
        double dz = direction.z();
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return Optional.empty();
        
        dx /= len; dy /= len; dz /= len;
        
        double x = from.x() + 0.5;
        double y = from.y() + 0.5;
        double z = from.z() + 0.5;
        
        for (int i = 0; i < maxDistance; i++) {
            Vec3i current = Vec3i.of((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
            if (hitTest.test(current)) {
                return Optional.of(current);
            }
            x += dx; y += dy; z += dz;
        }
        
        return Optional.empty();
    }
    
    public static <T> Optional<T> findNearest(Vec3i center, double radius, Collection<T> entities, java.util.function.Function<T, Vec3i> positionGetter) {
        return entities.stream()
            .filter(e -> positionGetter.apply(e).distanceTo(center) <= radius)
            .min(Comparator.comparingDouble(e -> positionGetter.apply(e).distanceTo(center)));
    }
    
    public static <T> List<T> findInRadius(Vec3i center, double radius, Collection<T> entities, java.util.function.Function<T, Vec3i> positionGetter) {
        return entities.stream()
            .filter(e -> positionGetter.apply(e).distanceTo(center) <= radius)
            .sorted(Comparator.comparingDouble(e -> positionGetter.apply(e).distanceTo(center)))
            .toList();
    }
    
    public static <T> List<T> findInBox(Vec3i min, Vec3i max, Collection<T> entities, java.util.function.Function<T, Vec3i> positionGetter) {
        return entities.stream()
            .filter(e -> {
                Vec3i pos = positionGetter.apply(e);
                return pos.x() >= min.x() && pos.x() <= max.x()
                    && pos.y() >= min.y() && pos.y() <= max.y()
                    && pos.z() >= min.z() && pos.z() <= max.z();
            })
            .toList();
    }
    
    public static Optional<Vec3i> findSafeLocation(Vec3i center, int radius, Predicate<Vec3i> isSolid, Predicate<Vec3i> isAir) {
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = -r; y <= r; y++) {
                        Vec3i pos = center.add(x, y, z);
                        Vec3i below = pos.add(0, -1, 0);
                        Vec3i above = pos.add(0, 1, 0);
                        
                        if (isSolid.test(below) && isAir.test(pos) && isAir.test(above)) {
                            return Optional.of(pos);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
    
    public static Optional<Vec3i> findHighestBlock(int x, int z, int minY, int maxY, Predicate<Vec3i> isSolid) {
        for (int y = maxY; y >= minY; y--) {
            Vec3i pos = Vec3i.of(x, y, z);
            if (isSolid.test(pos)) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }
    
    public static List<Vec3i> getSphere(Vec3i center, int radius) {
        List<Vec3i> positions = new ArrayList<>();
        int r2 = radius * radius;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + y*y + z*z <= r2) {
                        positions.add(center.add(x, y, z));
                    }
                }
            }
        }
        
        return positions;
    }
    
    public static List<Vec3i> getHollowSphere(Vec3i center, int radius) {
        List<Vec3i> positions = new ArrayList<>();
        int r2 = radius * radius;
        int innerR2 = (radius - 1) * (radius - 1);
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int d2 = x*x + y*y + z*z;
                    if (d2 <= r2 && d2 >= innerR2) {
                        positions.add(center.add(x, y, z));
                    }
                }
            }
        }
        
        return positions;
    }
    
    public static List<Vec3i> getCube(Vec3i center, int halfSize) {
        List<Vec3i> positions = new ArrayList<>();
        
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int y = -halfSize; y <= halfSize; y++) {
                for (int z = -halfSize; z <= halfSize; z++) {
                    positions.add(center.add(x, y, z));
                }
            }
        }
        
        return positions;
    }
    
    public static List<Vec3i> getCylinder(Vec3i center, int radius, int height) {
        List<Vec3i> positions = new ArrayList<>();
        int r2 = radius * radius;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= r2) {
                    for (int y = 0; y < height; y++) {
                        positions.add(center.add(x, y, z));
                    }
                }
            }
        }
        
        return positions;
    }
    
    public static List<Vec3i> getLine(Vec3i from, Vec3i to) {
        List<Vec3i> positions = new ArrayList<>();
        
        int dx = Math.abs(to.x() - from.x());
        int dy = Math.abs(to.y() - from.y());
        int dz = Math.abs(to.z() - from.z());
        
        int steps = Math.max(dx, Math.max(dy, dz));
        if (steps == 0) {
            positions.add(from);
            return positions;
        }
        
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(from.x() + t * (to.x() - from.x()));
            int y = (int) Math.round(from.y() + t * (to.y() - from.y()));
            int z = (int) Math.round(from.z() + t * (to.z() - from.z()));
            positions.add(Vec3i.of(x, y, z));
        }
        
        return positions;
    }
    
    public record AABB(Vec3i min, Vec3i max) {
        public static AABB of(Vec3i a, Vec3i b) {
            return new AABB(
                Vec3i.of(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                Vec3i.of(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()))
            );
        }
        
        public boolean contains(Vec3i pos) {
            return pos.x() >= min.x() && pos.x() <= max.x()
                && pos.y() >= min.y() && pos.y() <= max.y()
                && pos.z() >= min.z() && pos.z() <= max.z();
        }
        
        public boolean intersects(AABB other) {
            return min.x() <= other.max.x() && max.x() >= other.min.x()
                && min.y() <= other.max.y() && max.y() >= other.min.y()
                && min.z() <= other.max.z() && max.z() >= other.min.z();
        }
        
        public Vec3i getCenter() {
            return Vec3i.of(
                (min.x() + max.x()) / 2,
                (min.y() + max.y()) / 2,
                (min.z() + max.z()) / 2
            );
        }
        
        public Vec3i getSize() {
            return Vec3i.of(
                max.x() - min.x() + 1,
                max.y() - min.y() + 1,
                max.z() - min.z() + 1
            );
        }
        
        public AABB expand(int amount) {
            return new AABB(
                min.add(-amount, -amount, -amount),
                max.add(amount, amount, amount)
            );
        }
    }
}
