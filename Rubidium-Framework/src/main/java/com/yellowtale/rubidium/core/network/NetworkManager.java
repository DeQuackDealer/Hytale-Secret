package com.yellowtale.rubidium.core.network;

import com.yellowtale.rubidium.core.metrics.MetricsRegistry;
import com.yellowtale.rubidium.core.performance.PerformanceBudgetManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Network abstraction layer with packet batching and priority queues.
 * 
 * This layer provides a protocol-agnostic interface for network operations.
 * It handles:
 * - Packet batching for efficiency
 * - Priority queues for traffic shaping
 * - Connection state management
 * - Bandwidth limiting
 * 
 * NOTE: This is an abstraction layer. Actual protocol implementation
 * will be provided when official server APIs are available.
 * 
 * TODO: Implement actual network transport when Hytale server API is released
 */
public final class NetworkManager {
    
    private final MetricsRegistry metricsRegistry;
    private final PerformanceBudgetManager performanceManager;
    
    private final Map<String, ConnectionHandler> connections = new ConcurrentHashMap<>();
    private final Map<String, PacketHandler<?>> packetHandlers = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<QueuedPacket> outboundQueue = new PriorityBlockingQueue<>();
    private final List<PacketInterceptor> interceptors = new CopyOnWriteArrayList<>();
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong packetsSent = new AtomicLong(0);
    private final AtomicLong packetsReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    
    private ScheduledExecutorService flushExecutor;
    private volatile int batchSize = 64;
    private volatile long flushIntervalMs = 10;
    private volatile long maxBandwidthBytesPerSecond = -1;
    
    public NetworkManager(MetricsRegistry metricsRegistry, PerformanceBudgetManager performanceManager) {
        this.metricsRegistry = metricsRegistry;
        this.performanceManager = performanceManager;
    }
    
    /**
     * Start the network manager.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        
        flushExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-NetworkFlush");
            t.setDaemon(true);
            return t;
        });
        
        flushExecutor.scheduleAtFixedRate(
            this::flushOutbound,
            flushIntervalMs,
            flushIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Stop the network manager.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        
        if (flushExecutor != null) {
            flushExecutor.shutdownNow();
        }
        
        connections.values().forEach(ConnectionHandler::close);
        connections.clear();
        outboundQueue.clear();
    }
    
    /**
     * Register a packet handler for a packet type.
     * 
     * @param packetType The class of the packet to handle
     * @param handler The handler function
     */
    public <T extends Packet> void registerHandler(Class<T> packetType, PacketHandler<T> handler) {
        packetHandlers.put(packetType.getName(), handler);
    }
    
    /**
     * Unregister a packet handler.
     */
    public void unregisterHandler(Class<? extends Packet> packetType) {
        packetHandlers.remove(packetType.getName());
    }
    
    /**
     * Add a packet interceptor for inspection/modification.
     */
    public void addInterceptor(PacketInterceptor interceptor) {
        interceptors.add(interceptor);
    }
    
    /**
     * Remove a packet interceptor.
     */
    public void removeInterceptor(PacketInterceptor interceptor) {
        interceptors.remove(interceptor);
    }
    
    /**
     * Send a packet to a connection.
     * 
     * @param connectionId The target connection
     * @param packet The packet to send
     * @param priority The packet priority
     */
    public void send(String connectionId, Packet packet, PacketPriority priority) {
        if (!running.get()) {
            return;
        }
        
        for (PacketInterceptor interceptor : interceptors) {
            packet = interceptor.onOutbound(connectionId, packet);
            if (packet == null) {
                return;
            }
        }
        
        outboundQueue.offer(new QueuedPacket(connectionId, packet, priority, System.nanoTime()));
    }
    
    /**
     * Send a packet with normal priority.
     */
    public void send(String connectionId, Packet packet) {
        send(connectionId, packet, PacketPriority.NORMAL);
    }
    
    /**
     * Broadcast a packet to all connections.
     */
    public void broadcast(Packet packet, PacketPriority priority) {
        for (String connectionId : connections.keySet()) {
            send(connectionId, packet, priority);
        }
    }
    
    /**
     * Broadcast a packet with normal priority.
     */
    public void broadcast(Packet packet) {
        broadcast(packet, PacketPriority.NORMAL);
    }
    
