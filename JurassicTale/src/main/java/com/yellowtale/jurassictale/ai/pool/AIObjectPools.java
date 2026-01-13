package com.yellowtale.jurassictale.ai.pool;

import com.yellowtale.jurassictale.ai.perception.PerceptionData;
import com.yellowtale.jurassictale.ai.steering.SteeringPipeline;

import java.util.*;

public class AIObjectPools {
    
    private static final int DEFAULT_POOL_SIZE = 64;
    
    private final ObjectPool<PerceptionData> perceptionPool;
    private final ObjectPool<List<Object>> listPool;
    private final ObjectPool<SteeringPipeline> pipelinePool;
    
    public AIObjectPools() {
        this.perceptionPool = ObjectPool.create(
            PerceptionData::new,
            PerceptionData::clear,
            DEFAULT_POOL_SIZE
        );
        
        this.listPool = ObjectPool.create(
            ArrayList::new,
            List::clear,
            DEFAULT_POOL_SIZE * 2
        );
        
        this.pipelinePool = ObjectPool.create(
            () -> new SteeringPipeline(10.0f),
            SteeringPipeline::clear,
            DEFAULT_POOL_SIZE
        );
    }
    
    public PerceptionData acquirePerception() {
        return perceptionPool.acquire();
    }
    
    public void releasePerception(PerceptionData data) {
        perceptionPool.release(data);
    }
    
    @SuppressWarnings("unchecked")
    public <T> List<T> acquireList() {
        return (List<T>) listPool.acquire();
    }
    
    public void releaseList(List<?> list) {
        listPool.release((List<Object>) list);
    }
    
    public SteeringPipeline acquirePipeline() {
        return pipelinePool.acquire();
    }
    
    public void releasePipeline(SteeringPipeline pipeline) {
        pipelinePool.release(pipeline);
    }
    
    public PoolStats getStats() {
        return new PoolStats(
            perceptionPool.getAvailable(),
            perceptionPool.getCreated(),
            listPool.getAvailable(),
            listPool.getCreated(),
            pipelinePool.getAvailable(),
            pipelinePool.getCreated()
        );
    }
    
    public record PoolStats(
        int perceptionAvailable, int perceptionCreated,
        int listAvailable, int listCreated,
        int pipelineAvailable, int pipelineCreated
    ) {}
}
