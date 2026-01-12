namespace Rubidium.Api.Scheduling;

public interface ITaskScheduler
{
    IScheduledTask RunTask(RubidiumPlugin plugin, Action action);
    IScheduledTask RunTaskLater(RubidiumPlugin plugin, Action action, TimeSpan delay);
    IScheduledTask RunTaskTimer(RubidiumPlugin plugin, Action action, TimeSpan delay, TimeSpan period);
    IScheduledTask RunTaskAsync(RubidiumPlugin plugin, Func<Task> action);
    IScheduledTask RunTaskLaterAsync(RubidiumPlugin plugin, Func<Task> action, TimeSpan delay);
    IScheduledTask RunTaskTimerAsync(RubidiumPlugin plugin, Func<Task> action, TimeSpan delay, TimeSpan period);
    IScheduledTask RunOnMainThread(RubidiumPlugin plugin, Action action);
    void CancelTask(int taskId);
    void CancelTasks(RubidiumPlugin plugin);
    bool IsMainThread { get; }
    long CurrentTick { get; }
}

public interface IScheduledTask
{
    int TaskId { get; }
    RubidiumPlugin Owner { get; }
    bool IsCancelled { get; }
    bool IsSync { get; }
    void Cancel();
}
