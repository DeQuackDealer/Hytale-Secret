using Microsoft.Extensions.Logging;
using Rubidium.Network.Bridge;
using Rubidium.Network.Core;

namespace Rubidium.Network;

public sealed class Program
{
    public static async Task Main(string[] args)
    {
        var loggerFactory = LoggerFactory.Create(builder =>
        {
            builder.AddConsole();
            builder.SetMinimumLevel(LogLevel.Information);
        });
        
        var config = ParseConfiguration(args);
        var logger = loggerFactory.CreateLogger<NetworkServer>();
        var bridgeLogger = loggerFactory.CreateLogger<JavaBridge>();
        
        Console.WriteLine(@"
╔═══════════════════════════════════════════════════════════════╗
║   ██████╗ ██╗   ██╗██████╗ ██╗██████╗ ██╗██╗   ██╗███╗   ███╗ ║
║   ██╔══██╗██║   ██║██╔══██╗██║██╔══██╗██║██║   ██║████╗ ████║ ║
║   ██████╔╝██║   ██║██████╔╝██║██║  ██║██║██║   ██║██╔████╔██║ ║
║   ██╔══██╗██║   ██║██╔══██╗██║██║  ██║██║██║   ██║██║╚██╔╝██║ ║
║   ██║  ██║╚██████╔╝██████╔╝██║██████╔╝██║╚██████╔╝██║ ╚═╝ ██║ ║
║   ╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝╚═════╝ ╚═╝ ╚═════╝ ╚═╝     ╚═╝ ║
║                    Network Foundation v1.0                    ║
╚═══════════════════════════════════════════════════════════════╝
");
        
        Console.WriteLine($"[Config] TCP Port: {config.TcpPort}");
        Console.WriteLine($"[Config] UDP Port: {config.UdpPort}");
        Console.WriteLine($"[Config] Max Connections: {config.MaxConnections}");
        Console.WriteLine($"[Config] Worker Threads: {config.WorkerThreads}");
        Console.WriteLine($"[Config] Buffer Size: {config.ReceiveBufferSize} bytes");
        Console.WriteLine();
        
        using var cts = new CancellationTokenSource();
        
        Console.CancelKeyPress += (_, e) =>
        {
            e.Cancel = true;
            cts.Cancel();
            Console.WriteLine("\n[Shutdown] Graceful shutdown initiated...");
        };
        
        await using var server = new NetworkServer(config, logger);
        await using var bridge = new JavaBridge(server, bridgeLogger);
        
        await server.StartAsync(cts.Token);
        
        _ = Task.Run(async () =>
        {
            try
            {
                await bridge.StartAsync(cts.Token);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Bridge] Java bridge error: {ex.Message}");
            }
        }, cts.Token);
        
        _ = Task.Run(async () =>
        {
            while (!cts.Token.IsCancellationRequested)
            {
                await Task.Delay(TimeSpan.FromSeconds(30), cts.Token);
                var metrics = server.GetMetrics();
                Console.WriteLine($"[Metrics] Connections: {metrics.ActiveConnections} | " +
                                  $"Packets In: {metrics.TotalPacketsReceived} | " +
                                  $"Packets Out: {metrics.TotalPacketsSent} | " +
                                  $"Memory Pool Reuse: {metrics.MemoryPoolStats.ReuseRatio:P1}");
            }
        }, cts.Token);
        
        Console.WriteLine("[Server] Network server started. Press Ctrl+C to shutdown.");
        Console.WriteLine("[Bridge] Waiting for Java process connection...");
        Console.WriteLine();
        
        try
        {
            await Task.Delay(Timeout.Infinite, cts.Token);
        }
        catch (OperationCanceledException)
        {
            // Normal shutdown
        }
        
        Console.WriteLine("[Shutdown] Cleanup complete.");
    }
    
    private static NetworkConfiguration ParseConfiguration(string[] args)
    {
        var config = NetworkConfiguration.Default;
        
        for (int i = 0; i < args.Length; i++)
        {
            var arg = args[i].ToLowerInvariant();
            
            config = arg switch
            {
                "--tcp-port" when i + 1 < args.Length && int.TryParse(args[++i], out var tcp) 
                    => config with { TcpPort = tcp },
                    
                "--udp-port" when i + 1 < args.Length && int.TryParse(args[++i], out var udp) 
                    => config with { UdpPort = udp },
                    
                "--max-connections" when i + 1 < args.Length && int.TryParse(args[++i], out var max) 
                    => config with { MaxConnections = max },
                    
                "--workers" when i + 1 < args.Length && int.TryParse(args[++i], out var workers) 
                    => config with { WorkerThreads = workers },
                    
                "--high-performance" 
                    => NetworkConfiguration.HighPerformance,
                    
                _ => config
            };
        }
        
        return config;
    }
}