    /**
     * Process an inbound packet.
     * Called by the transport layer when a packet is received.
     */
    @SuppressWarnings("unchecked")
    public void onPacketReceived(String connectionId, Packet packet) {
        packetsReceived.incrementAndGet();
        bytesReceived.addAndGet(packet.getSize());
        metricsRegistry.counter("rubidium.network.packets.received").increment();
        
        for (PacketInterceptor interceptor : interceptors) {
            packet = interceptor.onInbound(connectionId, packet);
            if (packet == null) {
                return;
            }
        }
        
        PacketHandler handler = packetHandlers.get(packet.getClass().getName());
        if (handler != null) {
            try {
                handler.handle(connectionId, packet);
            } catch (Exception e) {
                metricsRegistry.counter("rubidium.network.handler.errors").increment();
            }
        }
    }
    
    /**
     * Register a new connection.
     */
    public void registerConnection(String connectionId, ConnectionHandler handler) {
        connections.put(connectionId, handler);
        metricsRegistry.counter("rubidium.network.connections.total").increment();
        metricsRegistry.gauge("rubidium.network.connections.active").set(connections.size());
    }
    
    /**
     * Unregister a connection.
     */
    public void unregisterConnection(String connectionId) {
        ConnectionHandler handler = connections.remove(connectionId);
        if (handler != null) {
            handler.close();
        }
        metricsRegistry.gauge("rubidium.network.connections.active").set(connections.size());
    }
    
    /**
     * Get the number of active connections.
     */
    public int getConnectionCount() {
        return connections.size();
    }
    
    /**
     * Get connection IDs.
     */
    public Set<String> getConnectionIds() {
        return Set.copyOf(connections.keySet());
    }
    
    /**
     * Get network statistics.
     */
    public NetworkStats getStats() {
        return new NetworkStats(
            packetsSent.get(),
            packetsReceived.get(),
            bytesSent.get(),
            bytesReceived.get(),
            connections.size(),
            outboundQueue.size()
        );
    }
    
    /**
     * Set the batch size for outbound packets.
     */
    public void setBatchSize(int size) {
        this.batchSize = Math.max(1, size);
    }
    
    /**
     * Set the flush interval in milliseconds.
     */
    public void setFlushInterval(long ms) {
        this.flushIntervalMs = Math.max(1, ms);
    }
    
    /**
     * Set maximum bandwidth limit in bytes per second.
     * -1 for unlimited.
     */
    public void setMaxBandwidth(long bytesPerSecond) {
        this.maxBandwidthBytesPerSecond = bytesPerSecond;
    }
    
    private void flushOutbound() {
        if (outboundQueue.isEmpty()) {
            return;
        }
        
        Map<String, List<Packet>> batches = new HashMap<>();
        int processed = 0;
        long bytesThisFlush = 0;
        long maxBytes = maxBandwidthBytesPerSecond > 0 
            ? maxBandwidthBytesPerSecond * flushIntervalMs / 1000 
            : Long.MAX_VALUE;
        
        while (processed < batchSize && bytesThisFlush < maxBytes) {
            QueuedPacket queued = outboundQueue.poll();
            if (queued == null) break;
            
            batches.computeIfAbsent(queued.connectionId(), k -> new ArrayList<>())
                .add(queued.packet());
            
            bytesThisFlush += queued.packet().getSize();
            processed++;
        }
        
        for (var entry : batches.entrySet()) {
            String connectionId = entry.getKey();
            List<Packet> packets = entry.getValue();
            
            ConnectionHandler handler = connections.get(connectionId);
            if (handler != null) {
                try {
                    for (Packet packet : packets) {
                        handler.send(packet);
                        packetsSent.incrementAndGet();
                        bytesSent.addAndGet(packet.getSize());
                    }
                    metricsRegistry.counter("rubidium.network.packets.sent").add(packets.size());
                } catch (Exception e) {
                    metricsRegistry.counter("rubidium.network.send.errors").increment();
                }
            }
        }
    }
    
    private record QueuedPacket(
        String connectionId,
        Packet packet,
        PacketPriority priority,
        long timestamp
    ) implements Comparable<QueuedPacket> {
        @Override
        public int compareTo(QueuedPacket other) {
            int priorityCompare = Integer.compare(
                other.priority.ordinal(), 
                this.priority.ordinal()
            );
            if (priorityCompare != 0) return priorityCompare;
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
    
    public record NetworkStats(
        long packetsSent,
        long packetsReceived,
        long bytesSent,
        long bytesReceived,
        int activeConnections,
        int queuedPackets
    ) {}
}
