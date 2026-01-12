namespace Rubidium.Api;

public record PluginDescriptor(
    string Id,
    string Name,
    string Version,
    string Author,
    string Description,
    string MainClass,
    string ApiVersion,
    IReadOnlyList<PluginDependency> Dependencies,
    IReadOnlyList<string> SoftDependencies,
    IReadOnlyList<string> LoadBefore)
{
    public static PluginDescriptorBuilder Builder(string id) => new(id);
}

public record PluginDependency(string PluginId, string VersionRange, bool Required)
{
    public static PluginDependency Required(string pluginId) => new(pluginId, "*", true);
    public static PluginDependency Optional(string pluginId) => new(pluginId, "*", false);
}

public class PluginDescriptorBuilder
{
    private readonly string _id;
    private string _name;
    private string _version = "1.0.0";
    private string _author = "Unknown";
    private string _description = "";
    private string _mainClass = "";
    private string _apiVersion = "1.0.0";
    private List<PluginDependency> _dependencies = new();
    private List<string> _softDependencies = new();
    private List<string> _loadBefore = new();

    internal PluginDescriptorBuilder(string id)
    {
        _id = id;
        _name = id;
    }

    public PluginDescriptorBuilder Name(string name) { _name = name; return this; }
    public PluginDescriptorBuilder Version(string version) { _version = version; return this; }
    public PluginDescriptorBuilder Author(string author) { _author = author; return this; }
    public PluginDescriptorBuilder Description(string desc) { _description = desc; return this; }
    public PluginDescriptorBuilder MainClass(string mainClass) { _mainClass = mainClass; return this; }
    public PluginDescriptorBuilder ApiVersion(string apiVersion) { _apiVersion = apiVersion; return this; }
    public PluginDescriptorBuilder AddDependency(PluginDependency dep) { _dependencies.Add(dep); return this; }
    public PluginDescriptorBuilder AddSoftDependency(string id) { _softDependencies.Add(id); return this; }
    public PluginDescriptorBuilder LoadBefore(string id) { _loadBefore.Add(id); return this; }

    public PluginDescriptor Build() => new(
        _id, _name, _version, _author, _description, _mainClass, _apiVersion,
        _dependencies.AsReadOnly(), _softDependencies.AsReadOnly(), _loadBefore.AsReadOnly());
}
