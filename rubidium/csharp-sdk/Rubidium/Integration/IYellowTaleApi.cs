namespace Rubidium.Integration;

public interface IYellowTaleApi
{
    ICosmeticsService Cosmetics { get; }
    IFriendsService Friends { get; }
    IPremiumService Premium { get; }
    ISessionService Sessions { get; }
    IAnalyticsService Analytics { get; }
    IMatchmakingService Matchmaking { get; }
}

public interface ICosmeticsService
{
    Task<PlayerCosmetics> GetPlayerCosmeticsAsync(Guid playerId);
    Task<bool> ValidateCosmeticAsync(Guid playerId, string cosmeticId);
    event Action<Guid, string, string>? OnCosmeticChange;
}

public interface IFriendsService
{
    Task<IReadOnlyList<FriendInfo>> GetFriendsAsync(Guid playerId);
    Task<bool> AreFriendsAsync(Guid player1, Guid player2);
    event Action<Guid, Guid>? OnFriendJoin;
    event Action<Guid, Guid>? OnFriendLeave;
}

public interface IPremiumService
{
    Task<bool> IsPremiumAsync(Guid playerId);
    Task<PremiumTier> GetPremiumTierAsync(Guid playerId);
    bool HasFeature(Guid playerId, string featureId);
}

public interface ISessionService
{
    Guid? GetSessionId(Guid playerId);
    Task<bool> TransferSessionAsync(Guid playerId, string targetServer);
    void RegisterServer(string serverId, ServerInfo info);
    void UpdatePlayerCount(int count);
}

public interface IAnalyticsService
{
    void TrackEvent(string eventName, object? data = null);
    void TrackPlayerAction(Guid playerId, string action, object? data = null);
    void TrackPerformanceMetric(string metric, double value);
}

public interface IMatchmakingService
{
    Task<MatchResult> FindMatchAsync(Guid playerId, MatchCriteria criteria);
    void JoinQueue(Guid playerId, string queueName);
    void LeaveQueue(Guid playerId);
    event Action<Guid, MatchResult>? OnMatchFound;
}

public record PlayerCosmetics(
    string? Skin,
    string? Cape,
    string? Wings,
    string? Aura,
    string[] Emotes);

public record FriendInfo(
    Guid Id,
    string Username,
    bool IsOnline,
    string? CurrentServer);

public enum PremiumTier
{
    None,
    Basic,
    Plus,
    Pro
}

public record ServerInfo(
    string Name,
    string Address,
    int MaxPlayers,
    string GameMode,
    string Region);

public record MatchCriteria
{
    public string? GameMode { get; init; }
    public int MinPlayers { get; init; }
    public int MaxPlayers { get; init; }
    public string? Region { get; init; }
    public int SkillLevel { get; init; }
}

public record MatchResult(
    bool Found,
    string? ServerId,
    string? ServerAddress);
