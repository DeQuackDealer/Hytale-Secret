package com.yellowtale.rubidium.optimization;

public interface OptimizationContext {
    
    ServerPerformance getServerPerformance();
    
    TickOptimizer getTickOptimizer();
    
    MemoryManager getMemoryManager();
    
    EntityBudget getEntityBudget();
    
    ChunkOptimizer getChunkOptimizer();
    
    NetworkOptimizer getNetworkOptimizer();
    
    interface ServerPerformance {
        double getTPS();
        double getAverageTPS();
        long getTickTime();
        long getAverageTickTime();
        double getCPUUsage();
        long getMemoryUsed();
        long getMemoryMax();
        int getLoadedChunks();
        int getEntityCount();
        int getPlayerCount();
    }
    
    interface TickOptimizer {
        void requestExtraTick();
        void requestReducedLoad(int durationTicks);
        void setTickPriority(String task, TickPriority priority);
        boolean isOptimizationActive();
    }
    
    interface MemoryManager {
        void requestGC();
        void trimWorkingSets();
        long getAvailableMemory();
        void setMemoryPressureCallback(Runnable callback);
    }
    
    interface EntityBudget {
        int getEntityLimit();
        int getCurrentEntityCount();
        void setEntityLimit(int limit);
        boolean canSpawnEntity();
        void registerEntity(String type);
        void unregisterEntity(String type);
    }
    
    interface ChunkOptimizer {
        void preloadChunks(String world, int centerX, int centerZ, int radius);
        void unloadDistantChunks(String world);
        void setChunkTickDistance(int distance);
        int getChunkTickDistance();
    }
    
    interface NetworkOptimizer {
        void enablePacketBatching(boolean enabled);
        void setCompressionLevel(int level);
        void prioritizePlayer(String playerId);
        NetworkStats getNetworkStats();
    }
    
    interface NetworkStats {
        long getBytesSent();
        long getBytesReceived();
        int getPacketsSent();
        int getPacketsReceived();
        double getAverageLatency();
    }
    
    enum TickPriority {
        CRITICAL,
        HIGH,
        NORMAL,
        LOW,
        BACKGROUND
    }
}
