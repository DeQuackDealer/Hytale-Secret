package rubidium.models;

import rubidium.core.logging.RubidiumLogger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ModelManager {
    
    private final RubidiumLogger logger;
    private final Path modelsDir;
    
    private final Map<String, Model> models;
    private final Map<String, ModelDefinition> definitions;
    private final List<Consumer<ModelLoadEvent>> loadListeners;
    
    public ModelManager(RubidiumLogger logger, Path dataDir) {
        this.logger = logger;
        this.modelsDir = dataDir.resolve("models");
        this.models = new ConcurrentHashMap<>();
        this.definitions = new ConcurrentHashMap<>();
        this.loadListeners = new ArrayList<>();
    }
    
    public void registerDefinition(ModelDefinition definition) {
        definitions.put(definition.getId(), definition);
        logger.debug("Registered model definition: {}", definition.getId());
    }
    
    public Optional<ModelDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
    
    public Model loadModel(String definitionId) {
        ModelDefinition def = definitions.get(definitionId);
        if (def == null) {
            throw new IllegalArgumentException("Unknown model definition: " + definitionId);
        }
        
        return models.computeIfAbsent(definitionId, id -> {
            Model model = createModel(def);
            notifyLoadListeners(new ModelLoadEvent(model, ModelLoadEvent.Type.LOADED));
            return model;
        });
    }
    
    public Optional<Model> getModel(String id) {
        return Optional.ofNullable(models.get(id));
    }
    
    public void unloadModel(String id) {
        Model model = models.remove(id);
        if (model != null) {
            notifyLoadListeners(new ModelLoadEvent(model, ModelLoadEvent.Type.UNLOADED));
            logger.debug("Unloaded model: {}", id);
        }
    }
    
    public Model createInstance(String definitionId, UUID entityId) {
        Model template = loadModel(definitionId);
        return new Model(
            UUID.randomUUID().toString(),
            template.getDefinition(),
            template.getMeshes(),
            template.getTextures(),
            new HashMap<>(template.getMetadata())
        );
    }
    
    public void onModelLoad(Consumer<ModelLoadEvent> listener) {
        loadListeners.add(listener);
    }
    
    private Model createModel(ModelDefinition def) {
        List<Mesh> meshes = new ArrayList<>();
        List<String> textures = new ArrayList<>();
        
        for (String meshPath : def.getMeshPaths()) {
            meshes.add(new Mesh(meshPath, def.getMeshFormat()));
        }
        
        textures.addAll(def.getTexturePaths());
        
        Model model = new Model(def.getId(), def, meshes, textures, new HashMap<>());
        logger.info("Loaded model: {} ({} meshes, {} textures)", 
            def.getId(), meshes.size(), textures.size());
        
        return model;
    }
    
    private void notifyLoadListeners(ModelLoadEvent event) {
        for (Consumer<ModelLoadEvent> listener : loadListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Error in model load listener: {}", e.getMessage());
            }
        }
    }
    
    public int getLoadedModelCount() {
        return models.size();
    }
    
    public int getDefinitionCount() {
        return definitions.size();
    }
    
    public Collection<String> getLoadedModelIds() {
        return Collections.unmodifiableCollection(models.keySet());
    }
    
    public record ModelLoadEvent(Model model, Type type) {
        public enum Type { LOADED, UNLOADED, RELOADED }
    }
}
