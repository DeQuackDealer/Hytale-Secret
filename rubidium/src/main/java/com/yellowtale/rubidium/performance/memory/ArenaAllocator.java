package com.yellowtale.rubidium.performance.memory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class ArenaAllocator implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ArenaAllocator.class.getName());
    
    private static final int DEFAULT_BLOCK_SIZE = 1024 * 1024;
    private static final int MAX_ALLOCATION_SIZE = 16 * 1024 * 1024;
    
    private final String name;
    private final int blockSize;
    private final boolean useDirect;
    private final List<ByteBuffer> blocks = new ArrayList<>();
    private ByteBuffer currentBlock;
    private int currentBlockIndex = -1;
    
    private final AtomicLong totalAllocated = new AtomicLong();
    private final AtomicLong totalRequested = new AtomicLong();
    private final AtomicLong allocationCount = new AtomicLong();
    
    public ArenaAllocator(String name) {
        this(name, DEFAULT_BLOCK_SIZE, true);
    }
    
    public ArenaAllocator(String name, int blockSize, boolean useDirect) {
        this.name = name;
        this.blockSize = Math.max(4096, blockSize);
        this.useDirect = useDirect;
        allocateNewBlock();
    }
    
    private synchronized void allocateNewBlock() {
        ByteBuffer newBlock = useDirect 
            ? ByteBuffer.allocateDirect(blockSize)
            : ByteBuffer.allocate(blockSize);
        blocks.add(newBlock);
        currentBlock = newBlock;
        currentBlockIndex++;
        totalAllocated.addAndGet(blockSize);
    }
    
    public synchronized ByteBuffer allocate(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        if (size > MAX_ALLOCATION_SIZE) {
            throw new IllegalArgumentException("Size exceeds maximum: " + MAX_ALLOCATION_SIZE);
        }
        
        allocationCount.incrementAndGet();
        totalRequested.addAndGet(size);
        
        int aligned = alignUp(size, 8);
        
        if (aligned > blockSize) {
            ByteBuffer largeBlock = useDirect 
                ? ByteBuffer.allocateDirect(aligned)
                : ByteBuffer.allocate(aligned);
            blocks.add(largeBlock);
            totalAllocated.addAndGet(aligned);
            return largeBlock;
        }
        
        if (currentBlock.remaining() < aligned) {
            allocateNewBlock();
        }
        
        int pos = currentBlock.position();
        currentBlock.position(pos + aligned);
        
        ByteBuffer slice = currentBlock.duplicate();
        slice.position(pos);
        slice.limit(pos + size);
        return slice.slice();
    }
    
    public synchronized byte[] allocateBytes(int size) {
        ByteBuffer buffer = allocate(size);
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        buffer.rewind();
        return bytes;
    }
    
    public synchronized void reset() {
        while (blocks.size() > 1) {
            blocks.remove(blocks.size() - 1);
        }
        if (!blocks.isEmpty()) {
            currentBlock = blocks.get(0);
            currentBlock.clear();
            currentBlockIndex = 0;
        }
        allocationCount.set(0);
        totalRequested.set(0);
    }
    
    @Override
    public synchronized void close() {
        blocks.clear();
        currentBlock = null;
        currentBlockIndex = -1;
        LOGGER.fine("[RPAL] Arena '" + name + "' closed");
    }
    
    public ArenaStats getStats() {
        long usedInCurrentBlock = currentBlock != null ? currentBlock.position() : 0;
        long usedInPreviousBlocks = (long) currentBlockIndex * blockSize;
        long totalUsed = usedInPreviousBlocks + usedInCurrentBlock;
        
        return new ArenaStats(
            name,
            blocks.size(),
            blockSize,
            totalAllocated.get(),
            totalRequested.get(),
            totalUsed,
            allocationCount.get(),
            useDirect
        );
    }
    
    public String getName() {
        return name;
    }
    
    private static int alignUp(int value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }
    
    public record ArenaStats(
        String name,
        int blockCount,
        int blockSize,
        long totalAllocated,
        long totalRequested,
        long totalUsed,
        long allocationCount,
        boolean directMemory
    ) {
        public double utilizationRatio() {
            return totalAllocated > 0 ? (double) totalUsed / totalAllocated : 0.0;
        }
        
        public double fragmentationRatio() {
            return totalAllocated > 0 ? (double) (totalAllocated - totalRequested) / totalAllocated : 0.0;
        }
    }
}
