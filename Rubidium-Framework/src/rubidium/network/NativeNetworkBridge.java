package rubidium.network;

import rubidium.core.logging.RubidiumLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class NativeNetworkBridge implements AutoCloseable {
    
    public record BridgeMessage(
        MessageType type,
        UUID connectionId,
        String data,
        String address,
        String reason,
        Map<String, Object> metadata
    ) {
        public static BridgeMessage playerConnected(UUID connectionId, String address) {
            return new BridgeMessage(MessageType.PLAYER_CONNECTED, connectionId, null, address, null, null);
        }
        
        public static BridgeMessage playerDisconnected(UUID connectionId, String reason) {
            return new BridgeMessage(MessageType.PLAYER_DISCONNECTED, connectionId, null, null, reason, null);
        }
        
        public static BridgeMessage packetReceived(UUID connectionId, byte[] data) {
            return new BridgeMessage(MessageType.PACKET_RECEIVED, connectionId, 
                Base64.getEncoder().encodeToString(data), null, null, null);
        }
        
        public static BridgeMessage sendPacket(UUID connectionId, byte[] data) {
            return new BridgeMessage(MessageType.SEND_PACKET, connectionId,
                Base64.getEncoder().encodeToString(data), null, null, null);
        }
        
        public static BridgeMessage broadcast(byte[] data) {
            return new BridgeMessage(MessageType.BROADCAST, null,
                Base64.getEncoder().encodeToString(data), null, null, null);
        }
        
        public static BridgeMessage disconnect(UUID connectionId, String reason) {
            return new BridgeMessage(MessageType.DISCONNECT, connectionId, null, null, reason, null);
        }
        
        public byte[] getDataBytes() {
            return data != null ? Base64.getDecoder().decode(data) : new byte[0];
        }
    }
    
    public enum MessageType {
        PLAYER_CONNECTED("PlayerConnected"),
        PLAYER_DISCONNECTED("PlayerDisconnected"),
        PACKET_RECEIVED("PacketReceived"),
        SEND_PACKET("SendPacket"),
        BROADCAST("Broadcast"),
        DISCONNECT("Disconnect"),
        GET_METRICS("GetMetrics"),
        METRICS_RESPONSE("MetricsResponse"),
        CUSTOM("Custom");
        
        private final String jsonValue;
        
        MessageType(String jsonValue) {
            this.jsonValue = jsonValue;
        }
        
        public String toJson() {
            return jsonValue;
        }
        
        public static MessageType fromJson(String value) {
            for (var type : values()) {
                if (type.jsonValue.equals(value)) {
                    return type;
                }
            }
            return CUSTOM;
        }
    }
    
    public record NetworkMetrics(
        long totalBytesReceived,
        long totalBytesSent,
        long totalPacketsReceived,
        long totalPacketsSent,
        long totalConnections,
        long activeConnections
    ) {}
    
    private final RubidiumLogger logger;
    private final String pipeName;
    private final ExecutorService executor;
    private final BlockingQueue<BridgeMessage> outgoingQueue;
    private final List<Consumer<BridgeMessage>> messageHandlers;
    
    private Process nativeProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private java.nio.channels.SocketChannel unixSocketChannel;
    private volatile boolean running;
    private CompletableFuture<NetworkMetrics> pendingMetricsRequest;
    
    public NativeNetworkBridge(RubidiumLogger logger, String pipeName) {
        this.logger = logger;
        this.pipeName = pipeName;
        this.executor = Executors.newCachedThreadPool();
        this.outgoingQueue = new LinkedBlockingQueue<>();
        this.messageHandlers = new CopyOnWriteArrayList<>();
    }
    
    public void registerMessageHandler(Consumer<BridgeMessage> handler) {
        messageHandlers.add(handler);
    }
    
    public CompletableFuture<Void> startAsync(String nativeExecutablePath) {
        return CompletableFuture.runAsync(() -> {
            try {
                startNativeProcess(nativeExecutablePath);
                connectToPipe();
                startReadLoop();
                startWriteLoop();
                running = true;
                logger.info("Native network bridge started");
            } catch (Exception e) {
                logger.error("Failed to start native network bridge: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    private void startNativeProcess(String executablePath) throws IOException {
        var processBuilder = new ProcessBuilder(executablePath);
        processBuilder.redirectErrorStream(true);
        nativeProcess = processBuilder.start();
        
        executor.submit(() -> {
            try (var procReader = new BufferedReader(new InputStreamReader(nativeProcess.getInputStream()))) {
                String line;
                while ((line = procReader.readLine()) != null) {
                    logger.debug("[Native] " + line);
                }
            } catch (IOException ignored) {}
        });
    }
    
    private void connectToPipe() throws IOException, InterruptedException {
        Thread.sleep(1000);
        
        var isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        
        for (int i = 0; i < 30; i++) {
            try {
                if (isWindows) {
                    var pipePath = "\\\\.\\pipe\\" + pipeName;
                    var file = new RandomAccessFile(pipePath, "rw");
                    reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file.getFD()), StandardCharsets.UTF_8));
                    writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file.getFD()), StandardCharsets.UTF_8));
                    return;
                } else {
                    var socketPath = java.nio.file.Path.of("/tmp", pipeName + ".sock");
                    var address = java.net.UnixDomainSocketAddress.of(socketPath);
                    unixSocketChannel = java.nio.channels.SocketChannel.open(address);
                    unixSocketChannel.configureBlocking(true);
                    
                    reader = new BufferedReader(java.nio.channels.Channels.newReader(
                        unixSocketChannel, StandardCharsets.UTF_8));
                    writer = new BufferedWriter(java.nio.channels.Channels.newWriter(
                        unixSocketChannel, StandardCharsets.UTF_8));
                    return;
                }
            } catch (IOException e) {
                Thread.sleep(500);
            }
        }
        
        throw new IOException("Failed to connect to native network bridge pipe");
    }
    
    private void startReadLoop() {
        executor.submit(() -> {
            while (running) {
                try {
                    var line = reader.readLine();
                    if (line == null) {
                        logger.warn("Native bridge disconnected");
                        running = false;
                        break;
                    }
                    
                    var message = parseMessage(line);
                    if (message != null) {
                        handleMessage(message);
                    }
                } catch (IOException e) {
                    if (running) {
                        logger.warn("Error reading from native bridge: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    private void startWriteLoop() {
        executor.submit(() -> {
            while (running) {
                try {
                    var message = outgoingQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        var json = serializeMessage(message);
                        writer.write(json);
                        writer.newLine();
                        writer.flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    if (running) {
                        logger.warn("Error writing to native bridge: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    private void handleMessage(BridgeMessage message) {
        if (message.type() == MessageType.METRICS_RESPONSE && pendingMetricsRequest != null) {
            pendingMetricsRequest.complete(parseMetrics(message.data()));
            pendingMetricsRequest = null;
            return;
        }
        
        for (var handler : messageHandlers) {
            try {
                handler.accept(message);
            } catch (Exception e) {
                logger.warn("Message handler error: " + e.getMessage());
            }
        }
    }
    
    public void sendPacket(UUID connectionId, byte[] data) {
        outgoingQueue.offer(BridgeMessage.sendPacket(connectionId, data));
    }
    
    public void broadcast(byte[] data) {
        outgoingQueue.offer(BridgeMessage.broadcast(data));
    }
    
    public void disconnect(UUID connectionId, String reason) {
        outgoingQueue.offer(BridgeMessage.disconnect(connectionId, reason));
    }
    
    public CompletableFuture<NetworkMetrics> getMetricsAsync() {
        pendingMetricsRequest = new CompletableFuture<>();
        outgoingQueue.offer(new BridgeMessage(MessageType.GET_METRICS, null, null, null, null, null));
        return pendingMetricsRequest.orTimeout(5, TimeUnit.SECONDS);
    }
    
    private BridgeMessage parseMessage(String json) {
        try {
            var typeStart = json.indexOf("\"Type\":\"") + 8;
            var typeEnd = json.indexOf("\"", typeStart);
            var type = MessageType.fromJson(json.substring(typeStart, typeEnd));
            
            UUID connectionId = null;
            var connIdStart = json.indexOf("\"ConnectionId\":\"");
            if (connIdStart > 0) {
                connIdStart += 16;
                var connIdEnd = json.indexOf("\"", connIdStart);
                connectionId = UUID.fromString(json.substring(connIdStart, connIdEnd));
            }
            
            String data = null;
            var dataStart = json.indexOf("\"Data\":\"");
            if (dataStart > 0) {
                dataStart += 8;
                var dataEnd = json.indexOf("\"", dataStart);
                data = json.substring(dataStart, dataEnd);
            }
            
            String address = null;
            var addrStart = json.indexOf("\"Address\":\"");
            if (addrStart > 0) {
                addrStart += 11;
                var addrEnd = json.indexOf("\"", addrStart);
                address = json.substring(addrStart, addrEnd);
            }
            
            String reason = null;
            var reasonStart = json.indexOf("\"Reason\":\"");
            if (reasonStart > 0) {
                reasonStart += 10;
                var reasonEnd = json.indexOf("\"", reasonStart);
                reason = json.substring(reasonStart, reasonEnd);
            }
            
            return new BridgeMessage(type, connectionId, data, address, reason, null);
        } catch (Exception e) {
            logger.warn("Failed to parse bridge message: " + e.getMessage());
            return null;
        }
    }
    
    private String serializeMessage(BridgeMessage message) {
        var sb = new StringBuilder();
        sb.append("{\"Type\":\"").append(message.type().toJson()).append("\"");
        
        if (message.connectionId() != null) {
            sb.append(",\"ConnectionId\":\"").append(message.connectionId()).append("\"");
        }
        if (message.data() != null) {
            sb.append(",\"Data\":\"").append(message.data()).append("\"");
        }
        if (message.address() != null) {
            sb.append(",\"Address\":\"").append(message.address()).append("\"");
        }
        if (message.reason() != null) {
            sb.append(",\"Reason\":\"").append(message.reason()).append("\"");
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private NetworkMetrics parseMetrics(String json) {
        return new NetworkMetrics(0, 0, 0, 0, 0, 0);
    }
    
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void close() {
        running = false;
        
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            if (reader != null) reader.close();
            if (unixSocketChannel != null && unixSocketChannel.isOpen()) {
                unixSocketChannel.close();
            }
        } catch (IOException ignored) {}
        
        if (nativeProcess != null) {
            nativeProcess.destroy();
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Native network bridge closed");
    }
}
