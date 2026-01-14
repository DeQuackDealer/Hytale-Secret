package rubidium.chat.tabcomplete;

import rubidium.hytale.api.player.Player;
import rubidium.core.RubidiumLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class TabCompleteService {
    
    public record Suggestion(
        String text,
        String tooltip,
        SuggestionType type,
        int priority
    ) implements Comparable<Suggestion> {
        @Override
        public int compareTo(Suggestion other) {
            return Integer.compare(other.priority, this.priority);
        }
    }
    
    public enum SuggestionType {
        PLAYER,
        COMMAND,
        ARGUMENT,
        EMOJI,
        CHANNEL,
        CUSTOM
    }
    
    public record TabCompleteRequest(
        Player player,
        String input,
        int cursorPosition,
        boolean isCommand
    ) {}
    
    public record TabCompleteResult(
        List<Suggestion> suggestions,
        int replacementStart,
        int replacementEnd
    ) {
        public static TabCompleteResult empty() {
            return new TabCompleteResult(List.of(), 0, 0);
        }
    }
    
    @FunctionalInterface
    public interface SuggestionProvider {
        List<Suggestion> suggest(TabCompleteRequest request, String currentWord);
    }
    
    private final RubidiumLogger logger;
    private final TrieIndex<String> playerTrie = new TrieIndex<>();
    private final TrieIndex<String> commandTrie = new TrieIndex<>();
    private final Map<String, TrieIndex<String>> argumentTries = new ConcurrentHashMap<>();
    private final List<SuggestionProvider> customProviders = new CopyOnWriteArrayList<>();
    
    private Supplier<Collection<String>> onlinePlayersSupplier;
    private BiFunction<Player, String, Boolean> permissionChecker;
    
    private volatile int maxSuggestions = 50;
    private volatile long cacheExpireMs = 5000;
    private volatile boolean fuzzyMatchEnabled = true;
    
    public TabCompleteService(RubidiumLogger logger) {
        this.logger = logger;
    }
    
    public void setOnlinePlayersSupplier(Supplier<Collection<String>> supplier) {
        this.onlinePlayersSupplier = supplier;
    }
    
    public void setPermissionChecker(BiFunction<Player, String, Boolean> checker) {
        this.permissionChecker = checker;
    }
    
    public void registerCommand(String command, String permission, String... aliases) {
        commandTrie.insert(command.toLowerCase(), permission);
        for (var alias : aliases) {
            commandTrie.insert(alias.toLowerCase(), permission);
        }
    }
    
    public void registerArguments(String command, int argIndex, String... validArgs) {
        var key = command.toLowerCase() + ":" + argIndex;
        var trie = argumentTries.computeIfAbsent(key, k -> new TrieIndex<>());
        for (var arg : validArgs) {
            trie.insert(arg.toLowerCase(), arg);
        }
    }
    
    public void registerProvider(SuggestionProvider provider) {
        customProviders.add(provider);
    }
    
    public TabCompleteResult complete(TabCompleteRequest request) {
        var input = request.input();
        var cursor = Math.min(request.cursorPosition(), input.length());
        
        var wordStart = findWordStart(input, cursor);
        var wordEnd = findWordEnd(input, cursor);
        var currentWord = input.substring(wordStart, wordEnd);
        
        List<Suggestion> suggestions;
        
        if (request.isCommand()) {
            suggestions = completeCommand(request, currentWord, wordStart);
        } else {
            suggestions = completeChat(request, currentWord);
        }
        
        for (var provider : customProviders) {
            try {
                suggestions.addAll(provider.suggest(request, currentWord));
            } catch (Exception e) {
                logger.warning("Custom tab provider failed: " + e.getMessage());
            }
        }
        
        suggestions = suggestions.stream()
            .distinct()
            .sorted()
            .limit(maxSuggestions)
            .toList();
        
        return new TabCompleteResult(suggestions, wordStart, wordEnd);
    }
    
    private List<Suggestion> completeCommand(TabCompleteRequest request, String currentWord, int wordStart) {
        var suggestions = new ArrayList<Suggestion>();
        var input = request.input();
        
        if (wordStart == 0 || (wordStart == 1 && input.startsWith("/"))) {
            var searchWord = currentWord.startsWith("/") ? currentWord.substring(1) : currentWord;
            var matches = commandTrie.findByPrefix(searchWord.toLowerCase());
            
            for (var match : matches) {
                var permission = commandTrie.getValue(match);
                if (permission == null || hasPermission(request.player(), permission)) {
                    suggestions.add(new Suggestion(
                        "/" + match,
                        "Command",
                        SuggestionType.COMMAND,
                        100
                    ));
                }
            }
        } else {
            var parts = input.split("\\s+");
            var command = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
            var argIndex = parts.length - 1;
            
            if (input.endsWith(" ")) {
                argIndex = parts.length;
            }
            
            suggestions.addAll(getArgumentSuggestions(command, argIndex, currentWord));
            
            if (currentWord.startsWith("@") || !currentWord.isEmpty()) {
                suggestions.addAll(getPlayerSuggestions(currentWord));
            }
        }
        
        return suggestions;
    }
    
    private List<Suggestion> completeChat(TabCompleteRequest request, String currentWord) {
        var suggestions = new ArrayList<Suggestion>();
        
        if (currentWord.startsWith("@")) {
            var searchTerm = currentWord.substring(1);
            suggestions.addAll(getPlayerSuggestions(searchTerm).stream()
                .map(s -> new Suggestion(
                    "@" + s.text(),
                    "Mention " + s.text(),
                    SuggestionType.PLAYER,
                    s.priority()
                ))
                .toList());
        }
        
        if (currentWord.startsWith(":")) {
            suggestions.addAll(getEmojiSuggestions(currentWord.substring(1)));
        }
        
        return suggestions;
    }
    
    private List<Suggestion> getPlayerSuggestions(String prefix) {
        if (onlinePlayersSupplier == null) {
            return List.of();
        }
        
        var onlinePlayers = onlinePlayersSupplier.get();
        var lowerPrefix = prefix.toLowerCase();
        
        return onlinePlayers.stream()
            .filter(name -> name.toLowerCase().startsWith(lowerPrefix))
            .map(name -> new Suggestion(
                name,
                "Player: " + name,
                SuggestionType.PLAYER,
                80
            ))
            .limit(maxSuggestions)
            .collect(Collectors.toList());
    }
    
    private List<Suggestion> getArgumentSuggestions(String command, int argIndex, String currentWord) {
        var key = command.toLowerCase() + ":" + argIndex;
        var trie = argumentTries.get(key);
        
        if (trie == null) {
            return List.of();
        }
        
        var matches = trie.findByPrefix(currentWord.toLowerCase());
        
        return matches.stream()
            .map(match -> new Suggestion(
                match,
                "Argument",
                SuggestionType.ARGUMENT,
                60
            ))
            .collect(Collectors.toList());
    }
    
    private List<Suggestion> getEmojiSuggestions(String prefix) {
        var emojis = Map.of(
            "smile", ":)",
            "sad", ":(",
            "heart", "<3",
            "star", "*",
            "fire", "🔥",
            "thumbsup", "👍",
            "check", "✓",
            "cross", "✗"
        );
        
        return emojis.entrySet().stream()
            .filter(e -> e.getKey().startsWith(prefix.toLowerCase()))
            .map(e -> new Suggestion(
                ":" + e.getKey() + ":",
                e.getValue(),
                SuggestionType.EMOJI,
                40
            ))
            .collect(Collectors.toList());
    }
    
    private boolean hasPermission(Player player, String permission) {
        if (permissionChecker == null) return true;
        return permissionChecker.apply(player, permission);
    }
    
    private int findWordStart(String input, int cursor) {
        for (int i = cursor - 1; i >= 0; i--) {
            if (Character.isWhitespace(input.charAt(i))) {
                return i + 1;
            }
        }
        return 0;
    }
    
    private int findWordEnd(String input, int cursor) {
        for (int i = cursor; i < input.length(); i++) {
            if (Character.isWhitespace(input.charAt(i))) {
                return i;
            }
        }
        return input.length();
    }
    
    public void setMaxSuggestions(int max) {
        this.maxSuggestions = max;
    }
    
    public void setFuzzyMatchEnabled(boolean enabled) {
        this.fuzzyMatchEnabled = enabled;
    }
    
    public void updatePlayerCache() {
        playerTrie.clear();
        if (onlinePlayersSupplier != null) {
            for (var name : onlinePlayersSupplier.get()) {
                playerTrie.insert(name.toLowerCase(), name);
            }
        }
    }
    
    public static final class TrieIndex<V> {
        private final Node<V> root = new Node<>();
        
        private static class Node<V> {
            final Map<Character, Node<V>> children = new ConcurrentHashMap<>();
            V value;
            boolean isEndOfWord;
        }
        
        public void insert(String key, V value) {
            var node = root;
            for (char c : key.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new Node<>());
            }
            node.isEndOfWord = true;
            node.value = value;
        }
        
        public V getValue(String key) {
            var node = root;
            for (char c : key.toCharArray()) {
                node = node.children.get(c);
                if (node == null) return null;
            }
            return node.isEndOfWord ? node.value : null;
        }
        
        public List<String> findByPrefix(String prefix) {
            var results = new ArrayList<String>();
            var node = root;
            
            for (char c : prefix.toCharArray()) {
                node = node.children.get(c);
                if (node == null) return results;
            }
            
            collectWords(node, new StringBuilder(prefix), results);
            return results;
        }
        
        private void collectWords(Node<V> node, StringBuilder current, List<String> results) {
            if (node.isEndOfWord) {
                results.add(current.toString());
            }
            
            for (var entry : node.children.entrySet()) {
                current.append(entry.getKey());
                collectWords(entry.getValue(), current, results);
                current.deleteCharAt(current.length() - 1);
            }
        }
        
        public void clear() {
            root.children.clear();
        }
    }
}
