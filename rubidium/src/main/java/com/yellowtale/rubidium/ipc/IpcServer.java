package com.yellowtale.rubidium.ipc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yellowtale.rubidium.RubidiumPlugin;
import com.yellowtale.rubidium.performance.PerformanceMonitor;
import com.yellowtale.rubidium.performance.RPAL;
import com.yellowtale.rubidium.performance.memory.ByteBufferPool;
import com.yellowtale.rubidium.performance.memory.ByteBufferPool.PooledByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class IpcServer {
    private static final Logger logger = LoggerFactory.getLogger(IpcServer.class);
    private static final Gson gson = new Gson();
    private static final int BUFFER_SIZE = 4096;
    
    private final int port;
    private final RubidiumPlugin rubidium;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService workerExecutor;
    private final ExecutorService writeExecutor;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private Thread selectorThread;
    
    private final AtomicLong messagesProcessed = new AtomicLong();
    private final AtomicLong bytesReceived = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();
    private final AtomicLong connectionCount = new AtomicLong();
    
    public IpcServer(int port, RubidiumPlugin rubidium) {
        this.port = port;
        this.rubidium = rubidium;
        this.workerExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "Rubidium-IPC-Worker");
                t.setDaemon(true);
                return t;
            }
        );
        this.writeExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-IPC-Writer");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            selectorThread = new Thread(this::selectorLoop, "Rubidium-IPC-Selector");
            selectorThread.setDaemon(true);
            selectorThread.start();
        }
    }
    
    public void stop() {
        running.set(false);
        try {
            if (selector != null) {
                selector.wakeup();
                selector.close();
            }
            if (serverChannel != null) {
                serverChannel.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing IPC server", e);
        }
        workerExecutor.shutdownNow();
        writeExecutor.shutdownNow();
    }
    
    private void selectorLoop() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            logger.info("IPC server listening on port {} (NIO with ByteBufferPool)", port);
            
            while (running.get()) {
                try {
                    int ready = selector.select(1000);
                    if (ready == 0) continue;
                    
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();
                        
                        if (!key.isValid()) continue;
                        
                        if (key.isAcceptable()) {
                            acceptConnection(key);
                        } else if (key.isReadable()) {
                            readFromClient(key);
                        }
                    }
                } catch (ClosedSelectorException e) {
                    break;
                } catch (IOException e) {
                    if (running.get()) {
                        logger.warn("Selector error", e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to start IPC server", e);
        } finally {
            cleanupAllClients();
        }
    }
    
    private void cleanupAllClients() {
        if (selector != null) {
            for (SelectionKey key : selector.keys()) {
                Object attachment = key.attachment();
                if (attachment instanceof ClientContext ctx) {
                    ctx.releaseBuffers();
                }
            }
        }
    }
    
    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        
        if (client != null) {
            client.configureBlocking(false);
            
            ClientContext ctx = new ClientContext(client);
            client.register(selector, SelectionKey.OP_READ, ctx);
            
            connectionCount.incrementAndGet();
            logger.debug("Client connected from {}", client.getRemoteAddress());
        }
    }
    
    private void readFromClient(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ClientContext ctx = (ClientContext) key.attachment();
        
        try {
            PooledByteBuffer pooledBuffer = ctx.getPooledReadBuffer();
            ByteBuffer readBuffer = pooledBuffer.buffer();
            
            readBuffer.clear();
            int bytesRead = client.read(readBuffer);
            
            if (bytesRead == -1) {
                ctx.releaseBuffers();
                key.cancel();
                client.close();
                return;
            }
            
            if (bytesRead > 0) {
                bytesReceived.addAndGet(bytesRead);
                readBuffer.flip();
                
                ctx.appendFromBuffer(readBuffer);
                
                String message;
                while ((message = ctx.extractLine()) != null) {
                    final String msg = message;
                    workerExecutor.submit(() -> processAndRespond(ctx, msg));
                }
            }
        } catch (IOException e) {
            logger.debug("Client read error: {}", e.getMessage());
            try {
                ctx.releaseBuffers();
                key.cancel();
                client.close();
            } catch (IOException ex) {
            }
        }
    }
    
    private void processAndRespond(ClientContext ctx, String message) {
        try {
            String response = processMessage(message);
            messagesProcessed.incrementAndGet();
            
            writeExecutor.submit(() -> {
                try {
                    sendResponse(ctx, response + "\n");
                } catch (IOException e) {
                    logger.debug("Failed to send response: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            logger.warn("Error processing message: {}", e.getMessage());
        }
    }
    
    private void sendResponse(ClientContext ctx, String response) throws IOException {
        if (!ctx.isOpen()) return;
        
        PooledByteBuffer pooledBuffer = ctx.getPooledWriteBuffer();
        ByteBuffer writeBuffer = pooledBuffer.buffer();
        
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        
        synchronized (ctx.getWriteLock()) {
            int offset = 0;
            while (offset < responseBytes.length) {
                writeBuffer.clear();
                int chunkSize = Math.min(responseBytes.length - offset, writeBuffer.capacity());
                writeBuffer.put(responseBytes, offset, chunkSize);
                writeBuffer.flip();
                
                while (writeBuffer.hasRemaining()) {
                    int written = ctx.getChannel().write(writeBuffer);
                    if (written > 0) {
                        bytesSent.addAndGet(written);
                    }
                    if (written == 0) {
                        Thread.yield();
                    }
                }
                offset += chunkSize;
            }
        }
    }
    
    private String processMessage(String json) {
        try {
            JsonObject msg = gson.fromJson(json, JsonObject.class);
            String type = msg.get("type").getAsString();
            
            JsonObject response = new JsonObject();
            
            switch (type) {
                case "Ping" -> {
                    response.addProperty("type", "Pong");
                    JsonObject payload = new JsonObject();
                    payload.addProperty("timestamp", System.currentTimeMillis());
                    response.add("payload", payload);
                }
                
                case "QueryStatus" -> {
                    response.addProperty("type", "ServerStatus");
                    JsonObject status = new JsonObject();
                    status.addProperty("id", "rubidium");
                    status.addProperty("name", "Rubidium Server");
                    status.addProperty("state", rubidium.getLifecycle().getState().name());
                    status.addProperty("uptime_seconds", rubidium.getLifecycle().getUptime() / 1000);
                    
                    PerformanceMonitor.PerformanceSnapshot snapshot = rubidium.getPerformanceMonitor().getLatest();
                    if (snapshot != null) {
                        status.addProperty("tps", snapshot.tps());
                        status.addProperty("memory_used_mb", snapshot.memoryUsedMb());
                        status.addProperty("cpu_percent", snapshot.cpuLoad());
                        status.addProperty("player_count", snapshot.playerCount());
                    }
                    
                    response.add("payload", status);
                }
                
                case "GetMetrics" -> {
                    response.addProperty("type", "Metrics");
                    JsonObject metrics = new JsonObject();
                    metrics.addProperty("server_id", "rubidium");
                    metrics.addProperty("timestamp", System.currentTimeMillis());
                    
                    PerformanceMonitor.PerformanceSnapshot snapshot = rubidium.getPerformanceMonitor().getLatest();
                    if (snapshot != null) {
                        metrics.addProperty("tps", snapshot.tps());
                        metrics.addProperty("memory_used_mb", snapshot.memoryUsedMb());
                        metrics.addProperty("memory_max_mb", snapshot.memoryMaxMb());
                        metrics.addProperty("cpu_percent", snapshot.cpuLoad());
                        metrics.addProperty("thread_count", snapshot.threadCount());
                        metrics.addProperty("gc_count", snapshot.gcCount());
                        metrics.addProperty("gc_time_ms", snapshot.gcTimeMs());
                        metrics.addProperty("buffer_pool_hit_rate", snapshot.bufferPoolHitRate());
                    }
                    
                    response.add("payload", metrics);
                }
                
                case "GetCapabilities" -> {
                    response.addProperty("type", "Capabilities");
                    JsonObject caps = new JsonObject();
                    caps.addProperty("version", rubidium.getVersion());
                    caps.addProperty("rubidium_version", rubidium.getVersion());
                    
                    if (RPAL.getInstance().isInitialized()) {
                        RPAL.Capabilities rpalCaps = RPAL.getInstance().getCapabilities();
                        
                        JsonObject rpal = new JsonObject();
                        rpal.addProperty("simd_available", rpalCaps.simdAvailable());
                        rpal.addProperty("jit_tiered", rpalCaps.jitTieredCompilation());
                        rpal.addProperty("direct_buffers", rpalCaps.directBuffersEnabled());
                        rpal.addProperty("object_pooling", rpalCaps.objectPoolingEnabled());
                        rpal.addProperty("tick_scheduler", rpalCaps.tickSchedulerEnabled());
                        rpal.addProperty("max_heap_mb", rpalCaps.maxHeapMemory() / (1024 * 1024));
                        rpal.addProperty("cpu_cores", rpalCaps.availableCpuCores());
                        rpal.addProperty("gc_type", rpalCaps.gcType());
                        
                        caps.add("rpal", rpal);
                    }
                    
                    JsonArray features = new JsonArray();
                    features.add("ipc_nio");
                    features.add("ipc_buffer_pool");
                    features.add("ipc_async_workers");
                    features.add("performance_monitoring");
                    features.add("plugin_system");
                    features.add("event_bus");
                    if (RPAL.getInstance().isInitialized()) {
                        features.add("rpal_simd");
                        features.add("rpal_memory_pools");
                        features.add("rpal_tick_scheduler");
                        features.add("rpal_jit_optimizer");
                    }
                    caps.add("features", features);
                    
                    response.add("payload", caps);
                }
                
                case "GetRPALSnapshot" -> {
                    response.addProperty("type", "RPALSnapshot");
                    JsonObject payload = new JsonObject();
                    
                    if (RPAL.getInstance().isInitialized()) {
                        RPAL.PerformanceSnapshot snap = RPAL.getInstance().getPerformanceSnapshot();
                        
                        JsonObject memory = new JsonObject();
                        memory.addProperty("used_mb", snap.usedMemory() / (1024 * 1024));
                        memory.addProperty("total_mb", snap.totalMemory() / (1024 * 1024));
                        memory.addProperty("max_mb", snap.maxMemory() / (1024 * 1024));
                        memory.addProperty("heap_used_mb", snap.heapUsed() / (1024 * 1024));
                        memory.addProperty("non_heap_used_mb", snap.nonHeapUsed() / (1024 * 1024));
                        memory.addProperty("utilization", snap.memoryUtilization());
                        payload.add("memory", memory);
                        
                        JsonObject gc = new JsonObject();
                        gc.addProperty("total_time_ms", snap.totalGcTimeMs());
                        gc.addProperty("total_count", snap.totalGcCount());
                        gc.addProperty("time_delta_ms", snap.gcTimeDeltaMs());
                        gc.addProperty("count_delta", snap.gcCountDelta());
                        payload.add("gc", gc);
                        
                        ByteBufferPool.PoolStats bufferStats = snap.bufferPoolStats();
                        JsonObject buffers = new JsonObject();
                        buffers.addProperty("allocations", bufferStats.totalAllocations());
                        buffers.addProperty("direct_allocations", bufferStats.directAllocations());
                        buffers.addProperty("hit_rate", bufferStats.hitRate());
                        buffers.addProperty("pooled_count", bufferStats.buffersInPool());
                        buffers.addProperty("pooled_bytes", bufferStats.pooledBytes());
                        payload.add("buffer_pool", buffers);
                        
                        if (snap.schedulerStats() != null) {
                            JsonObject scheduler = new JsonObject();
                            scheduler.addProperty("current_tick", snap.schedulerStats().currentTick());
                            scheduler.addProperty("tps", snap.schedulerStats().currentTps());
                            scheduler.addProperty("tick_utilization", snap.schedulerStats().tickUtilization());
                            scheduler.addProperty("overruns", snap.schedulerStats().tickOverruns());
                            scheduler.addProperty("pending_tasks", snap.schedulerStats().pendingTasks());
                            scheduler.addProperty("healthy", snap.schedulerStats().isHealthy());
                            payload.add("scheduler", scheduler);
                        }
                        
                        payload.addProperty("active_threads", snap.activeThreads());
                        payload.addProperty("timestamp", snap.timestamp());
                        payload.addProperty("memory_pressure_high", snap.isMemoryPressureHigh());
                    } else {
                        payload.addProperty("error", "RPAL not initialized");
                    }
                    
                    response.add("payload", payload);
                }
                
                case "GetIpcStats" -> {
                    response.addProperty("type", "IpcStats");
                    JsonObject stats = new JsonObject();
                    stats.addProperty("messages_processed", messagesProcessed.get());
                    stats.addProperty("bytes_received", bytesReceived.get());
                    stats.addProperty("bytes_sent", bytesSent.get());
                    stats.addProperty("total_connections", connectionCount.get());
                    
                    ByteBufferPool.PoolStats poolStats = ByteBufferPool.getInstance().getStats();
                    stats.addProperty("buffer_pool_allocations", poolStats.totalAllocations());
                    stats.addProperty("buffer_pool_direct_allocations", poolStats.directAllocations());
                    stats.addProperty("buffer_pool_hit_rate", poolStats.hitRate());
                    stats.addProperty("buffer_pool_hits", poolStats.poolHits());
                    stats.addProperty("buffer_pool_misses", poolStats.poolMisses());
                    stats.addProperty("buffers_in_pool", poolStats.buffersInPool());
                    
                    response.add("payload", stats);
                }
                
                default -> {
                    response.addProperty("type", "Error");
                    JsonObject error = new JsonObject();
                    error.addProperty("code", "InvalidRequest");
                    error.addProperty("message", "Unknown message type: " + type);
                    response.add("payload", error);
                }
            }
            
            return gson.toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "Error");
            JsonObject payload = new JsonObject();
            payload.addProperty("code", "InternalError");
            payload.addProperty("message", e.getMessage());
            error.add("payload", payload);
            return gson.toJson(error);
        }
    }
    
    public IpcStats getStats() {
        ByteBufferPool.PoolStats poolStats = ByteBufferPool.getInstance().getStats();
        return new IpcStats(
            messagesProcessed.get(),
            bytesReceived.get(),
            bytesSent.get(),
            connectionCount.get(),
            poolStats.totalAllocations(),
            poolStats.hitRate()
        );
    }
    
    private class ClientContext {
        private final SocketChannel channel;
        private final PooledByteBuffer pooledReadBuffer;
        private final PooledByteBuffer pooledWriteBuffer;
        private final StringBuilder accumulator = new StringBuilder();
        private final Object writeLock = new Object();
        private volatile boolean open = true;
        
        ClientContext(SocketChannel channel) {
            this.channel = channel;
            this.pooledReadBuffer = ByteBufferPool.getInstance().acquireDirect(BUFFER_SIZE);
            this.pooledWriteBuffer = ByteBufferPool.getInstance().acquireDirect(BUFFER_SIZE);
        }
        
        SocketChannel getChannel() {
            return channel;
        }
        
        PooledByteBuffer getPooledReadBuffer() {
            return pooledReadBuffer;
        }
        
        PooledByteBuffer getPooledWriteBuffer() {
            return pooledWriteBuffer;
        }
        
        Object getWriteLock() {
            return writeLock;
        }
        
        boolean isOpen() {
            return open && channel.isOpen();
        }
        
        synchronized void appendFromBuffer(ByteBuffer buffer) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            accumulator.append(new String(bytes, StandardCharsets.UTF_8));
        }
        
        synchronized String extractLine() {
            int newlineIdx = accumulator.indexOf("\n");
            if (newlineIdx == -1) return null;
            
            String line = accumulator.substring(0, newlineIdx);
            accumulator.delete(0, newlineIdx + 1);
            return line.isEmpty() ? null : line;
        }
        
        void releaseBuffers() {
            if (open) {
                open = false;
                pooledReadBuffer.release();
                pooledWriteBuffer.release();
            }
        }
    }
    
    public record IpcStats(
        long messagesProcessed,
        long bytesReceived,
        long bytesSent,
        long totalConnections,
        long bufferPoolAllocations,
        double bufferPoolHitRate
    ) {}
}
