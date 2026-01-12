namespace Rubidium.Api.Events;

public interface IEventBus
{
    void RegisterListener(RubidiumPlugin plugin, object listener);
    void UnregisterListener(RubidiumPlugin plugin, object listener);
    void UnregisterAll(RubidiumPlugin plugin);
    T FireEvent<T>(T evt) where T : Event;
    Task<T> FireEventAsync<T>(T evt) where T : Event;
}

public abstract class Event
{
    public string EventName => GetType().Name;
    public bool IsAsync { get; }

    protected Event(bool isAsync = false)
    {
        IsAsync = isAsync;
    }
}

public interface ICancellable
{
    bool IsCancelled { get; set; }
}

public enum EventPriority
{
    Lowest = 0,
    Low = 1,
    Normal = 2,
    High = 3,
    Highest = 4,
    Monitor = 5
}
