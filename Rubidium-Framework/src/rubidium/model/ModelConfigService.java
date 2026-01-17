package rubidium.model;

import rubidium.core.logging.RubidiumLogger;
import rubidium.annotations.Command;
import rubidium.commands.CommandContext;
import rubidium.commands.CommandResult;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class ModelConfigService {
    
    public record ModelDefinition(
        String id,
        String name,
        String category,
        String baseModelPath,
        Map<String, Object> properties,
        List<ModelComponent> components,
        Map<String, TextureOverride> textureOverrides,
        AnimationConfig animations,
        TransformConfig transform,
        boolean isCustom,
        long lastModified
    ) {}
    
    public record ModelComponent(
        String name,
        String type,
        Vec3 position,
        Vec3 rotation,
        Vec3 scale,
        boolean visible,
        Map<String, Object> customData
    ) {}
    
    public record TextureOverride(
        String originalTexture,
        String newTexture,
        String layer,
        boolean animated,
        int animationFrames,
        int animationSpeed
    ) {}
    
    public record AnimationConfig(
        Map<String, AnimationEntry> animations,
        String defaultAnimation,
        float transitionSpeed
    ) {}
    
    public record AnimationEntry(
        String name,
        String path,
        float speed,
        boolean looping,
        int priority
    ) {}
    
    public record TransformConfig(
        Vec3 position,
        Vec3 rotation,
        Vec3 scale,
        Vec3 pivot
    ) {
        public static TransformConfig identity() {
            return new TransformConfig(
                Vec3.ZERO,
                Vec3.ZERO,
                Vec3.ONE,
                Vec3.ZERO
            );
        }
    }
    
    public record Vec3(double x, double y, double z) {
        public static final Vec3 ZERO = new Vec3(0, 0, 0);
        public static final Vec3 ONE = new Vec3(1, 1, 1);
        
        public Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }
        
        public Vec3 multiply(double scalar) {
            return new Vec3(x * scalar, y * scalar, z * scalar);
        }
    }
    
    public record ModelPatch(
        String targetModelId,
        PatchOperation operation,
        String path,
        Object value,
        String description
    ) {}
    
    public enum PatchOperation {
        SET,
        REMOVE,
        ADD,
        REPLACE,
        MERGE
    }
    
    public record ValidationResult(
        boolean valid,
        List<String> errors,
        List<String> warnings
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of(), List.of());
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors, List.of());
        }
    }
    
    private final RubidiumLogger logger;
    private final Path configDirectory;
    private final Map<String, ModelDefinition> models = new ConcurrentHashMap<>();
    private final Map<String, ModelDefinition> originalModels = new ConcurrentHashMap<>();
    private final Map<String, List<ModelPatch>> pendingPatches = new ConcurrentHashMap<>();
    private final List<Consumer<ModelChangeEvent>> changeListeners = new CopyOnWriteArrayList<>();
    
    private BiConsumer<String, ModelDefinition> modelApplier;
    private WatchService watchService;
    private volatile boolean hotReloadEnabled = true;
    
    public ModelConfigService(RubidiumLogger logger, Path configDirectory) {
        this.logger = logger;
        this.configDirectory = configDirectory;
        
        try {
            Files.createDirectories(configDirectory);
            initializeWatchService();
        } catch (IOException e) {
            logger.error("Failed to initialize model config directory: " + e.getMessage());
        }
    }
    
    private void initializeWatchService() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            configDirectory.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            );
            
            new Thread(this::watchForChanges, "model-config-watcher").start();
            logger.info("Model hot-reload watcher initialized");
        } catch (IOException e) {
            logger.warn("Failed to initialize file watcher: " + e.getMessage());
        }
    }
    
    private void watchForChanges() {
        while (hotReloadEnabled) {
            try {
                var key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) continue;
                
                for (var event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    
                    var filename = event.context().toString();
                    if (filename.endsWith(".json") || filename.endsWith(".yaml")) {
                        logger.info("Model config changed: " + filename);
                        reloadModel(filename);
                    }
                }
                
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void setModelApplier(BiConsumer<String, ModelDefinition> applier) {
        this.modelApplier = applier;
    }
    
    public void registerModel(ModelDefinition model) {
        models.put(model.id(), model);
        originalModels.putIfAbsent(model.id(), model);
        logger.debug("Registered model: " + model.id());
    }
    
    public Optional<ModelDefinition> getModel(String id) {
        return Optional.ofNullable(models.get(id));
    }
    
    public Collection<ModelDefinition> getAllModels() {
        return Collections.unmodifiableCollection(models.values());
    }
    
    public List<ModelDefinition> getModelsByCategory(String category) {
        return models.values().stream()
            .filter(m -> m.category().equalsIgnoreCase(category))
            .toList();
    }
    
    public ValidationResult validateModel(ModelDefinition model) {
        var errors = new ArrayList<String>();
        var warnings = new ArrayList<String>();
        
        if (model.id() == null || model.id().isBlank()) {
            errors.add("Model ID is required");
        }
        
        if (model.name() == null || model.name().isBlank()) {
            errors.add("Model name is required");
        }
        
        if (model.baseModelPath() == null && !model.isCustom()) {
            warnings.add("No base model path specified");
        }
        
        for (var component : model.components()) {
            if (component.name() == null || component.name().isBlank()) {
                errors.add("Component name is required");
            }
            if (component.scale() != null) {
                if (component.scale().x() <= 0 || component.scale().y() <= 0 || component.scale().z() <= 0) {
                    warnings.add("Component '" + component.name() + "' has non-positive scale values");
                }
            }
        }
        
        for (var override : model.textureOverrides().values()) {
            if (override.animated() && override.animationFrames() <= 0) {
                errors.add("Animated texture must have positive frame count");
            }
        }
        
        if (errors.isEmpty()) {
            return new ValidationResult(true, errors, warnings);
        }
        return new ValidationResult(false, errors, warnings);
    }
    
    public boolean applyPatch(ModelPatch patch) {
        var model = models.get(patch.targetModelId());
        if (model == null) {
            logger.warn("Cannot patch unknown model: " + patch.targetModelId());
            return false;
        }
        
        try {
            var patched = applyPatchToModel(model, patch);
            var validation = validateModel(patched);
            
            if (!validation.valid()) {
                logger.warn("Patch validation failed: " + String.join(", ", validation.errors()));
                return false;
            }
            
            models.put(patched.id(), patched);
            
            if (modelApplier != null) {
                modelApplier.accept(patched.id(), patched);
            }
            
            notifyChange(new ModelChangeEvent(patched.id(), "PATCH", patch.description()));
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to apply patch: " + e.getMessage());
            return false;
        }
    }
    
    private ModelDefinition applyPatchToModel(ModelDefinition model, ModelPatch patch) {
        return switch (patch.operation()) {
            case SET -> applySetPatch(model, patch);
            case REMOVE -> applyRemovePatch(model, patch);
            case ADD -> applyAddPatch(model, patch);
            case REPLACE -> applyReplacePatch(model, patch);
            case MERGE -> applyMergePatch(model, patch);
        };
    }
    
    private ModelDefinition applySetPatch(ModelDefinition model, ModelPatch patch) {
        var newProps = new HashMap<>(model.properties());
        newProps.put(patch.path(), patch.value());
        
        return new ModelDefinition(
            model.id(),
            model.name(),
            model.category(),
            model.baseModelPath(),
            newProps,
            model.components(),
            model.textureOverrides(),
            model.animations(),
            model.transform(),
            model.isCustom(),
            System.currentTimeMillis()
        );
    }
    
    private ModelDefinition applyRemovePatch(ModelDefinition model, ModelPatch patch) {
        var newProps = new HashMap<>(model.properties());
        newProps.remove(patch.path());
        
        return new ModelDefinition(
            model.id(),
            model.name(),
            model.category(),
            model.baseModelPath(),
            newProps,
            model.components(),
            model.textureOverrides(),
            model.animations(),
            model.transform(),
            model.isCustom(),
            System.currentTimeMillis()
        );
    }
    
    private ModelDefinition applyAddPatch(ModelDefinition model, ModelPatch patch) {
        return applySetPatch(model, patch);
    }
    
    private ModelDefinition applyReplacePatch(ModelDefinition model, ModelPatch patch) {
        return applySetPatch(model, patch);
    }
    
    @SuppressWarnings("unchecked")
    private ModelDefinition applyMergePatch(ModelDefinition model, ModelPatch patch) {
        var newProps = new HashMap<>(model.properties());
        
        if (patch.value() instanceof Map<?, ?> patchMap) {
            var existingValue = newProps.get(patch.path());
            if (existingValue instanceof Map<?, ?> existingMap) {
                var merged = new HashMap<>((Map<String, Object>) existingMap);
                merged.putAll((Map<String, Object>) patchMap);
                newProps.put(patch.path(), merged);
            } else {
                newProps.put(patch.path(), patch.value());
            }
        }
        
        return new ModelDefinition(
            model.id(),
            model.name(),
            model.category(),
            model.baseModelPath(),
            newProps,
            model.components(),
            model.textureOverrides(),
            model.animations(),
            model.transform(),
            model.isCustom(),
            System.currentTimeMillis()
        );
    }
    
    public ModelDefinition setTransform(String modelId, TransformConfig transform) {
        var model = models.get(modelId);
        if (model == null) return null;
        
        var updated = new ModelDefinition(
            model.id(),
            model.name(),
            model.category(),
            model.baseModelPath(),
            model.properties(),
            model.components(),
            model.textureOverrides(),
            model.animations(),
            transform,
            model.isCustom(),
            System.currentTimeMillis()
        );
        
        models.put(modelId, updated);
        
        if (modelApplier != null) {
            modelApplier.accept(modelId, updated);
        }
        
        notifyChange(new ModelChangeEvent(modelId, "TRANSFORM", "Transform updated"));
        
        return updated;
    }
    
    public ModelDefinition addTextureOverride(String modelId, String key, TextureOverride override) {
        var model = models.get(modelId);
        if (model == null) return null;
        
        var newOverrides = new HashMap<>(model.textureOverrides());
        newOverrides.put(key, override);
        
        var updated = new ModelDefinition(
            model.id(),
            model.name(),
            model.category(),
            model.baseModelPath(),
            model.properties(),
            model.components(),
            newOverrides,
            model.animations(),
            model.transform(),
            model.isCustom(),
            System.currentTimeMillis()
        );
        
        models.put(modelId, updated);
        
        if (modelApplier != null) {
            modelApplier.accept(modelId, updated);
        }
        
        notifyChange(new ModelChangeEvent(modelId, "TEXTURE", "Texture override added: " + key));
        
        return updated;
    }
    
    public boolean resetModel(String modelId) {
        var original = originalModels.get(modelId);
        if (original == null) {
            logger.warn("No original model found for: " + modelId);
            return false;
        }
        
        models.put(modelId, original);
        
        if (modelApplier != null) {
            modelApplier.accept(modelId, original);
        }
        
        notifyChange(new ModelChangeEvent(modelId, "RESET", "Model reset to original"));
        
        return true;
    }
    
    public void saveConfig(String modelId) throws IOException {
        var model = models.get(modelId);
        if (model == null) return;
        
        var file = configDirectory.resolve(modelId + ".json");
        var json = serializeModel(model);
        Files.writeString(file, json);
        
        logger.info("Saved model config: " + modelId);
    }
    
    private String serializeModel(ModelDefinition model) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(model.id()).append("\",\n");
        sb.append("  \"name\": \"").append(model.name()).append("\",\n");
        sb.append("  \"category\": \"").append(model.category()).append("\",\n");
        sb.append("  \"baseModelPath\": \"").append(model.baseModelPath() != null ? model.baseModelPath() : "").append("\",\n");
        sb.append("  \"isCustom\": ").append(model.isCustom()).append(",\n");
        sb.append("  \"transform\": {\n");
        sb.append("    \"position\": [").append(model.transform().position().x()).append(", ")
          .append(model.transform().position().y()).append(", ")
          .append(model.transform().position().z()).append("],\n");
        sb.append("    \"rotation\": [").append(model.transform().rotation().x()).append(", ")
          .append(model.transform().rotation().y()).append(", ")
          .append(model.transform().rotation().z()).append("],\n");
        sb.append("    \"scale\": [").append(model.transform().scale().x()).append(", ")
          .append(model.transform().scale().y()).append(", ")
          .append(model.transform().scale().z()).append("]\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }
    
    public void reloadModel(String filename) {
        try {
            var file = configDirectory.resolve(filename);
            if (!Files.exists(file)) return;
            
            var content = Files.readString(file);
            var model = parseModelConfig(content);
            
            if (model != null) {
                var validation = validateModel(model);
                if (validation.valid()) {
                    models.put(model.id(), model);
                    
                    if (modelApplier != null) {
                        modelApplier.accept(model.id(), model);
                    }
                    
                    notifyChange(new ModelChangeEvent(model.id(), "RELOAD", "Hot-reloaded from file"));
                    logger.info("Reloaded model: " + model.id());
                } else {
                    logger.warn("Model validation failed: " + String.join(", ", validation.errors()));
                }
            }
        } catch (IOException e) {
            logger.error("Failed to reload model: " + e.getMessage());
        }
    }
    
    private ModelDefinition parseModelConfig(String content) {
        return null;
    }
    
    public void registerChangeListener(Consumer<ModelChangeEvent> listener) {
        changeListeners.add(listener);
    }
    
    private void notifyChange(ModelChangeEvent event) {
        for (var listener : changeListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.warn("Model change listener failed: " + e.getMessage());
            }
        }
    }
    
    public void setHotReloadEnabled(boolean enabled) {
        this.hotReloadEnabled = enabled;
    }
    
    public void shutdown() {
        hotReloadEnabled = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {}
    }
    
    public record ModelChangeEvent(
        String modelId,
        String changeType,
        String description
    ) {}
    
    @Command(
        name = "model",
        description = "Manage model configurations",
        usage = "/model <list|info|reset|scale|rotate> [model] [args]",
        permission = "rubidium.model.admin"
    )
    public CommandResult modelCommand(CommandContext ctx) {
        if (ctx.args().length == 0) {
            showModelHelp(ctx);
            return CommandResult.ok();
        }
        
        var subcommand = ctx.args()[0].toLowerCase();
        
        return switch (subcommand) {
            case "list" -> listModels(ctx);
            case "info" -> showModelInfo(ctx);
            case "reset" -> resetModelCmd(ctx);
            case "scale" -> scaleModel(ctx);
            case "rotate" -> rotateModel(ctx);
            default -> {
                showModelHelp(ctx);
                yield CommandResult.ok();
            }
        };
    }
    
    private void showModelHelp(CommandContext ctx) {
        ctx.sender().sendMessage("&6=== Model Configuration ===");
        ctx.sender().sendMessage("&e/model list [category] &7- List all models");
        ctx.sender().sendMessage("&e/model info <model> &7- Show model details");
        ctx.sender().sendMessage("&e/model reset <model> &7- Reset model to original");
        ctx.sender().sendMessage("&e/model scale <model> <x> <y> <z> &7- Scale a model");
        ctx.sender().sendMessage("&e/model rotate <model> <x> <y> <z> &7- Rotate a model");
    }
    
    private CommandResult listModels(CommandContext ctx) {
        var category = ctx.args().length > 1 ? ctx.args()[1] : null;
        
        var modelList = category != null 
            ? getModelsByCategory(category) 
            : getAllModels().stream().toList();
        
        ctx.sender().sendMessage("&6=== Models (" + modelList.size() + ") ===");
        for (var model : modelList) {
            var status = model.isCustom() ? "&a[Custom]" : "&7[Base]";
            ctx.sender().sendMessage("  " + status + " &f" + model.id() + " &7- " + model.name());
        }
        
        return CommandResult.ok();
    }
    
    private CommandResult showModelInfo(CommandContext ctx) {
        if (ctx.args().length < 2) {
            ctx.sender().sendMessage("&cUsage: /model info <model>");
            return CommandResult.failure("Missing model ID");
        }
        
        var modelId = ctx.args()[1];
        var modelOpt = getModel(modelId);
        
        if (modelOpt.isEmpty()) {
            ctx.sender().sendMessage("&cModel not found: " + modelId);
            return CommandResult.failure("Model not found");
        }
        
        var model = modelOpt.get();
        ctx.sender().sendMessage("&6=== Model: " + model.name() + " ===");
        ctx.sender().sendMessage("&7ID: &f" + model.id());
        ctx.sender().sendMessage("&7Category: &f" + model.category());
        ctx.sender().sendMessage("&7Base Path: &f" + (model.baseModelPath() != null ? model.baseModelPath() : "N/A"));
        ctx.sender().sendMessage("&7Components: &f" + model.components().size());
        ctx.sender().sendMessage("&7Texture Overrides: &f" + model.textureOverrides().size());
        ctx.sender().sendMessage("&7Custom: &f" + (model.isCustom() ? "Yes" : "No"));
        
        var t = model.transform();
        ctx.sender().sendMessage("&7Transform:");
        ctx.sender().sendMessage("  &7Position: &f" + t.position().x() + ", " + t.position().y() + ", " + t.position().z());
        ctx.sender().sendMessage("  &7Rotation: &f" + t.rotation().x() + ", " + t.rotation().y() + ", " + t.rotation().z());
        ctx.sender().sendMessage("  &7Scale: &f" + t.scale().x() + ", " + t.scale().y() + ", " + t.scale().z());
        
        return CommandResult.ok();
    }
    
    private CommandResult resetModelCmd(CommandContext ctx) {
        if (ctx.args().length < 2) {
            ctx.sender().sendMessage("&cUsage: /model reset <model>");
            return CommandResult.failure("Missing model ID");
        }
        
        var modelId = ctx.args()[1];
        if (resetModel(modelId)) {
            ctx.sender().sendMessage("&aModel '" + modelId + "' has been reset.");
        } else {
            ctx.sender().sendMessage("&cFailed to reset model: " + modelId);
        }
        
        return CommandResult.ok();
    }
    
    private CommandResult scaleModel(CommandContext ctx) {
        if (ctx.args().length < 5) {
            ctx.sender().sendMessage("&cUsage: /model scale <model> <x> <y> <z>");
            return CommandResult.failure("Missing arguments");
        }
        
        var modelId = ctx.args()[1];
        try {
            var x = Double.parseDouble(ctx.args()[2]);
            var y = Double.parseDouble(ctx.args()[3]);
            var z = Double.parseDouble(ctx.args()[4]);
            
            var model = models.get(modelId);
            if (model == null) {
                ctx.sender().sendMessage("&cModel not found: " + modelId);
                return CommandResult.failure("Model not found");
            }
            
            var newTransform = new TransformConfig(
                model.transform().position(),
                model.transform().rotation(),
                new Vec3(x, y, z),
                model.transform().pivot()
            );
            
            setTransform(modelId, newTransform);
            ctx.sender().sendMessage("&aModel scale updated to " + x + ", " + y + ", " + z);
            
        } catch (NumberFormatException e) {
            ctx.sender().sendMessage("&cInvalid number format");
            return CommandResult.failure("Invalid numbers");
        }
        
        return CommandResult.ok();
    }
    
    private CommandResult rotateModel(CommandContext ctx) {
        if (ctx.args().length < 5) {
            ctx.sender().sendMessage("&cUsage: /model rotate <model> <x> <y> <z>");
            return CommandResult.failure("Missing arguments");
        }
        
        var modelId = ctx.args()[1];
        try {
            var x = Double.parseDouble(ctx.args()[2]);
            var y = Double.parseDouble(ctx.args()[3]);
            var z = Double.parseDouble(ctx.args()[4]);
            
            var model = models.get(modelId);
            if (model == null) {
                ctx.sender().sendMessage("&cModel not found: " + modelId);
                return CommandResult.failure("Model not found");
            }
            
            var newTransform = new TransformConfig(
                model.transform().position(),
                new Vec3(x, y, z),
                model.transform().scale(),
                model.transform().pivot()
            );
            
            setTransform(modelId, newTransform);
            ctx.sender().sendMessage("&aModel rotation updated to " + x + ", " + y + ", " + z);
            
        } catch (NumberFormatException e) {
            ctx.sender().sendMessage("&cInvalid number format");
            return CommandResult.failure("Invalid numbers");
        }
        
        return CommandResult.ok();
    }
}
