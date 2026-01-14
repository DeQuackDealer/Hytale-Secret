package rubidium.hytale.api;

import java.util.List;

public record PluginMetadata(
    String name,
    String version,
    String mainClass,
    String description,
    String author,
    List<String> authors,
    List<String> depend,
    List<String> softDepend,
    String website
) {
    public PluginMetadata(String name, String version, String mainClass, String description) {
        this(name, version, mainClass, description, "", List.of(), List.of(), List.of(), "");
    }
    
    public static PluginMetadata from(JavaPluginInit init, String mainClass) {
        return new PluginMetadata(
            init.name(),
            init.version(),
            mainClass,
            init.description(),
            init.author(),
            List.of(init.authors()),
            List.of(init.depend()),
            List.of(init.softDepend()),
            init.website()
        );
    }
}
