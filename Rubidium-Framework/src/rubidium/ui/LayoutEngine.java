package rubidium.ui;

import java.util.List;

/**
 * Layout engine for rendering UI screens.
 */
public class LayoutEngine {
    
    public RubidiumUI.UIRenderPacket render(UIScreen screen, PlayerUIState state, Theme theme) {
        List<RubidiumUI.WidgetData> widgetData = screen.toWidgetData();
        
        return new RubidiumUI.UIRenderPacket(
            screen.getId(),
            widgetData,
            theme
        );
    }
    
    public void calculateLayout(UIScreen screen, int screenWidth, int screenHeight) {
        for (Widget widget : screen.getWidgets()) {
            layoutWidget(widget, 0, 0, screenWidth, screenHeight);
        }
    }
    
    private void layoutWidget(Widget widget, int parentX, int parentY, int availableWidth, int availableHeight) {
        int childY = widget.getY();
        for (Widget child : widget.getChildren()) {
            layoutWidget(child, widget.getX(), childY, widget.getWidth(), widget.getHeight());
            childY += child.getHeight();
        }
    }
}
