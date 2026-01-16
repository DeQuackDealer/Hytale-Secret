package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;

public class UIGrid extends UIComponent {
    
    private int columns;
    private int rows;
    private int cellWidth = 18;
    private int cellHeight = 18;
    private int spacing = 0;
    
    public UIGrid(String id, int columns, int rows) {
        super(id);
        this.columns = columns;
        this.rows = rows;
        this.width = columns * cellWidth + (columns - 1) * spacing;
        this.height = rows * cellHeight + (rows - 1) * spacing;
    }
    
    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public int getCellWidth() { return cellWidth; }
    public int getCellHeight() { return cellHeight; }
    public int getSpacing() { return spacing; }
    
    public UIGrid cellSize(int w, int h) { 
        this.cellWidth = w; 
        this.cellHeight = h; 
        recalculateSize();
        return this; 
    }
    
    public UIGrid spacing(int s) { 
        this.spacing = s; 
        recalculateSize();
        return this; 
    }
    
    private void recalculateSize() {
        this.width = columns * cellWidth + (columns - 1) * spacing;
        this.height = rows * cellHeight + (rows - 1) * spacing;
    }
    
    @Override
    public UIGrid setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    @Override
    public UIGrid add(UIComponent child) { 
        int index = children.size();
        int col = index % columns;
        int row = index / columns;
        child.setPosition(col * (cellWidth + spacing), row * (cellHeight + spacing));
        super.add(child);
        return this; 
    }
}
