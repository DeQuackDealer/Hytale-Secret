package rubidium.test;

import rubidium.api.ui.*;
import rubidium.api.ui.overlays.*;
import rubidium.features.minimap.MinimapManager;
import rubidium.features.minimap.Waypoint;
import rubidium.features.voicechat.VoiceChatManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UIAPITest {
    
    public static void runAllTests() {
        System.out.println("\n=== Rubidium UI API Tests ===");
        
        testUIState();
        testUICommandBuilder();
        testUIEventBuilder();
        testUIBinder();
        testMinimapOverlay();
        testVoiceChatOverlay();
        testStatsOverlay();
        
        System.out.println("=== All UI API Tests Completed ===\n");
    }
    
    private static void testUIState() {
        UIState state = new UIState();
        
        state.setString("test.text", "Hello");
        state.setBoolean("test.visible", true);
        state.setInt("test.count", 42);
        state.setFloat("test.scale", 1.5f);
        
        boolean pass = 
            "Hello".equals(state.getString("test.text")) &&
            state.getBoolean("test.visible") &&
            state.getInt("test.count") == 42 &&
            Math.abs(state.getFloat("test.scale") - 1.5f) < 0.001f &&
            state.isDirty() &&
            state.getDirtyEntries().size() == 4;
        
        state.clearDirty();
        boolean cleared = !state.isDirty() && state.getDirtyEntries().isEmpty();
        
        System.out.println("  [" + (pass && cleared ? "PASS" : "FAIL") + "] UIState: State management works");
    }
    
    private static void testUICommandBuilder() {
        UICommandBuilder builder = new UICommandBuilder();
        
        builder.set("#Element.text", "Test")
               .set("#Element.visible", true)
               .set("#Slider.value", 0.5f)
               .set("#Counter.count", 10)
               .clear("#Container")
               .append("#List", "item1", "New Item");
        
        CustomUICommand[] commands = builder.getCommands();
        boolean pass = commands.length == 6 && builder.getCommandCount() == 6;
        
        builder.reset();
        boolean reset = builder.getCommandCount() == 0;
        
        System.out.println("  [" + (pass && reset ? "PASS" : "FAIL") + "] UICommandBuilder: Command generation works");
    }
    
    private static void testUIEventBuilder() {
        UIEventBuilder events = new UIEventBuilder();
        AtomicBoolean clicked = new AtomicBoolean(false);
        AtomicBoolean toggled = new AtomicBoolean(false);
        AtomicReference<Float> sliderValue = new AtomicReference<>(0f);
        
        events.onClick("#Button", () -> clicked.set(true));
        events.onToggle("#Toggle", toggled::set);
        events.onSliderChange("#Slider", sliderValue::set);
        
        events.dispatch("#Button", "click", "");
        events.dispatch("#Toggle", "toggle", "true");
        events.dispatch("#Slider", "change", "0.75");
        
        boolean pass = 
            clicked.get() &&
            toggled.get() &&
            Math.abs(sliderValue.get() - 0.75f) < 0.001f &&
            events.getHandlerCount() == 3;
        
        System.out.println("  [" + (pass ? "PASS" : "FAIL") + "] UIEventBuilder: Event handling works");
    }
    
    private static void testUIBinder() {
        UIEventBuilder events = new UIEventBuilder();
        UIBinder binder = new UIBinder(events);
        
        AtomicBoolean clicked = new AtomicBoolean(false);
        AtomicBoolean hovered = new AtomicBoolean(false);
        AtomicReference<String> submitted = new AtomicReference<>("");
        
        binder.bindClick("#Button", () -> clicked.set(true))
              .bindHover("#Element", hovered::set)
              .bindSubmit("#Form", submitted::set);
        
        events.dispatch("#Button", "click", "");
        events.dispatch("#Element", "hover", "true");
        events.dispatch("#Form", "submit", "test data");
        
        boolean pass = 
            clicked.get() &&
            hovered.get() &&
            "test data".equals(submitted.get()) &&
            binder.getBindingCount() == 3 &&
            binder.hasBinding("#Button", "click");
        
        System.out.println("  [" + (pass ? "PASS" : "FAIL") + "] UIBinder: Event binding works");
    }
    
    private static void testMinimapOverlay() {
        MinimapManager manager = new MinimapManager();
        MinimapOverlay overlay = new MinimapOverlay(manager);
        
        overlay.zoomIn();
        overlay.zoomIn();
        boolean zoomCorrect = Math.abs(overlay.getZoomLevel() - 2.0f) < 0.001f;
        
        overlay.zoomOut();
        boolean zoomAfter = Math.abs(overlay.getZoomLevel() - 1.5f) < 0.001f;
        
        overlay.setShowWaypoints(false);
        boolean waypointsOff = !overlay.isShowingWaypoints();
        
        overlay.setShowPlayers(false);
        boolean playersOff = !overlay.isShowingPlayers();
        
        Waypoint wp = new Waypoint("Home", 100, 64, 200);
        manager.addWaypoint(java.util.UUID.randomUUID(), wp);
        
        boolean pass = zoomCorrect && zoomAfter && waypointsOff && playersOff;
        System.out.println("  [" + (pass ? "PASS" : "FAIL") + "] MinimapOverlay: Overlay controls work");
    }
    
    private static void testVoiceChatOverlay() {
        VoiceChatManager manager = new VoiceChatManager();
        VoiceChatOverlay overlay = new VoiceChatOverlay(manager);
        
        boolean initialPTT = !overlay.isPTTActive();
        overlay.togglePTT();
        boolean afterPTT = overlay.isPTTActive();
        
        boolean initialMute = !overlay.isMuted();
        overlay.toggleMute();
        boolean afterMute = overlay.isMuted();
        
        overlay.setVolume(0.5f);
        boolean volumeCorrect = Math.abs(overlay.getVolume() - 0.5f) < 0.001f;
        
        overlay.setVolume(1.5f);
        boolean volumeCapped = Math.abs(overlay.getVolume() - 1.0f) < 0.001f;
        
        boolean pass = initialPTT && afterPTT && initialMute && afterMute && volumeCorrect && volumeCapped;
        System.out.println("  [" + (pass ? "PASS" : "FAIL") + "] VoiceChatOverlay: Voice chat controls work");
    }
    
    private static void testStatsOverlay() {
        StatsOverlay overlay = new StatsOverlay();
        
        overlay.updateStats(60, 50, 512 * 1024 * 1024, 1024 * 1024 * 1024);
        boolean clientStats = overlay.getFPS() == 60 && overlay.getPing() == 50;
        
        overlay.updateServerStats(19, 100, 64);
        boolean serverStats = overlay.getTPS() == 19;
        
        boolean pass = clientStats && serverStats;
        System.out.println("  [" + (pass ? "PASS" : "FAIL") + "] StatsOverlay: Stats display works");
    }
    
    public static void main(String[] args) {
        runAllTests();
    }
}
