using YamlDotNet.Serialization;
using YamlDotNet.Serialization.NamingConventions;

namespace Rubidium.Api.Config;

public class PluginConfig
{
    public string FilePath { get; }
    private Dictionary<string, object?> _data = new();
    
    private readonly IDeserializer _deserializer;
    private readonly ISerializer _serializer;

    public PluginConfig(string filePath)
    {
        FilePath = filePath;
        _deserializer = new DeserializerBuilder()
            .WithNamingConvention(CamelCaseNamingConvention.Instance)
            .Build();
        _serializer = new SerializerBuilder()
            .WithNamingConvention(CamelCaseNamingConvention.Instance)
            .Build();
        Reload();
    }

    public void Reload()
    {
        if (!File.Exists(FilePath))
        {
            _data = new Dictionary<string, object?>();
            return;
        }

        var content = File.ReadAllText(FilePath);
        _data = _deserializer.Deserialize<Dictionary<string, object?>>(content) ?? new();
    }

    public void Save()
    {
        Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
        var yaml = _serializer.Serialize(_data);
        File.WriteAllText(FilePath, yaml);
    }

    public void Set(string path, object? value)
    {
        var keys = path.Split('.');
        var current = _data;

        for (int i = 0; i < keys.Length - 1; i++)
        {
            if (!current.TryGetValue(keys[i], out var next) || next is not Dictionary<string, object?> nextDict)
            {
                nextDict = new Dictionary<string, object?>();
                current[keys[i]] = nextDict;
            }
            current = (Dictionary<string, object?>)current[keys[i]]!;
        }

        current[keys[^1]] = value;
    }

    public object? Get(string path, object? defaultValue = null)
    {
        var keys = path.Split('.');
        object? current = _data;

        foreach (var key in keys)
        {
            if (current is not Dictionary<string, object?> dict || !dict.TryGetValue(key, out current))
                return defaultValue;
        }

        return current ?? defaultValue;
    }

    public T Get<T>(string path, T defaultValue = default!) where T : notnull
    {
        var value = Get(path);
        if (value == null) return defaultValue;

        try
        {
            if (value is T typed) return typed;
            return (T)Convert.ChangeType(value, typeof(T));
        }
        catch
        {
            return defaultValue;
        }
    }

    public string GetString(string path, string defaultValue = "") => Get(path, defaultValue);
    public int GetInt(string path, int defaultValue = 0) => Get(path, defaultValue);
    public long GetLong(string path, long defaultValue = 0) => Get(path, defaultValue);
    public double GetDouble(string path, double defaultValue = 0.0) => Get(path, defaultValue);
    public bool GetBool(string path, bool defaultValue = false) => Get(path, defaultValue);

    public List<string> GetStringList(string path)
    {
        var value = Get(path);
        if (value is List<object> list)
            return list.Select(o => o?.ToString() ?? "").ToList();
        return new List<string>();
    }

    public bool Contains(string path) => Get(path) != null;

    public IEnumerable<string> GetKeys(bool deep = false)
    {
        if (!deep) return _data.Keys;
        return GetKeysRecursive(_data, "");
    }

    private IEnumerable<string> GetKeysRecursive(Dictionary<string, object?> dict, string prefix)
    {
        foreach (var kvp in dict)
        {
            var key = string.IsNullOrEmpty(prefix) ? kvp.Key : $"{prefix}.{kvp.Key}";
            yield return key;
            if (kvp.Value is Dictionary<string, object?> nested)
            {
                foreach (var nestedKey in GetKeysRecursive(nested, key))
                    yield return nestedKey;
            }
        }
    }
}
