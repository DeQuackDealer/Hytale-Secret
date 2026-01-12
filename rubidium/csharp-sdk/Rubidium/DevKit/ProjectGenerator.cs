namespace Rubidium.DevKit;

public static class ProjectGenerator
{
    public static async Task GenerateCSharpPluginAsync(string outputDir, PluginTemplate template)
    {
        Directory.CreateDirectory(outputDir);
        
        await GenerateCsprojAsync(outputDir, template);
        await GenerateMainClassAsync(outputDir, template);
        await GeneratePluginTomlAsync(outputDir, template);
        await GenerateGitignoreAsync(outputDir);
    }

    private static async Task GenerateCsprojAsync(string outputDir, PluginTemplate template)
    {
        var content = $"""
            <Project Sdk="Microsoft.NET.Sdk">

              <PropertyGroup>
                <TargetFramework>net7.0</TargetFramework>
                <ImplicitUsings>enable</ImplicitUsings>
                <Nullable>enable</Nullable>
                <RootNamespace>{template.Namespace}</RootNamespace>
                <AssemblyName>{template.PluginName}</AssemblyName>
                <Version>{template.Version}</Version>
              </PropertyGroup>

              <ItemGroup>
                <PackageReference Include="Rubidium.SDK" Version="1.0.0" />
              </ItemGroup>

              <ItemGroup>
                <EmbeddedResource Include="plugin.toml" />
                <EmbeddedResource Include="config.yml" Condition="Exists('config.yml')" />
              </ItemGroup>

            </Project>
            """;
        
        await File.WriteAllTextAsync(Path.Combine(outputDir, $"{template.PluginName}.csproj"), content);
    }

    private static async Task GenerateMainClassAsync(string outputDir, PluginTemplate template)
    {
        var className = ToPascalCase(template.PluginId) + "Plugin";
        var content = $"""
            using Rubidium.Api;
            using Rubidium.Api.Attributes;
            using Rubidium.Api.Events;
            using Rubidium.Api.Player;

            namespace {template.Namespace};

            [Plugin("{template.PluginId}", "{template.PluginName}", "{template.Version}",
                Author = "{template.Author}",
                Description = "{template.Description}")]
            public class {className} : RubidiumPlugin
            {{
                public override async Task OnEnableAsync()
                {{
                    SaveDefaultConfig();
                    Logger.LogInformation("{template.PluginName} has been enabled!");
                    
                    RegisterEvents(this);
                    RegisterCommand(this);
                    
                    await Task.CompletedTask;
                }}

                public override async Task OnDisableAsync()
                {{
                    Logger.LogInformation("{template.PluginName} has been disabled!");
                    await Task.CompletedTask;
                }}

                [EventListener(EventPriority.Normal)]
                public void OnPlayerJoin(PlayerJoinEvent e)
                {{
                    e.Player.SendMessage($"Welcome to the server, {{e.Player.Name}}!");
                }}

                [Command("{template.PluginId}", Permission = "{template.PluginId}.use")]
                public void MainCommand(ICommandSender sender, string[] args)
                {{
                    sender.SendMessage($"{template.PluginName} v{template.Version} by {template.Author}");
                }}
            }}
            """;
        
        await File.WriteAllTextAsync(Path.Combine(outputDir, $"{className}.cs"), content);
    }

    private static async Task GeneratePluginTomlAsync(string outputDir, PluginTemplate template)
    {
        var className = ToPascalCase(template.PluginId) + "Plugin";
        var content = $"""
            [plugin]
            id = "{template.PluginId}"
            name = "{template.PluginName}"
            version = "{template.Version}"
            author = "{template.Author}"
            description = "{template.Description}"
            api_version = "1.0.0"
            main = "{template.Namespace}.{className}"

            [[dependencies]]
            # Add dependencies here
            # id = "other-plugin"
            # version = ">=1.0.0"
            # required = true
            """;
        
        await File.WriteAllTextAsync(Path.Combine(outputDir, "plugin.toml"), content);
    }

    private static async Task GenerateGitignoreAsync(string outputDir)
    {
        var content = """
            # Build
            bin/
            obj/
            
            # IDE
            .idea/
            *.suo
            *.user
            .vs/
            
            # OS
            .DS_Store
            Thumbs.db
            
            # Plugin
            run/
            """;
        
        await File.WriteAllTextAsync(Path.Combine(outputDir, ".gitignore"), content);
    }

    private static string ToPascalCase(string input)
    {
        var result = new System.Text.StringBuilder();
        bool capitalizeNext = true;

        foreach (var c in input)
        {
            if (c == '-' || c == '_')
            {
                capitalizeNext = true;
            }
            else if (capitalizeNext)
            {
                result.Append(char.ToUpper(c));
                capitalizeNext = false;
            }
            else
            {
                result.Append(c);
            }
        }

        return result.ToString();
    }
}

public record PluginTemplate(
    string PluginId,
    string PluginName,
    string Namespace,
    string Author)
{
    public string Version { get; init; } = "1.0.0";
    public string Description { get; init; } = "";
}
