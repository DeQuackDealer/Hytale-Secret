using Microsoft.Extensions.Logging;
using Rubidium.Api.Commands;
using Rubidium.Api.Config;
using Rubidium.Api.Events;
using Rubidium.Api.Scheduling;
using Rubidium.Optimization;
using Rubidium.Integration;

namespace Rubidium.Api;

public abstract class RubidiumPlugin
{
    public PluginDescriptor Descriptor { get; private set; } = null!;
    public ILogger Logger { get; private set; } = null!;
    public string DataFolder { get; private set; } = null!;
    public PluginConfig Config { get; private set; } = null!;
    public bool IsEnabled { get; internal set; }
    
    internal IEventBus EventBus { get; private set; } = null!;
    internal ICommandManager CommandManager { get; private set; } = null!;
    internal ITaskScheduler Scheduler { get; private set; } = null!;
    internal IOptimizationContext Optimization { get; private set; } = null!;
    internal IYellowTaleApi YellowTale { get; private set; } = null!;

    internal void Initialize(
        PluginDescriptor descriptor,
        string dataFolder,
        ILoggerFactory loggerFactory,
        IEventBus eventBus,
        ICommandManager commandManager,
        ITaskScheduler scheduler,
        IOptimizationContext optimization,
        IYellowTaleApi yellowTale)
    {
        Descriptor = descriptor;
        DataFolder = dataFolder;
        Logger = loggerFactory.CreateLogger(descriptor.Id);
        Config = new PluginConfig(Path.Combine(dataFolder, "config.yml"));
        EventBus = eventBus;
        CommandManager = commandManager;
        Scheduler = scheduler;
        Optimization = optimization;
        YellowTale = yellowTale;
    }

    public abstract Task OnEnableAsync();

    public virtual Task OnDisableAsync() => Task.CompletedTask;

    public virtual Task OnLoadAsync() => Task.CompletedTask;

    public virtual Task OnReloadAsync()
    {
        Config.Reload();
        return Task.CompletedTask;
    }

    public void RegisterEvents(object listener) => EventBus.RegisterListener(this, listener);

    public void UnregisterEvents(object listener) => EventBus.UnregisterListener(this, listener);

    public void RegisterCommand(object handler) => CommandManager.RegisterHandler(this, handler);

    public IScheduledTask RunTask(Action action) => Scheduler.RunTask(this, action);

    public IScheduledTask RunTaskLater(Action action, TimeSpan delay) 
        => Scheduler.RunTaskLater(this, action, delay);

    public IScheduledTask RunTaskTimer(Action action, TimeSpan delay, TimeSpan period)
        => Scheduler.RunTaskTimer(this, action, delay, period);

    public IScheduledTask RunTaskAsync(Func<Task> action) => Scheduler.RunTaskAsync(this, action);

    protected void SaveDefaultConfig()
    {
        if (!File.Exists(Config.FilePath))
        {
            var stream = GetType().Assembly.GetManifestResourceStream("config.yml");
            if (stream != null)
            {
                Directory.CreateDirectory(DataFolder);
                using var fileStream = File.Create(Config.FilePath);
                stream.CopyTo(fileStream);
                Config.Reload();
            }
        }
    }

    protected Stream? GetResource(string name)
        => GetType().Assembly.GetManifestResourceStream(name);
}
