package com.yellowtale.jurassictale.ai.spatial;

import java.util.*;
import java.util.function.Predicate;

public class SpatialHashGrid<T extends SpatialEntity> {
    
    private final float cellSize;
    private final float inverseCellSize;
    private final Map<Long, List<T>> cells;
    private final Map<UUID, Long> entityCells;
    
    public SpatialHashGrid(float cellSize) {
        this.cellSize = cellSize;
        this.inverseCellSize = 1.0f / cellSize;
        this.cells = new HashMap<>();
        this.entityCells = new HashMap<>();
    }
    
    public void insert(T entity) {
        long cellKey = getCellKey(entity.getX(), entity.getZ());
        cells.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(entity);
        entityCells.put(entity.getId(), cellKey);
    }
    
    public void remove(T entity) {
        Long oldKey = entityCells.remove(entity.getId());
        if (oldKey != null) {
            List<T> cell = cells.get(oldKey);
            if (cell != null) {
                cell.remove(entity);
                if (cell.isEmpty()) {
                    cells.remove(oldKey);
                }
            }
        }
    }
    
    public void update(T entity) {
        long newKey = getCellKey(entity.getX(), entity.getZ());
        Long oldKey = entityCells.get(entity.getId());
        
        if (oldKey == null || oldKey != newKey) {
            remove(entity);
            insert(entity);
        }
    }
    
    public List<T> queryRadius(double centerX, double centerZ, double radius) {
        return queryRadius(centerX, centerZ, radius, e -> true);
    }
    
    public List<T> queryRadius(double centerX, double centerZ, double radius, Predicate<T> filter) {
        List<T> results = new ArrayList<>();
        double radiusSq = radius * radius;
        
        int minCellX = (int) Math.floor((centerX - radius) * inverseCellSize);
        int maxCellX = (int) Math.floor((centerX + radius) * inverseCellSize);
        int minCellZ = (int) Math.floor((centerZ - radius) * inverseCellSize);
        int maxCellZ = (int) Math.floor((centerZ + radius) * inverseCellSize);
        
        for (int cx = minCellX; cx <= maxCellX; cx++) {
            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                List<T> cell = cells.get(cellKey(cx, cz));
                if (cell != null) {
                    for (T entity : cell) {
                        double dx = entity.getX() - centerX;
                        double dz = entity.getZ() - centerZ;
                        if (dx * dx + dz * dz <= radiusSq && filter.test(entity)) {
                            results.add(entity);
                        }
                    }
                }
            }
        }
        return results;
    }
    
    public Optional<T> findNearest(double x, double z, double maxRadius, Predicate<T> filter) {
        List<T> candidates = queryRadius(x, z, maxRadius, filter);
        if (candidates.isEmpty()) return Optional.empty();
        
        T nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        
        for (T entity : candidates) {
            double dx = entity.getX() - x;
            double dz = entity.getZ() - z;
            double distSq = dx * dx + dz * dz;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = entity;
            }
        }
        return Optional.ofNullable(nearest);
    }
    
    public int getEntityCount() {
        return entityCells.size();
    }
    
    public void clear() {
        cells.clear();
        entityCells.clear();
    }
    
    private long getCellKey(double x, double z) {
        int cx = (int) Math.floor(x * inverseCellSize);
        int cz = (int) Math.floor(z * inverseCellSize);
        return cellKey(cx, cz);
    }
    
    private long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
