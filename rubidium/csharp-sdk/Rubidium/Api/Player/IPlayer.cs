namespace Rubidium.Api.Player;

public interface IPlayer : ICommandSender
{
    Guid UniqueId { get; }
    new string Name { get; }
    string DisplayName { get; set; }
    bool IsOnline { get; }
    
    void Kick(string reason);
    void Teleport(double x, double y, double z);
    void Teleport(double x, double y, double z, float yaw, float pitch);
    
    double X { get; }
    double Y { get; }
    double Z { get; }
    float Yaw { get; }
    float Pitch { get; }
    string World { get; }
    
    int Ping { get; }
    string Address { get; }
    
    DateTime FirstPlayed { get; }
    DateTime LastPlayed { get; }
    bool HasPlayedBefore { get; }
    
    bool IsOp { get; set; }
    
    void ShowTitle(string title, string subtitle, int fadeIn, int stay, int fadeOut);
    void ShowActionBar(string message);
    void PlaySound(string sound, float volume, float pitch);
    
    IPlayerInventory Inventory { get; }
    IPlayerData Data { get; }
}

public interface ICommandSender
{
    void SendMessage(string message);
    void SendMessage(params string[] messages);
    bool HasPermission(string permission);
    bool IsPlayer { get; }
    string Name { get; }
}

public interface IPlayerInventory
{
    object? GetItem(int slot);
    void SetItem(int slot, object? item);
    object? MainHand { get; set; }
    object? OffHand { get; set; }
    object?[] Contents { get; set; }
    int Size { get; }
    void Clear();
    void Clear(int slot);
    int FirstEmpty();
    bool Contains(object item);
    void AddItem(params object[] items);
    void RemoveItem(params object[] items);
}

public interface IPlayerData
{
    void Set<T>(string key, T value);
    T? Get<T>(string key);
    T Get<T>(string key, T defaultValue);
    bool Has(string key);
    void Remove(string key);
    Task SaveAsync();
    void Reload();
}
