package rubidium.util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class MathUtil {
    
    private MathUtil() {}
    
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }
    
    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
    
    public static double inverseLerp(double start, double end, double value) {
        return (value - start) / (end - start);
    }
    
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    }
    
    public static double smoothStep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }
    
    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public static double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }
    
    public static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    public static double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
    
    public static float randomFloat(float min, float max) {
        return (float) (min + ThreadLocalRandom.current().nextFloat() * (max - min));
    }
    
    public static boolean chance(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }
    
    public static <T> T randomElement(List<T> list) {
        if (list.isEmpty()) return null;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
    
    public static <T> T randomElement(T[] array) {
        if (array.length == 0) return null;
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
    
    public static <T> T weightedRandom(Map<T, Double> weights) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double random = ThreadLocalRandom.current().nextDouble() * total;
        
        double cumulative = 0;
        for (Map.Entry<T, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                return entry.getKey();
            }
        }
        
        return weights.keySet().iterator().next();
    }
    
    public static double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
    }
    
    public static float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
    }
    
    public static double angleDifference(double angle1, double angle2) {
        double diff = normalizeAngle(angle2 - angle1);
        if (diff > 180) diff -= 360;
        return diff;
    }
    
    public static double[] directionToAngles(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        double pitch = Math.toDegrees(-Math.asin(y / length));
        double yaw = Math.toDegrees(Math.atan2(-x, z));
        return new double[]{yaw, pitch};
    }
    
    public static double[] anglesToDirection(double yaw, double pitch) {
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);
        double x = -Math.sin(radYaw) * Math.cos(radPitch);
        double y = -Math.sin(radPitch);
        double z = Math.cos(radYaw) * Math.cos(radPitch);
        return new double[]{x, y, z};
    }
    
    public static int floorDiv(int x, int y) {
        return Math.floorDiv(x, y);
    }
    
    public static int ceilDiv(int x, int y) {
        return -Math.floorDiv(-x, y);
    }
    
    public static int nextPowerOfTwo(int value) {
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }
    
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
}
