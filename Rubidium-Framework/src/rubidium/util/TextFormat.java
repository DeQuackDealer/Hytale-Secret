package rubidium.util;

import java.util.regex.Pattern;

public final class TextFormat {
    
    public static final char COLOR_CHAR = '\u00A7';
    public static final char ALT_COLOR_CHAR = '&';
    
    public static final String BLACK = COLOR_CHAR + "0";
    public static final String DARK_BLUE = COLOR_CHAR + "1";
    public static final String DARK_GREEN = COLOR_CHAR + "2";
    public static final String DARK_AQUA = COLOR_CHAR + "3";
    public static final String DARK_RED = COLOR_CHAR + "4";
    public static final String DARK_PURPLE = COLOR_CHAR + "5";
    public static final String GOLD = COLOR_CHAR + "6";
    public static final String GRAY = COLOR_CHAR + "7";
    public static final String DARK_GRAY = COLOR_CHAR + "8";
    public static final String BLUE = COLOR_CHAR + "9";
    public static final String GREEN = COLOR_CHAR + "a";
    public static final String AQUA = COLOR_CHAR + "b";
    public static final String RED = COLOR_CHAR + "c";
    public static final String LIGHT_PURPLE = COLOR_CHAR + "d";
    public static final String YELLOW = COLOR_CHAR + "e";
    public static final String WHITE = COLOR_CHAR + "f";
    
    public static final String OBFUSCATED = COLOR_CHAR + "k";
    public static final String BOLD = COLOR_CHAR + "l";
    public static final String STRIKETHROUGH = COLOR_CHAR + "m";
    public static final String UNDERLINE = COLOR_CHAR + "n";
    public static final String ITALIC = COLOR_CHAR + "o";
    public static final String RESET = COLOR_CHAR + "r";
    
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-OR]");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    private TextFormat() {}
    
    public static String colorize(String text) {
        if (text == null) return null;
        
        text = HEX_PATTERN.matcher(text).replaceAll(m -> {
            StringBuilder hex = new StringBuilder(COLOR_CHAR + "x");
            for (char c : m.group(1).toCharArray()) {
                hex.append(COLOR_CHAR).append(c);
            }
            return hex.toString();
        });
        
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == ALT_COLOR_CHAR && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
    
    public static String stripColor(String text) {
        if (text == null) return null;
        return STRIP_COLOR_PATTERN.matcher(text).replaceAll("");
    }
    
    public static String translateAlternateColorCodes(char altColorChar, String text) {
        if (text == null) return null;
        
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
    
    public static String getLastColors(String text) {
        if (text == null) return "";
        
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        for (int i = length - 1; i > -1; i--) {
            char section = text.charAt(i);
            if (section == COLOR_CHAR && i < length - 1) {
                char c = text.charAt(i + 1);
                if ("0123456789abcdefABCDEF".indexOf(c) > -1) {
                    result.insert(0, "" + COLOR_CHAR + c);
                    break;
                } else if ("klmnoKLMNO".indexOf(c) > -1) {
                    result.insert(0, "" + COLOR_CHAR + c);
                } else if (c == 'r' || c == 'R') {
                    break;
                }
            }
        }
        
        return result.toString();
    }
    
    public static String center(String text, int lineLength) {
        if (text == null || text.isEmpty()) return text;
        
        String stripped = stripColor(text);
        int spaces = (lineLength - stripped.length()) / 2;
        
        if (spaces <= 0) return text;
        
        return " ".repeat(spaces) + text;
    }
    
    public static String repeat(String text, int count) {
        return text.repeat(count);
    }
    
    public static String progressBar(double progress, int length, char filled, char empty, String filledColor, String emptyColor) {
        int filledCount = (int) (progress * length);
        int emptyCount = length - filledCount;
        
        return colorize(filledColor + repeat(String.valueOf(filled), filledCount) 
                      + emptyColor + repeat(String.valueOf(empty), emptyCount));
    }
    
    public static String gradient(String text, int startColor, int endColor) {
        if (text == null || text.isEmpty()) return text;
        
        int r1 = (startColor >> 16) & 0xFF;
        int g1 = (startColor >> 8) & 0xFF;
        int b1 = startColor & 0xFF;
        
        int r2 = (endColor >> 16) & 0xFF;
        int g2 = (endColor >> 8) & 0xFF;
        int b2 = endColor & 0xFF;
        
        StringBuilder result = new StringBuilder();
        int len = text.length();
        
        for (int i = 0; i < len; i++) {
            float ratio = (float) i / (len - 1);
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);
            
            result.append(COLOR_CHAR).append("x");
            result.append(COLOR_CHAR).append(Integer.toHexString((r >> 4) & 0xF));
            result.append(COLOR_CHAR).append(Integer.toHexString(r & 0xF));
            result.append(COLOR_CHAR).append(Integer.toHexString((g >> 4) & 0xF));
            result.append(COLOR_CHAR).append(Integer.toHexString(g & 0xF));
            result.append(COLOR_CHAR).append(Integer.toHexString((b >> 4) & 0xF));
            result.append(COLOR_CHAR).append(Integer.toHexString(b & 0xF));
            result.append(text.charAt(i));
        }
        
        return result.toString();
    }
}
