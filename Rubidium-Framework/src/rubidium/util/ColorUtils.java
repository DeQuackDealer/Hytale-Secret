package rubidium.util;

import java.awt.Color;

/**
 * Color manipulation utilities for Hytale UI.
 * Uses Hytale's native color formats (hex, ARGB) instead of legacy codes.
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
    
    /**
     * Convert to Hytale color format (hex string for UI components).
     */
    public static String toHytaleColor(Color color) {
        return toHex(color);
    }
    
    /**
     * Convert to ARGB integer for Hytale rendering.
     */
    public static int toARGB(Color color) {
        return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }
    
    /**
     * Convert to ARGB with specified alpha.
     */
    public static int toARGB(Color color, int alpha) {
        return (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }
    
    /**
     * Create color from ARGB integer.
     */
    public static Color fromARGB(int argb) {
        return new Color(
            (argb >> 16) & 0xFF,
            (argb >> 8) & 0xFF,
            argb & 0xFF,
            (argb >> 24) & 0xFF
        );
    }
    
    /**
     * Generate gradient colors for Hytale UI elements.
     */
    public static String[] gradientHex(Color startColor, Color endColor, int steps) {
        String[] result = new String[steps];
        for (int i = 0; i < steps; i++) {
            double ratio = steps > 1 ? (double) i / (steps - 1) : 0;
            result[i] = toHex(lerp(startColor, endColor, ratio));
        }
        return result;
    }
    
    /**
     * Generate rainbow gradient colors.
     */
    public static String[] rainbowHex(int steps) {
        Color[] colors = {
            Color.RED, new Color(255, 127, 0), Color.YELLOW,
            Color.GREEN, Color.CYAN, Color.BLUE, new Color(139, 0, 255)
        };
        
        String[] result = new String[steps];
        for (int i = 0; i < steps; i++) {
            double position = (double) i / steps * (colors.length - 1);
            int index = (int) position;
            double ratio = position - index;
            
            Color color;
            if (index >= colors.length - 1) {
                color = colors[colors.length - 1];
            } else {
                color = lerp(colors[index], colors[index + 1], ratio);
            }
            result[i] = toHex(color);
        }
        return result;
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
    
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
