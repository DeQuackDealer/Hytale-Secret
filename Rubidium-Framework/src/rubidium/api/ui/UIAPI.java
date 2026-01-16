package rubidium.api.ui;

import rubidium.api.ui.components.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class UIAPI {
    
    private static final Map<String, UIScreen> screens = new ConcurrentHashMap<>();
    private static final Map<Object, String> playerScreens = new ConcurrentHashMap<>();
    
    private UIAPI() {}
    
    public static UIScreen screen(String id) {
        return screens.computeIfAbsent(id, UIScreen::new);
    }
    
    public static UIScreen createScreen(String id, String title) {
        UIScreen screen = new UIScreen(id).title(title);
        screens.put(id, screen);
        return screen;
    }
    
    public static void open(Object player, String screenId) {
        playerScreens.put(player, screenId);
    }
    
    public static void open(Object player, UIScreen screen) {
        screens.put(screen.getId(), screen);
        playerScreens.put(player, screen.getId());
    }
    
    public static void close(Object player) {
        playerScreens.remove(player);
    }
    
    public static Optional<UIScreen> getScreen(String id) {
        return Optional.ofNullable(screens.get(id));
    }
    
    public static UIButton button(String id) {
        return new UIButton(id);
    }
    
    public static UIButton button(String id, String text) {
        return new UIButton(id).text(text);
    }
    
    public static UILabel label(String id, String text) {
        return new UILabel(id, text);
    }
    
    public static UIPanel panel(String id) {
        return new UIPanel(id);
    }
    
    public static UIPanel panel(String id, int width, int height) {
        return new UIPanel(id).setSize(width, height);
    }
    
    public static UIImage image(String id, String texture) {
        return new UIImage(id, texture);
    }
    
    public static UISlot slot(String id, int index) {
        return new UISlot(id, index);
    }
    
    public static UIProgressBar progressBar(String id) {
        return new UIProgressBar(id);
    }
    
    public static UITextField textField(String id) {
        return new UITextField(id);
    }
    
    public static UIList list(String id) {
        return new UIList(id);
    }
    
    public static UIGrid grid(String id, int columns, int rows) {
        return new UIGrid(id, columns, rows);
    }
    
    public static UIScreen inventory(String id, int slots) {
        UIScreen screen = createScreen(id, "Inventory");
        UIGrid grid = grid("slots", 9, slots / 9);
        for (int i = 0; i < slots; i++) {
            grid.add(slot("slot_" + i, i));
        }
        return screen.add(grid);
    }
    
    public static UIScreen chest(String id, int rows) {
        return inventory(id, rows * 9).title("Chest");
    }
    
    public static UIScreen craftingTable(String id) {
        UIScreen screen = createScreen(id, "Crafting");
        screen.add(grid("crafting", 3, 3));
        screen.add(slot("result", 0).setPosition(124, 35));
        return screen;
    }
    
    public static UIScreen furnace(String id) {
        UIScreen screen = createScreen(id, "Furnace");
        screen.add(slot("input", 0).setPosition(56, 17));
        screen.add(slot("fuel", 1).setPosition(56, 53));
        screen.add(slot("output", 2).setPosition(116, 35));
        screen.add(progressBar("progress").setPosition(79, 34));
        screen.add(progressBar("fuel").setPosition(56, 36));
        return screen;
    }
    
    public static UIScreen dialog(String id, String title, String message) {
        UIScreen screen = createScreen(id, title);
        screen.add(label("message", message).setPosition(20, 40));
        screen.add(button("ok", "OK").setPosition(60, 100).on("click", e -> close(e.source())));
        return screen;
    }
    
    public static UIScreen confirm(String id, String title, String message, 
                                    Consumer<UIComponent.UIEvent> onConfirm,
                                    Consumer<UIComponent.UIEvent> onCancel) {
        UIScreen screen = createScreen(id, title);
        screen.add(label("message", message).setPosition(20, 40));
        screen.add(button("confirm", "Confirm").setPosition(30, 100).on("click", onConfirm));
        screen.add(button("cancel", "Cancel").setPosition(110, 100).on("click", onCancel));
        return screen;
    }
}
