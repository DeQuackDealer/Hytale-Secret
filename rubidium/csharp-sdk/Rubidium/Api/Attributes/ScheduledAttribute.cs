namespace Rubidium.Api.Attributes;

[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public class ScheduledAttribute : Attribute
{
    public long DelayMs { get; init; } = 0;
    public long PeriodMs { get; init; } = -1;
    public bool Async { get; init; } = false;
    public string Cron { get; init; } = "";
}
