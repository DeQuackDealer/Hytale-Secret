package rubidium.chat.filters;

import rubidium.chat.ChatFilter;
import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.regex.*;

/**
 * Profanity filter that blocks or censors bad words.
 */
public class ProfanityFilter implements ChatFilter {
    
    private final Set<String> blockedWords;
    private final boolean censorMode;
    private final char censorChar;
    
    public ProfanityFilter(boolean censorMode) {
        this.blockedWords = new HashSet<>();
        this.censorMode = censorMode;
        this.censorChar = '*';
    }
    
    public ProfanityFilter addWord(String word) {
        blockedWords.add(word.toLowerCase());
        return this;
    }
    
    public ProfanityFilter addWords(String... words) {
        for (String word : words) {
            blockedWords.add(word.toLowerCase());
        }
        return this;
    }
    
    @Override
    public String getName() { return "Profanity Filter"; }
    
    @Override
    public Result filter(Player sender, String message) {
        String lowerMessage = message.toLowerCase();
        
        for (String word : blockedWords) {
            if (lowerMessage.contains(word)) {
                if (censorMode) {
                    String replacement = String.valueOf(censorChar).repeat(word.length());
                    Pattern pattern = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE);
                    message = pattern.matcher(message).replaceAll(replacement);
                } else {
                    return Result.block("Message contains inappropriate language");
                }
            }
        }
        
        return Result.allow(message);
    }
}
