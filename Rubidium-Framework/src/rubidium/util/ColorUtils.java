package rubidium.util;

import java.awt.Color;

/**
 * Color manipulation utilities.
 */
public final class ColorUtils {
    
    private ColorUtils() {}
    
    public static String toHex(Color color) {
        return String.format("#%02x%02x%02x",
            color.getRed(),
            color.getGreen(),
            color.getBlue()
        );
    }
    
    public static Color fromHex(String hex) {
        hex = hex.replace("#", "");
        return new Color(
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        );
    }
    
    public static Color lerp(Color start, Color end, double ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        
        int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
        int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
        int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
        
        return new Color(r, g, b);
    }
    
    public static String toMinecraftColor(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        return "§x§" + toHexChar(r >> 4) + "§" + toHexChar(r & 0xF) +
               "§" + toHexChar(g >> 4) + "§" + toHexChar(g & 0xF) +
               "§" + toHexChar(b >> 4) + "§" + toHexChar(b & 0xF);
    }
    
    private static char toHexChar(int value) {
        return "0123456789abcdef".charAt(value & 0xF);
    }
    
    public static String gradient(String text, Color startColor, Color endColor) {
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        for (int i = 0; i < length; i++) {
            double ratio = length > 1 ? (double) i / (length - 1) : 0;
            Color color = lerp(startColor, endColor, ratio);
            result.append(toMinecraftColor(color)).append(text.charAt(i));
        }
        
        return result.toString();
    }
    
    public static String rainbow(String text) {
        Color[] colors = {
            Color.RED, new Color(255, 127, 0), Color.YELLOW,
            Color.GREEN, Color.CYAN, Color.BLUE, new Color(139, 0, 255)
        };
        
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        for (int i = 0; i < length; i++) {
            double position = (double) i / length * (colors.length - 1);
            int index = (int) position;
            double ratio = position - index;
            
            Color color;
            if (index >= colors.length - 1) {
                color = colors[colors.length - 1];
            } else {
                color = lerp(colors[index], colors[index + 1], ratio);
            }
            
            result.append(toMinecraftColor(color)).append(text.charAt(i));
        }
        
        return result.toString();
    }
    
    public static Color darken(Color color, double amount) {
        int r = (int) Math.max(0, color.getRed() * (1 - amount));
        int g = (int) Math.max(0, color.getGreen() * (1 - amount));
        int b = (int) Math.max(0, color.getBlue() * (1 - amount));
        return new Color(r, g, b);
    }
    
    public static Color lighten(Color color, double amount) {
        int r = (int) Math.min(255, color.getRed() + (255 - color.getRed()) * amount);
        int g = (int) Math.min(255, color.getGreen() + (255 - color.getGreen()) * amount);
        int b = (int) Math.min(255, color.getBlue() + (255 - color.getBlue()) * amount);
        return new Color(r, g, b);
    }
}
