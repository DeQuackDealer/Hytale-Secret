package rubidium.model;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Loader for .blockymodel files and runtime model management.
 * Supports Hytale's 64px/unit for characters, 32px/unit for blocks.
 */
public class ModelLoader {
    
    private static ModelLoader instance;
    
    private final Map<String, BlockyModel> models;
    private final Map<String, ModelGroup> groups;
    private final ExecutorService loadExecutor;
    private final Path modelsPath;
    private final Gson gson;
    
    private ModelLoader() {
        this.models = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.loadExecutor = Executors.newFixedThreadPool(4);
        this.modelsPath = Paths.get("assets", "models");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public static ModelLoader getInstance() {
        if (instance == null) {
            instance = new ModelLoader();
        }
        return instance;
    }
    
    public CompletableFuture<BlockyModel> loadModel(String modelId) {
        BlockyModel cached = models.get(modelId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            Path modelPath = modelsPath.resolve(modelId + ".blockymodel");
            
            if (!Files.exists(modelPath)) {
                modelPath = modelsPath.resolve(modelId + ".json");
            }
            
            try {
                String content = Files.readString(modelPath);
                BlockyModel model = parseModel(modelId, content);
                models.put(modelId, model);
                return model;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load model: " + modelId, e);
            }
        }, loadExecutor);
    }
    
    private BlockyModel parseModel(String id, String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        int formatVersion = root.has("formatVersion") ? root.get("formatVersion").getAsInt() : 1;
        String texture = root.has("texture") ? root.get("texture").getAsString() : null;
        
        List<ModelElement> elements = new ArrayList<>();
        if (root.has("elements")) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            for (JsonElement elem : elementsArray) {
                elements.add(parseElement(elem.getAsJsonObject()));
            }
        }
        
        Map<String, ModelBone> bones = new HashMap<>();
        if (root.has("bones")) {
            JsonObject bonesObj = root.getAsJsonObject("bones");
            for (String boneName : bonesObj.keySet()) {
                bones.put(boneName, parseBone(boneName, bonesObj.getAsJsonObject(boneName)));
            }
        }
        
        return new BlockyModel(id, formatVersion, texture, elements, bones);
    }
    
    private ModelElement parseElement(JsonObject elem) {
        float[] from = parseVec3(elem.getAsJsonArray("from"));
        float[] to = parseVec3(elem.getAsJsonArray("to"));
        
        Map<String, ModelFace> faces = new HashMap<>();
        if (elem.has("faces")) {
            JsonObject facesObj = elem.getAsJsonObject("faces");
            for (String faceName : facesObj.keySet()) {
                faces.put(faceName, parseFace(facesObj.getAsJsonObject(faceName)));
            }
        }
        
        float[] rotation = null;
        float[] origin = null;
        String axis = null;
        float angle = 0;
        
        if (elem.has("rotation")) {
            JsonObject rot = elem.getAsJsonObject("rotation");
            origin = parseVec3(rot.getAsJsonArray("origin"));
            axis = rot.get("axis").getAsString();
            angle = rot.get("angle").getAsFloat();
        }
        
        String name = elem.has("name") ? elem.get("name").getAsString() : null;
        String parent = elem.has("parent") ? elem.get("parent").getAsString() : null;
        
        return new ModelElement(name, parent, from, to, faces, origin, axis, angle);
    }
    
    private ModelBone parseBone(String name, JsonObject bone) {
        float[] pivot = bone.has("pivot") ? parseVec3(bone.getAsJsonArray("pivot")) : new float[]{0, 0, 0};
        float[] rotation = bone.has("rotation") ? parseVec3(bone.getAsJsonArray("rotation")) : new float[]{0, 0, 0};
        String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
        
        return new ModelBone(name, parent, pivot, rotation);
    }
    
    private ModelFace parseFace(JsonObject face) {
        float[] uv = parseVec4(face.getAsJsonArray("uv"));
        String texture = face.has("texture") ? face.get("texture").getAsString() : null;
        int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
        
        return new ModelFace(uv, texture, rotation);
    }
    
    private float[] parseVec3(JsonArray arr) {
        return new float[]{
            arr.get(0).getAsFloat(),
            arr.get(1).getAsFloat(),
            arr.get(2).getAsFloat()
        };
    }
    
    private float[] parseVec4(JsonArray arr) {
        return new float[]{
            arr.get(0).getAsFloat(),
            arr.get(1).getAsFloat(),
            arr.get(2).getAsFloat(),
            arr.get(3).getAsFloat()
        };
    }
    
    public void registerModel(BlockyModel model) {
        models.put(model.getId(), model);
    }
    
    public Optional<BlockyModel> getModel(String modelId) {
        return Optional.ofNullable(models.get(modelId));
    }
    
    public void registerGroup(ModelGroup group) {
        groups.put(group.getId(), group);
    }
    
    public Optional<ModelGroup> getGroup(String groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }
    
    public void unloadModel(String modelId) {
        models.remove(modelId);
    }
    
    public void clearCache() {
        models.clear();
    }
    
    public void shutdown() {
        loadExecutor.shutdown();
    }
}
