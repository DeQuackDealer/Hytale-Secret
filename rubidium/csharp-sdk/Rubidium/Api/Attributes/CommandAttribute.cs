namespace Rubidium.Api.Attributes;

[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public class CommandAttribute : Attribute
{
    public string Name { get; }
    public string[] Aliases { get; init; } = Array.Empty<string>();
    public string Description { get; init; } = "";
    public string Usage { get; init; } = "";
    public string Permission { get; init; } = "";
    public string PermissionMessage { get; init; } = "You don't have permission to use this command.";
    public bool PlayerOnly { get; init; } = false;
    public int MinArgs { get; init; } = 0;
    public int MaxArgs { get; init; } = -1;

    public CommandAttribute(string name)
    {
        Name = name;
    }
}
