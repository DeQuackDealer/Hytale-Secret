using Rubidium.Api.Player;

namespace Rubidium.Api.Commands;

public interface ICommandManager
{
    void RegisterHandler(RubidiumPlugin plugin, object handler);
    void UnregisterHandlers(RubidiumPlugin plugin);
    bool ExecuteCommand(ICommandSender sender, string commandLine);
    IReadOnlyCollection<IRegisteredCommand> GetCommands();
    IRegisteredCommand? GetCommand(string name);
    IReadOnlyList<string> TabComplete(ICommandSender sender, string commandLine);
}

public interface IRegisteredCommand
{
    RubidiumPlugin Plugin { get; }
    string Name { get; }
    string[] Aliases { get; }
    string Description { get; }
    string Usage { get; }
    string Permission { get; }
    bool PlayerOnly { get; }
    int MinArgs { get; }
    int MaxArgs { get; }
    void Execute(ICommandSender sender, string[] args);
}
