package rubidium.ui;

import rubidium.core.RubidiumBootstrap;
import rubidium.core.tier.FeatureRegistry;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.minimap.MinimapModule;
import rubidium.voicechat.VoiceChatModule;
import rubidium.admin.AdminUIModule;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class RubidiumOverlay {
    
    private static RubidiumOverlay instance;
    private static final Color ACCENT = new Color(138, 43, 226);
    private static final Color BG_DARK = new Color(30, 30, 35);
    private static final Color BG_PANEL = new Color(45, 45, 53);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_DIM = new Color(128, 128, 144);
    private static final Color SUCCESS = new Color(50, 205, 50);
    private static final Color WARNING = new Color(255, 165, 0);
    
    private JFrame mainFrame;
    private JFrame minimapFrame;
    private JFrame hudFrame;
    private final UUID playerId;
    
    public RubidiumOverlay(UUID playerId) {
        this.playerId = playerId;
        instance = this;
    }
    
    public static RubidiumOverlay getInstance() {
        if (instance == null) {
            instance = new RubidiumOverlay(UUID.randomUUID());
        }
        return instance;
    }
    
    public void showSettingsPanel() {
        if (mainFrame != null && mainFrame.isVisible()) {
            mainFrame.toFront();
            return;
        }
        
        mainFrame = new JFrame("Rubidium Settings");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setSize(550, 700);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setResizable(false);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        content.add(createHeader());
        content.add(Box.createVerticalStrut(20));
        content.add(createSection("Display Features", createDisplayToggles()));
        content.add(Box.createVerticalStrut(15));
        content.add(createSection("Voice Chat", createVoiceChatSettings()));
        content.add(Box.createVerticalStrut(15));
        content.add(createSection("Minimap", createMinimapSettings()));
        content.add(Box.createVerticalStrut(15));
        
        if (FeatureRegistry.isEnabled("feature.adminpanel")) {
            content.add(createSection("Administration", createAdminSection()));
            content.add(Box.createVerticalStrut(15));
        }
        
        content.add(createButtonBar());
        
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        mainFrame.add(scroll);
        mainFrame.setVisible(true);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel title = new JLabel("Rubidium Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel version = new JLabel("v" + RubidiumBootstrap.getVersion() + " - " + 
            FeatureRegistry.getCurrentTier().getDisplayName());
        version.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        version.setForeground(TEXT_DIM);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        titlePanel.add(title);
        titlePanel.add(version);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setOpaque(false);
        
        JPanel statusDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SUCCESS);
                g2.fillOval(0, 0, 12, 12);
            }
        };
        statusDot.setPreferredSize(new Dimension(12, 12));
        statusDot.setOpaque(false);
        
        JLabel statusLabel = new JLabel("Online");
        statusLabel.setForeground(SUCCESS);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        statusPanel.add(statusLabel);
        statusPanel.add(statusDot);
        
        header.add(titlePanel, BorderLayout.WEST);
        header.add(statusPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createSection(String title, JPanel content) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(BG_PANEL);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 70), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, section.getPreferredSize().height + 200));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(10));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(content);
        
        return section;
    }
    
    private JPanel createDisplayToggles() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        panel.add(createToggle("Minimap", "Show the minimap overlay", 
            settings.isMinimapEnabled(), b -> { settings.setMinimapEnabled(b); updateMinimap(); }));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createToggle("Waypoints", "Show waypoint markers on minimap", 
            settings.isWaypointsEnabled(), settings::setWaypointsEnabled));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createToggle("Performance Stats", "Show FPS, RAM, ping overlay", 
            settings.isStatisticsEnabled(), settings::setStatisticsEnabled));
        
        return panel;
    }
    
    private JPanel createVoiceChatSettings() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        panel.add(createToggle("Voice Chat", "Enable proximity voice communication", 
            settings.isVoiceChatEnabled(), settings::setVoiceChatEnabled));
        panel.add(Box.createVerticalStrut(12));
        
        JPanel volumePanel = new JPanel(new BorderLayout(10, 0));
        volumePanel.setOpaque(false);
        volumePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel volLabel = new JLabel("Volume");
        volLabel.setForeground(TEXT_PRIMARY);
        volLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JSlider volSlider = new JSlider(0, 100, (int)(settings.getVoiceChatVolume() * 100));
        volSlider.setOpaque(false);
        volSlider.setForeground(ACCENT);
        volSlider.addChangeListener(e -> settings.setVoiceChatVolume(volSlider.getValue() / 100f));
        
        JLabel volValue = new JLabel(volSlider.getValue() + "%");
        volValue.setForeground(TEXT_DIM);
        volSlider.addChangeListener(e -> volValue.setText(volSlider.getValue() + "%"));
        
        volumePanel.add(volLabel, BorderLayout.WEST);
        volumePanel.add(volSlider, BorderLayout.CENTER);
        volumePanel.add(volValue, BorderLayout.EAST);
        
        panel.add(volumePanel);
        panel.add(Box.createVerticalStrut(8));
        
        JPanel pttPanel = new JPanel(new BorderLayout(10, 0));
        pttPanel.setOpaque(false);
        pttPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel pttLabel = new JLabel("Push-to-Talk Key");
        pttLabel.setForeground(TEXT_PRIMARY);
        pttLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JButton pttBtn = new JButton(settings.getPushToTalkKey());
        pttBtn.setBackground(new Color(80, 80, 96));
        pttBtn.setForeground(TEXT_PRIMARY);
        pttBtn.setFocusPainted(false);
        pttBtn.addActionListener(e -> {
            pttBtn.setText("Press a key...");
            pttBtn.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    String key = KeyEvent.getKeyText(e.getKeyCode());
                    settings.setPushToTalkKey(key);
                    pttBtn.setText(key);
                    pttBtn.removeKeyListener(this);
                }
            });
        });
        
        pttPanel.add(pttLabel, BorderLayout.WEST);
        pttPanel.add(pttBtn, BorderLayout.EAST);
        
        panel.add(pttPanel);
        
        return panel;
    }
    
    private JPanel createMinimapSettings() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        JPanel zoomPanel = new JPanel(new BorderLayout(10, 0));
        zoomPanel.setOpaque(false);
        zoomPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel zoomLabel = new JLabel("Zoom Level");
        zoomLabel.setForeground(TEXT_PRIMARY);
        
        JSlider zoomSlider = new JSlider(10, 50, (int)(settings.getMinimapZoom() * 10));
        zoomSlider.setOpaque(false);
        zoomSlider.addChangeListener(e -> settings.setMinimapZoom(zoomSlider.getValue() / 10f));
        
        JLabel zoomValue = new JLabel(String.format("%.1fx", zoomSlider.getValue() / 10f));
        zoomValue.setForeground(TEXT_DIM);
        zoomSlider.addChangeListener(e -> zoomValue.setText(String.format("%.1fx", zoomSlider.getValue() / 10f)));
        
        zoomPanel.add(zoomLabel, BorderLayout.WEST);
        zoomPanel.add(zoomSlider, BorderLayout.CENTER);
        zoomPanel.add(zoomValue, BorderLayout.EAST);
        
        panel.add(zoomPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(createToggle("Rotate with Player", "Minimap rotates based on direction", 
            settings.isMinimapRotate(), settings::setMinimapRotate));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createToggle("Show Compass", "Display N/S/E/W markers", 
            settings.isMinimapCompassEnabled(), settings::setMinimapCompassEnabled));
        
        return panel;
    }
    
    private JPanel createAdminSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        JLabel roleLabel = new JLabel("Your Role: Administrator");
        roleLabel.setForeground(new Color(255, 215, 0));
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(roleLabel);
        panel.add(Box.createVerticalStrut(12));
        
        JButton adminBtn = createStyledButton("Open Admin Panel", ACCENT);
        adminBtn.addActionListener(e -> showAdminPanel());
        adminBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(adminBtn);
        
        return panel;
    }
    
    private JPanel createButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        JButton hudBtn = createStyledButton("Edit HUD Layout", ACCENT);
        hudBtn.addActionListener(e -> showHUDEditor());
        
        JButton minimapBtn = createStyledButton("Toggle Minimap", new Color(70, 130, 180));
        minimapBtn.addActionListener(e -> toggleMinimap());
        
        JButton resetBtn = createStyledButton("Reset Defaults", new Color(139, 0, 0));
        resetBtn.addActionListener(e -> resetSettings());
        
        bar.add(hudBtn);
        bar.add(minimapBtn);
        bar.add(resetBtn);
        
        return bar;
    }
    
    private JPanel createToggle(String name, String description, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel descLabel = new JLabel(description);
        descLabel.setForeground(TEXT_DIM);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        textPanel.add(nameLabel);
        textPanel.add(descLabel);
        
        JToggleButton toggle = new JToggleButton(initial ? "ON" : "OFF");
        toggle.setSelected(initial);
        toggle.setBackground(initial ? SUCCESS : new Color(80, 80, 96));
        toggle.setForeground(Color.WHITE);
        toggle.setFocusPainted(false);
        toggle.setPreferredSize(new Dimension(60, 28));
        toggle.addActionListener(e -> {
            boolean on = toggle.isSelected();
            toggle.setText(on ? "ON" : "OFF");
            toggle.setBackground(on ? SUCCESS : new Color(80, 80, 96));
            onChange.accept(on);
        });
        
        row.add(textPanel, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);
        
        return row;
    }
    
    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    public void showMinimap() {
        if (minimapFrame != null && minimapFrame.isVisible()) return;
        
        minimapFrame = new JFrame();
        minimapFrame.setUndecorated(true);
        minimapFrame.setAlwaysOnTop(true);
        minimapFrame.setSize(180, 180);
        minimapFrame.setLocation(20, 20);
        minimapFrame.setBackground(new Color(0, 0, 0, 0));
        
        JPanel minimap = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(new Color(20, 60, 20));
                g2.fillOval(0, 0, getWidth(), getHeight());
                
                g2.setColor(new Color(40, 80, 40));
                g2.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
                
                Random rand = new Random(42);
                g2.setColor(new Color(60, 100, 60));
                for (int i = 0; i < 20; i++) {
                    int x = rand.nextInt(getWidth() - 20) + 10;
                    int y = rand.nextInt(getHeight() - 20) + 10;
                    g2.fillOval(x, y, 8, 8);
                }
                
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 4, cy - 4, 8, 8);
                g2.setColor(ACCENT);
                g2.drawOval(cx - 6, cy - 6, 12, 12);
                
                g2.setColor(new Color(255, 85, 0));
                g2.fillOval(cx + 30, cy - 20, 6, 6);
                
                g2.setColor(new Color(0, 255, 0));
                g2.fillOval(cx - 40, cy + 25, 6, 6);
                
                g2.setColor(TEXT_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString("N", cx - 4, 15);
                g2.drawString("S", cx - 4, getHeight() - 8);
                g2.drawString("E", getWidth() - 15, cy + 4);
                g2.drawString("W", 5, cy + 4);
                
                g2.setColor(new Color(30, 30, 35, 200));
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            }
        };
        minimap.setOpaque(false);
        
        ComponentMover mover = new ComponentMover(minimapFrame, minimap);
        
        minimapFrame.add(minimap);
        minimapFrame.setVisible(true);
    }
    
    public void hideMinimap() {
        if (minimapFrame != null) {
            minimapFrame.dispose();
            minimapFrame = null;
        }
    }
    
    public void toggleMinimap() {
        if (minimapFrame != null && minimapFrame.isVisible()) {
            hideMinimap();
        } else {
            showMinimap();
        }
    }
    
    private void updateMinimap() {
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        if (settings.isMinimapEnabled()) {
            showMinimap();
        } else {
            hideMinimap();
        }
    }
    
    public void showAdminPanel() {
        JFrame adminFrame = new JFrame("Rubidium Admin Panel");
        adminFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        adminFrame.setSize(700, 500);
        adminFrame.setLocationRelativeTo(null);
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_DARK);
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(40, 40, 50));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel title = new JLabel("Admin Panel");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(255, 215, 0));
        header.add(title, BorderLayout.WEST);
        
        String[] panels = {"Players", "World", "Permissions", "Server", "Chunks", "Teleport", "Bans", "Items"};
        JPanel tabs = new JPanel(new GridLayout(1, panels.length, 5, 0));
        tabs.setOpaque(false);
        
        JPanel mainContent = new JPanel(new CardLayout());
        mainContent.setBackground(BG_PANEL);
        
        for (String panel : panels) {
            JButton tabBtn = new JButton(panel);
            tabBtn.setBackground(new Color(60, 60, 70));
            tabBtn.setForeground(TEXT_PRIMARY);
            tabBtn.setFocusPainted(false);
            tabBtn.addActionListener(e -> {
                ((CardLayout) mainContent.getLayout()).show(mainContent, panel);
            });
            tabs.add(tabBtn);
            
            JPanel panelContent = createAdminPanelContent(panel);
            mainContent.add(panelContent, panel);
        }
        
        header.add(tabs, BorderLayout.CENTER);
        
        content.add(header, BorderLayout.NORTH);
        content.add(mainContent, BorderLayout.CENTER);
        
        adminFrame.add(content);
        adminFrame.setVisible(true);
    }
    
    private JPanel createAdminPanelContent(String panelName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel(panelName + " Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));
        
        JLabel desc = new JLabel("Manage " + panelName.toLowerCase() + " settings and configurations.");
        desc.setForeground(TEXT_DIM);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(20));
        
        switch (panelName) {
            case "Players" -> {
                panel.add(createAdminRow("View Online Players", "See all connected players"));
                panel.add(createAdminRow("Kick Player", "Remove a player from the server"));
                panel.add(createAdminRow("Give Permissions", "Assign permissions to players"));
            }
            case "Server" -> {
                panel.add(createAdminRow("Restart Server", "Restart the game server"));
                panel.add(createAdminRow("Stop Server", "Shut down the server"));
                panel.add(createAdminRow("Performance", "View server performance metrics"));
            }
            case "Teleport" -> {
                panel.add(createAdminRow("Teleport to Player", "Go to a player's location"));
                panel.add(createAdminRow("Summon Player", "Bring a player to you"));
                panel.add(createAdminRow("Set Spawn", "Set the server spawn point"));
            }
            default -> {
                panel.add(createAdminRow("Configure " + panelName, "Adjust " + panelName.toLowerCase() + " settings"));
            }
        }
        
        return panel;
    }
    
    private JPanel createAdminRow(String action, String desc) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel actionLabel = new JLabel(action);
        actionLabel.setForeground(TEXT_PRIMARY);
        actionLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JLabel descLabel = new JLabel(desc);
        descLabel.setForeground(TEXT_DIM);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        textPanel.add(actionLabel);
        textPanel.add(descLabel);
        
        JButton actionBtn = createStyledButton("Execute", new Color(70, 130, 180));
        
        row.add(textPanel, BorderLayout.CENTER);
        row.add(actionBtn, BorderLayout.EAST);
        
        return row;
    }
    
    public void showHUDEditor() {
        JFrame hudFrame = new JFrame("HUD Editor");
        hudFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        hudFrame.setSize(600, 400);
        hudFrame.setLocationRelativeTo(null);
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("HUD Layout Editor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(ACCENT);
        
        JPanel preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(new Color(50, 50, 60));
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.setColor(new Color(70, 70, 80));
                g2.drawRect(20, 20, 100, 100);
                g2.drawString("Minimap", 45, 75);
                
                g2.drawRect(getWidth() - 120, 20, 100, 60);
                g2.drawString("Stats", getWidth() - 85, 55);
                
                g2.drawRect(getWidth() - 120, getHeight() - 80, 100, 60);
                g2.drawString("Voice", getWidth() - 85, getHeight() - 45);
                
                g2.setColor(TEXT_DIM);
                g2.drawString("Drag elements to reposition", getWidth()/2 - 80, getHeight()/2);
            }
        };
        preview.setPreferredSize(new Dimension(500, 280));
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controls.setOpaque(false);
        
        JButton saveBtn = createStyledButton("Save Layout", SUCCESS);
        JButton resetBtn = createStyledButton("Reset", new Color(139, 0, 0));
        
        controls.add(saveBtn);
        controls.add(resetBtn);
        
        content.add(title, BorderLayout.NORTH);
        content.add(preview, BorderLayout.CENTER);
        content.add(controls, BorderLayout.SOUTH);
        
        hudFrame.add(content);
        hudFrame.setVisible(true);
    }
    
    private void resetSettings() {
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        settings.setMinimapEnabled(true);
        settings.setWaypointsEnabled(true);
        settings.setStatisticsEnabled(false);
        settings.setVoiceChatEnabled(true);
        settings.setMinimapZoom(1.0f);
        settings.setVoiceChatVolume(1.0f);
        settings.setPushToTalkKey("V");
        
        JOptionPane.showMessageDialog(mainFrame, "Settings reset to defaults!", 
            "Rubidium", JOptionPane.INFORMATION_MESSAGE);
        
        if (mainFrame != null) {
            mainFrame.dispose();
            showSettingsPanel();
        }
    }
    
    public void dispose() {
        if (mainFrame != null) mainFrame.dispose();
        if (minimapFrame != null) minimapFrame.dispose();
        if (hudFrame != null) hudFrame.dispose();
    }
    
    private static class ComponentMover extends MouseAdapter {
        private final Window window;
        private Point startPoint;
        
        public ComponentMover(Window window, Component component) {
            this.window = window;
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            startPoint = e.getPoint();
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            Point location = window.getLocation();
            int x = location.x + e.getX() - startPoint.x;
            int y = location.y + e.getY() - startPoint.y;
            window.setLocation(x, y);
        }
    }
}
