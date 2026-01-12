namespace Rubidium.Api.Attributes;

[AttributeUsage(AttributeTargets.Class, AllowMultiple = false)]
public class PluginAttribute : Attribute
{
    public string Id { get; }
    public string Name { get; init; }
    public string Version { get; init; } = "1.0.0";
    public string Author { get; init; } = "";
    public string Description { get; init; } = "";
    public string ApiVersion { get; init; } = "1.0.0";
    public string[] Dependencies { get; init; } = Array.Empty<string>();
    public string[] SoftDependencies { get; init; } = Array.Empty<string>();
    public string[] LoadBefore { get; init; } = Array.Empty<string>();

    public PluginAttribute(string id, string name, string version)
    {
        Id = id;
        Name = name;
        Version = version;
    }
}
