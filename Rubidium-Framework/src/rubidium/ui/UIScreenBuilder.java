package rubidium.ui;

import java.util.function.Consumer;

/**
 * Fluent builder for creating UI screens.
 */
public class UIScreenBuilder {
    
    private final UIScreen screen;
    
    public UIScreenBuilder(String id) {
        this.screen = new UIScreen(id);
    }
    
    public UIScreenBuilder title(String title) {
        screen.setTitle(title);
        return this;
    }
    
    public UIScreenBuilder layer(UIScreen.ScreenLayer layer) {
        screen.setLayer(layer);
        return this;
    }
    
    public UIScreenBuilder pausesGame(boolean pauses) {
        screen.setPausesGame(pauses);
        return this;
    }
    
    public UIScreenBuilder capturesMouse(boolean captures) {
        screen.setCapturesMouse(captures);
        return this;
    }
    
    public UIScreenBuilder widget(Widget widget) {
        screen.addWidget(widget);
        return this;
    }
    
    public UIScreenBuilder text(String id, String text, Consumer<TextWidget> config) {
        TextWidget widget = new TextWidget(id).setText(text);
        if (config != null) config.accept(widget);
        screen.addWidget(widget);
        return this;
    }
    
    public UIScreenBuilder button(String id, String label, Consumer<ButtonWidget> config) {
        ButtonWidget widget = new ButtonWidget(id).setLabel(label);
        if (config != null) config.accept(widget);
        screen.addWidget(widget);
        return this;
    }
    
    public UIScreenBuilder panel(String id, Consumer<PanelWidget> config) {
        PanelWidget widget = new PanelWidget(id);
        if (config != null) config.accept(widget);
        screen.addWidget(widget);
        return this;
    }
    
    public UIScreenBuilder progress(String id, Consumer<ProgressWidget> config) {
        ProgressWidget widget = new ProgressWidget(id);
        if (config != null) config.accept(widget);
        screen.addWidget(widget);
        return this;
    }
    
    public UIScreenBuilder slider(String id, Consumer<SliderWidget> config) {
        SliderWidget widget = new SliderWidget(id);
        if (config != null) config.accept(widget);
        screen.addWidget(widget);
        return this;
    }
    
    public UIScreen build() {
        return screen;
    }
}
