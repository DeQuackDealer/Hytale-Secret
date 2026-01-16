package rubidium.api.message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageAPI {
    
    private static final Map<String, String> messages = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> localizedMessages = new ConcurrentHashMap<>();
    private static String defaultLocale = "en";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");
    private static final Map<String, String> colorCodes = new HashMap<>();
    
    static {
        colorCodes.put("black", "\u00A70");
        colorCodes.put("dark_blue", "\u00A71");
        colorCodes.put("dark_green", "\u00A72");
        colorCodes.put("dark_aqua", "\u00A73");
        colorCodes.put("dark_red", "\u00A74");
        colorCodes.put("dark_purple", "\u00A75");
        colorCodes.put("gold", "\u00A76");
        colorCodes.put("gray", "\u00A77");
        colorCodes.put("dark_gray", "\u00A78");
        colorCodes.put("blue", "\u00A79");
        colorCodes.put("green", "\u00A7a");
        colorCodes.put("aqua", "\u00A7b");
        colorCodes.put("red", "\u00A7c");
        colorCodes.put("light_purple", "\u00A7d");
        colorCodes.put("yellow", "\u00A7e");
        colorCodes.put("white", "\u00A7f");
        colorCodes.put("bold", "\u00A7l");
        colorCodes.put("italic", "\u00A7o");
        colorCodes.put("underline", "\u00A7n");
        colorCodes.put("strikethrough", "\u00A7m");
        colorCodes.put("obfuscated", "\u00A7k");
        colorCodes.put("reset", "\u00A7r");
    }
    
    private MessageAPI() {}
    
    public static void register(String key, String message) {
        messages.put(key, message);
    }
    
    public static void register(String key, String locale, String message) {
        localizedMessages.computeIfAbsent(locale, k -> new ConcurrentHashMap<>()).put(key, message);
    }
    
    public static void registerAll(Map<String, String> messages) {
        MessageAPI.messages.putAll(messages);
    }
    
    public static void setDefaultLocale(String locale) {
        defaultLocale = locale;
    }
    
    public static String getDefaultLocale() {
        return defaultLocale;
    }
    
    public static String get(String key) {
        return messages.getOrDefault(key, key);
    }
    
    public static String get(String key, String locale) {
        Map<String, String> localeMessages = localizedMessages.get(locale);
        if (localeMessages != null && localeMessages.containsKey(key)) {
            return localeMessages.get(key);
        }
        return get(key);
    }
    
    public static String format(String key, Object... args) {
        String message = get(key);
        return String.format(message, args);
    }
    
    public static String format(String key, Map<String, Object> placeholders) {
        String message = get(key);
        return replacePlaceholders(message, placeholders);
    }
    
    public static String replacePlaceholders(String message, Map<String, Object> placeholders) {
        if (message == null || placeholders == null) return message;
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = placeholders.get(placeholder);
            matcher.appendReplacement(result, value != null ? Matcher.quoteReplacement(String.valueOf(value)) : "{" + placeholder + "}");
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    public static String colorize(String message) {
        if (message == null) return null;
        
        message = message.replace("&0", "\u00A70");
        message = message.replace("&1", "\u00A71");
        message = message.replace("&2", "\u00A72");
        message = message.replace("&3", "\u00A73");
        message = message.replace("&4", "\u00A74");
        message = message.replace("&5", "\u00A75");
        message = message.replace("&6", "\u00A76");
        message = message.replace("&7", "\u00A77");
        message = message.replace("&8", "\u00A78");
        message = message.replace("&9", "\u00A79");
        message = message.replace("&a", "\u00A7a");
        message = message.replace("&b", "\u00A7b");
        message = message.replace("&c", "\u00A7c");
        message = message.replace("&d", "\u00A7d");
        message = message.replace("&e", "\u00A7e");
        message = message.replace("&f", "\u00A7f");
        message = message.replace("&l", "\u00A7l");
        message = message.replace("&o", "\u00A7o");
        message = message.replace("&n", "\u00A7n");
        message = message.replace("&m", "\u00A7m");
        message = message.replace("&k", "\u00A7k");
        message = message.replace("&r", "\u00A7r");
        
        return message;
    }
    
    public static String stripColors(String message) {
        if (message == null) return null;
        return message.replaceAll("\u00A7[0-9a-fk-or]", "");
    }
    
    public static String color(String colorName) {
        return colorCodes.getOrDefault(colorName.toLowerCase(), "");
    }
    
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }
    
    public static MessageBuilder builder(String text) {
        return new MessageBuilder().text(text);
    }
    
    public static class MessageBuilder {
        private final StringBuilder message = new StringBuilder();
        
        public MessageBuilder text(String text) { message.append(text); return this; }
        public MessageBuilder black(String text) { message.append("\u00A70").append(text); return this; }
        public MessageBuilder darkBlue(String text) { message.append("\u00A71").append(text); return this; }
        public MessageBuilder darkGreen(String text) { message.append("\u00A72").append(text); return this; }
        public MessageBuilder darkAqua(String text) { message.append("\u00A73").append(text); return this; }
        public MessageBuilder darkRed(String text) { message.append("\u00A74").append(text); return this; }
        public MessageBuilder darkPurple(String text) { message.append("\u00A75").append(text); return this; }
        public MessageBuilder gold(String text) { message.append("\u00A76").append(text); return this; }
        public MessageBuilder gray(String text) { message.append("\u00A77").append(text); return this; }
        public MessageBuilder darkGray(String text) { message.append("\u00A78").append(text); return this; }
        public MessageBuilder blue(String text) { message.append("\u00A79").append(text); return this; }
        public MessageBuilder green(String text) { message.append("\u00A7a").append(text); return this; }
        public MessageBuilder aqua(String text) { message.append("\u00A7b").append(text); return this; }
        public MessageBuilder red(String text) { message.append("\u00A7c").append(text); return this; }
        public MessageBuilder lightPurple(String text) { message.append("\u00A7d").append(text); return this; }
        public MessageBuilder yellow(String text) { message.append("\u00A7e").append(text); return this; }
        public MessageBuilder white(String text) { message.append("\u00A7f").append(text); return this; }
        public MessageBuilder bold() { message.append("\u00A7l"); return this; }
        public MessageBuilder italic() { message.append("\u00A7o"); return this; }
        public MessageBuilder underline() { message.append("\u00A7n"); return this; }
        public MessageBuilder strikethrough() { message.append("\u00A7m"); return this; }
        public MessageBuilder obfuscated() { message.append("\u00A7k"); return this; }
        public MessageBuilder reset() { message.append("\u00A7r"); return this; }
        public MessageBuilder newLine() { message.append("\n"); return this; }
        public MessageBuilder space() { message.append(" "); return this; }
        
        public String build() { return message.toString(); }
        
        @Override
        public String toString() { return build(); }
    }
    
    public static class ActionBar {
        private final String message;
        
        public ActionBar(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
        
        public static ActionBar of(String message) {
            return new ActionBar(colorize(message));
        }
    }
    
    public static class Title {
        private final String title;
        private final String subtitle;
        private final int fadeIn;
        private final int stay;
        private final int fadeOut;
        
        public Title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            this.title = colorize(title);
            this.subtitle = colorize(subtitle);
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }
        
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public int getFadeIn() { return fadeIn; }
        public int getStay() { return stay; }
        public int getFadeOut() { return fadeOut; }
        
        public static Title of(String title) {
            return new Title(title, "", 10, 70, 20);
        }
        
        public static Title of(String title, String subtitle) {
            return new Title(title, subtitle, 10, 70, 20);
        }
        
        public static Title of(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            return new Title(title, subtitle, fadeIn, stay, fadeOut);
        }
    }
    
    public record ClickableMessage(
        String text,
        ClickAction action,
        String actionValue,
        String hoverText
    ) {
        public enum ClickAction {
            RUN_COMMAND,
            SUGGEST_COMMAND,
            OPEN_URL,
            COPY_TO_CLIPBOARD
        }
        
        public static ClickableMessage command(String text, String command) {
            return new ClickableMessage(text, ClickAction.RUN_COMMAND, command, null);
        }
        
        public static ClickableMessage suggest(String text, String suggestion) {
            return new ClickableMessage(text, ClickAction.SUGGEST_COMMAND, suggestion, null);
        }
        
        public static ClickableMessage url(String text, String url) {
            return new ClickableMessage(text, ClickAction.OPEN_URL, url, null);
        }
        
        public static ClickableMessage copy(String text, String copyText) {
            return new ClickableMessage(text, ClickAction.COPY_TO_CLIPBOARD, copyText, null);
        }
        
        public ClickableMessage withHover(String hoverText) {
            return new ClickableMessage(text, action, actionValue, hoverText);
        }
    }
}
