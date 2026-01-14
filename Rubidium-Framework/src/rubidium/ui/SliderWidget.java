package rubidium.ui;

import java.util.function.Consumer;

public class SliderWidget extends Widget {
    private float value = 0f;
    private float min = 0f;
    private float max = 100f;
    private float step = 1f;
    private String trackColor = "#333333";
    private String fillColor = "#4A90D9";
    private String thumbColor = "#FFFFFF";
    private boolean showValue = true;
    private Consumer<Float> onChange;
    
    public SliderWidget(String id) { super(id); setSize(200, 20); }
    
    @Override public String getType() { return "slider"; }
    
    public float getValue() { return value; }
    public SliderWidget setValue(float value) { this.value = Math.max(min, Math.min(max, value)); setProperty("value", this.value); return this; }
    
    public float getMin() { return min; }
    public SliderWidget setMin(float min) { this.min = min; setProperty("min", min); return this; }
    
    public float getMax() { return max; }
    public SliderWidget setMax(float max) { this.max = max; setProperty("max", max); return this; }
    
    public SliderWidget setRange(float min, float max) { this.min = min; this.max = max; return this; }
    
    public float getStep() { return step; }
    public SliderWidget setStep(float step) { this.step = step; setProperty("step", step); return this; }
    
    public String getTrackColor() { return trackColor; }
    public SliderWidget setTrackColor(String color) { this.trackColor = color; setProperty("trackColor", color); return this; }
    
    public String getFillColor() { return fillColor; }
    public SliderWidget setFillColor(String color) { this.fillColor = color; setProperty("fillColor", color); return this; }
    
    public String getThumbColor() { return thumbColor; }
    public SliderWidget setThumbColor(String color) { this.thumbColor = color; setProperty("thumbColor", color); return this; }
    
    public boolean isShowValue() { return showValue; }
    public SliderWidget setShowValue(boolean show) { this.showValue = show; setProperty("showValue", show); return this; }
    
    public SliderWidget onChange(Consumer<Float> handler) { this.onChange = handler; on("value_change", e -> handler.accept(Float.parseFloat(e.value()))); return this; }
}
