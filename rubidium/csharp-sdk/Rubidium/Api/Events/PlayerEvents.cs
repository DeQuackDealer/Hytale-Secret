using Rubidium.Api.Player;

namespace Rubidium.Api.Events;

public abstract class PlayerEvent : Event
{
    public IPlayer Player { get; }

    protected PlayerEvent(IPlayer player, bool isAsync = false) : base(isAsync)
    {
        Player = player;
    }
}

public class PlayerJoinEvent : PlayerEvent, ICancellable
{
    public string JoinMessage { get; set; }
    public bool IsCancelled { get; set; }

    public PlayerJoinEvent(IPlayer player, string joinMessage) : base(player)
    {
        JoinMessage = joinMessage;
    }
}

public class PlayerQuitEvent : PlayerEvent
{
    public string QuitMessage { get; set; }
    public QuitReason Reason { get; }

    public PlayerQuitEvent(IPlayer player, string quitMessage, QuitReason reason) : base(player)
    {
        QuitMessage = quitMessage;
        Reason = reason;
    }

    public enum QuitReason
    {
        Disconnected,
        Kicked,
        TimedOut,
        ServerShutdown
    }
}

public class PlayerChatEvent : PlayerEvent, ICancellable
{
    public string Message { get; set; }
    public string Format { get; set; }
    public bool IsCancelled { get; set; }

    public PlayerChatEvent(IPlayer player, string message, string format) : base(player)
    {
        Message = message;
        Format = format;
    }
}
