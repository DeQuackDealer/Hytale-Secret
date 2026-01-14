package rubidium.ui;

public class ProgressWidget extends Widget {
    private float value = 0f;
    private float min = 0f;
    private float max = 100f;
    private String fillColor = "#4A90D9";
    private String backgroundColor = "#333333";
    private boolean showLabel = true;
    private String labelFormat = "{value}%";
    
    public ProgressWidget(String id) { super(id); setSize(200, 20); }
    
    @Override public String getType() { return "progress"; }
    
    public float getValue() { return value; }
    public ProgressWidget setValue(float value) { this.value = Math.max(min, Math.min(max, value)); setProperty("value", this.value); return this; }
    
    public float getMin() { return min; }
    public ProgressWidget setMin(float min) { this.min = min; setProperty("min", min); return this; }
    
    public float getMax() { return max; }
    public ProgressWidget setMax(float max) { this.max = max; setProperty("max", max); return this; }
    
    public ProgressWidget setRange(float min, float max) { this.min = min; this.max = max; return this; }
    
    public float getPercent() { return (value - min) / (max - min) * 100; }
    
    public String getFillColor() { return fillColor; }
    public ProgressWidget setFillColor(String color) { this.fillColor = color; setProperty("fillColor", color); return this; }
    
    public String getBackgroundColor() { return backgroundColor; }
    public ProgressWidget setBackgroundColor(String color) { this.backgroundColor = color; setProperty("backgroundColor", color); return this; }
    
    public boolean isShowLabel() { return showLabel; }
    public ProgressWidget setShowLabel(boolean show) { this.showLabel = show; setProperty("showLabel", show); return this; }
    
    public String getLabelFormat() { return labelFormat; }
    public ProgressWidget setLabelFormat(String format) { this.labelFormat = format; setProperty("labelFormat", format); return this; }
}
