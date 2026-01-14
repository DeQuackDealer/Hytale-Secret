package rubidium.i18n;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Language pack with translations.
 */
public class LanguagePack {
    
    private final String code;
    private String displayName;
    private final Map<String, String> messages;
    
    public LanguagePack(String code) {
        this.code = code;
        this.displayName = code;
        this.messages = new ConcurrentHashMap<>();
    }
    
    public LanguagePack(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
        this.messages = new ConcurrentHashMap<>();
    }
    
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name; }
    
    public LanguagePack addMessage(String key, String message) {
        messages.put(key, message);
        return this;
    }
    
    public String getMessage(String key) {
        return messages.get(key);
    }
    
    public boolean hasMessage(String key) {
        return messages.containsKey(key);
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    public Map<String, String> getAllMessages() {
        return Collections.unmodifiableMap(messages);
    }
}
