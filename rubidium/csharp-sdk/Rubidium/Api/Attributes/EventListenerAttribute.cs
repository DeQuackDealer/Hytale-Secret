using Rubidium.Api.Events;

namespace Rubidium.Api.Attributes;

[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public class EventListenerAttribute : Attribute
{
    public EventPriority Priority { get; }
    public bool IgnoreCancelled { get; init; } = false;

    public EventListenerAttribute(EventPriority priority = EventPriority.Normal)
    {
        Priority = priority;
    }
}
