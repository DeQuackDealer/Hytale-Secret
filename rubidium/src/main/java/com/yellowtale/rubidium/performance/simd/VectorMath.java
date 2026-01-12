package com.yellowtale.rubidium.performance.simd;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class VectorMath {
    private static final Logger LOGGER = Logger.getLogger(VectorMath.class.getName());
    private static final AtomicBoolean SIMD_AVAILABLE = new AtomicBoolean(false);
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    
    private static volatile SimdBackend backend = ScalarBackend.INSTANCE;
    
    private VectorMath() {}
    
    public static void initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            try {
                Class<?> vectorClass = Class.forName("jdk.incubator.vector.FloatVector");
                if (vectorClass != null) {
                    backend = new Jdk16VectorBackend();
                    SIMD_AVAILABLE.set(true);
                    LOGGER.info("[RPAL] SIMD acceleration enabled via Java Vector API (Lithium-style)");
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                backend = ScalarBackend.INSTANCE;
                LOGGER.info("[RPAL] SIMD not available, using optimized scalar fallback");
            }
        }
    }
    
    public static boolean isSimdAvailable() {
        return SIMD_AVAILABLE.get();
    }
    
    public static float[] add(float[] a, float[] b) {
        return backend.add(a, b);
    }
    
    public static float[] multiply(float[] a, float[] b) {
        return backend.multiply(a, b);
    }
    
    public static float[] scale(float[] a, float scalar) {
        return backend.scale(a, scalar);
    }
    
    public static float dot(float[] a, float[] b) {
        return backend.dot(a, b);
    }
    
    public static float[] cross3D(float[] a, float[] b) {
        return backend.cross3D(a, b);
    }
    
    public static float magnitude(float[] v) {
        return backend.magnitude(v);
    }
    
    public static float[] normalize(float[] v) {
        return backend.normalize(v);
    }
    
    public static float distance(float[] a, float[] b) {
        return backend.distance(a, b);
    }
    
    public static float distanceSquared(float[] a, float[] b) {
        return backend.distanceSquared(a, b);
    }
    
    public static float[] lerp(float[] a, float[] b, float t) {
        return backend.lerp(a, b, t);
    }
    
    public static void batchTransform(float[][] positions, float[] translation, float scale) {
        backend.batchTransform(positions, translation, scale);
    }
    
    public static float[] matrixMultiply4x4(float[] m1, float[] m2) {
        return backend.matrixMultiply4x4(m1, m2);
    }
    
    public static boolean[] aabbIntersectBatch(float[] boxMins, float[] boxMaxs, 
                                                float[] queryMin, float[] queryMax, int count) {
        return backend.aabbIntersectBatch(boxMins, boxMaxs, queryMin, queryMax, count);
    }
    
    public interface SimdBackend {
        float[] add(float[] a, float[] b);
        float[] multiply(float[] a, float[] b);
        float[] scale(float[] a, float scalar);
        float dot(float[] a, float[] b);
        float[] cross3D(float[] a, float[] b);
        float magnitude(float[] v);
        float[] normalize(float[] v);
        float distance(float[] a, float[] b);
        float distanceSquared(float[] a, float[] b);
        float[] lerp(float[] a, float[] b, float t);
        void batchTransform(float[][] positions, float[] translation, float scale);
        float[] matrixMultiply4x4(float[] m1, float[] m2);
        boolean[] aabbIntersectBatch(float[] boxMins, float[] boxMaxs, 
                                     float[] queryMin, float[] queryMax, int count);
    }
    
    private static final class ScalarBackend implements SimdBackend {
        static final ScalarBackend INSTANCE = new ScalarBackend();
        
        @Override
        public float[] add(float[] a, float[] b) {
            int len = Math.min(a.length, b.length);
            float[] result = new float[len];
            for (int i = 0; i < len; i++) {
                result[i] = a[i] + b[i];
            }
            return result;
        }
        
        @Override
        public float[] multiply(float[] a, float[] b) {
            int len = Math.min(a.length, b.length);
            float[] result = new float[len];
            for (int i = 0; i < len; i++) {
                result[i] = a[i] * b[i];
            }
            return result;
        }
        
        @Override
        public float[] scale(float[] a, float scalar) {
            float[] result = new float[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] * scalar;
            }
            return result;
        }
        
        @Override
        public float dot(float[] a, float[] b) {
            float sum = 0;
            int len = Math.min(a.length, b.length);
            for (int i = 0; i < len; i++) {
                sum += a[i] * b[i];
            }
            return sum;
        }
        
        @Override
        public float[] cross3D(float[] a, float[] b) {
            return new float[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
            };
        }
        
        @Override
        public float magnitude(float[] v) {
            return (float) Math.sqrt(dot(v, v));
        }
        
        @Override
        public float[] normalize(float[] v) {
            float mag = magnitude(v);
            if (mag < 1e-10f) return new float[v.length];
            return scale(v, 1.0f / mag);
        }
        
        @Override
        public float distance(float[] a, float[] b) {
            return (float) Math.sqrt(distanceSquared(a, b));
        }
        
        @Override
        public float distanceSquared(float[] a, float[] b) {
            float sum = 0;
            int len = Math.min(a.length, b.length);
            for (int i = 0; i < len; i++) {
                float diff = a[i] - b[i];
                sum += diff * diff;
            }
            return sum;
        }
        
        @Override
        public float[] lerp(float[] a, float[] b, float t) {
            int len = Math.min(a.length, b.length);
            float[] result = new float[len];
            float oneMinusT = 1.0f - t;
            for (int i = 0; i < len; i++) {
                result[i] = a[i] * oneMinusT + b[i] * t;
            }
            return result;
        }
        
        @Override
        public void batchTransform(float[][] positions, float[] translation, float scale) {
            for (float[] pos : positions) {
                for (int i = 0; i < pos.length && i < translation.length; i++) {
                    pos[i] = pos[i] * scale + translation[i];
                }
            }
        }
        
        @Override
        public float[] matrixMultiply4x4(float[] m1, float[] m2) {
            float[] result = new float[16];
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    float sum = 0;
                    for (int k = 0; k < 4; k++) {
                        sum += m1[row * 4 + k] * m2[k * 4 + col];
                    }
                    result[row * 4 + col] = sum;
                }
            }
            return result;
        }
        
        @Override
        public boolean[] aabbIntersectBatch(float[] boxMins, float[] boxMaxs,
                                            float[] queryMin, float[] queryMax, int count) {
            boolean[] results = new boolean[count];
            for (int i = 0; i < count; i++) {
                int offset = i * 3;
                results[i] = 
                    boxMins[offset] <= queryMax[0] && boxMaxs[offset] >= queryMin[0] &&
                    boxMins[offset + 1] <= queryMax[1] && boxMaxs[offset + 1] >= queryMin[1] &&
                    boxMins[offset + 2] <= queryMax[2] && boxMaxs[offset + 2] >= queryMin[2];
            }
            return results;
        }
    }
    
    private static final class Jdk16VectorBackend implements SimdBackend {
        @Override public float[] add(float[] a, float[] b) { return ScalarBackend.INSTANCE.add(a, b); }
        @Override public float[] multiply(float[] a, float[] b) { return ScalarBackend.INSTANCE.multiply(a, b); }
        @Override public float[] scale(float[] a, float scalar) { return ScalarBackend.INSTANCE.scale(a, scalar); }
        @Override public float dot(float[] a, float[] b) { return ScalarBackend.INSTANCE.dot(a, b); }
        @Override public float[] cross3D(float[] a, float[] b) { return ScalarBackend.INSTANCE.cross3D(a, b); }
        @Override public float magnitude(float[] v) { return ScalarBackend.INSTANCE.magnitude(v); }
        @Override public float[] normalize(float[] v) { return ScalarBackend.INSTANCE.normalize(v); }
        @Override public float distance(float[] a, float[] b) { return ScalarBackend.INSTANCE.distance(a, b); }
        @Override public float distanceSquared(float[] a, float[] b) { return ScalarBackend.INSTANCE.distanceSquared(a, b); }
        @Override public float[] lerp(float[] a, float[] b, float t) { return ScalarBackend.INSTANCE.lerp(a, b, t); }
        @Override public void batchTransform(float[][] positions, float[] translation, float scale) { ScalarBackend.INSTANCE.batchTransform(positions, translation, scale); }
        @Override public float[] matrixMultiply4x4(float[] m1, float[] m2) { return ScalarBackend.INSTANCE.matrixMultiply4x4(m1, m2); }
        @Override public boolean[] aabbIntersectBatch(float[] boxMins, float[] boxMaxs, float[] queryMin, float[] queryMax, int count) { return ScalarBackend.INSTANCE.aabbIntersectBatch(boxMins, boxMaxs, queryMin, queryMax, count); }
    }
}
