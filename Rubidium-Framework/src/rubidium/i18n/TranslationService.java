package rubidium.i18n;

import rubidium.hytale.api.player.Player;
import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-language translation system.
 */
public class TranslationService {
    
    private static TranslationService instance;
    
    private final Map<String, LanguagePack> languages;
    private final Map<UUID, String> playerLanguages;
    private String defaultLanguage = "en";
    private final Gson gson = new Gson();
    
    private TranslationService() {
        this.languages = new ConcurrentHashMap<>();
        this.playerLanguages = new ConcurrentHashMap<>();
    }
    
    public static TranslationService getInstance() {
        if (instance == null) {
            instance = new TranslationService();
        }
        return instance;
    }
    
    public void loadLanguage(String code, Path file) throws IOException {
        String content = Files.readString(file);
        JsonObject json = JsonParser.parseString(content).getAsJsonObject();
        
        LanguagePack pack = new LanguagePack(code);
        
        if (json.has("messages")) {
            JsonObject messages = json.getAsJsonObject("messages");
            for (String key : messages.keySet()) {
                pack.addMessage(key, messages.get(key).getAsString());
            }
        }
        
        languages.put(code, pack);
    }
    
    public void registerLanguage(LanguagePack pack) {
        languages.put(pack.getCode(), pack);
    }
    
    public void setDefaultLanguage(String code) {
        this.defaultLanguage = code;
    }
    
    public void setPlayerLanguage(Player player, String code) {
        playerLanguages.put(player.getUuid(), code);
    }
    
    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUuid(), defaultLanguage);
    }
    
    public Message getMessage(String key) {
        return getMessage(key, defaultLanguage);
    }
    
    public Message getMessage(String key, String languageCode) {
        LanguagePack pack = languages.get(languageCode);
        if (pack == null) {
            pack = languages.get(defaultLanguage);
        }
        
        if (pack == null) {
            return new Message(key, "Missing translation: " + key);
        }
        
        String message = pack.getMessage(key);
        if (message == null) {
            LanguagePack defaultPack = languages.get(defaultLanguage);
            if (defaultPack != null) {
                message = defaultPack.getMessage(key);
            }
        }
        
        if (message == null) {
            return new Message(key, "Missing translation: " + key);
        }
        
        return new Message(key, message);
    }
    
    public Message getMessage(Player player, String key) {
        String lang = getPlayerLanguage(player);
        return getMessage(key, lang);
    }
    
    public Collection<LanguagePack> getLanguages() {
        return languages.values();
    }
    
    public Optional<LanguagePack> getLanguage(String code) {
        return Optional.ofNullable(languages.get(code));
    }
}
