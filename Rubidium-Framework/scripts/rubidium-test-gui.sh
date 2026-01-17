#!/bin/bash
# Rubidium Test Server GUI Launcher
# This script launches a GUI for testing the Rubidium plugin

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUBIDIUM_JAR="$SCRIPT_DIR/Rubidium-1.0.0.jar"

# Check for Java
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 25 or later: https://adoptium.net/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 19 ]; then
    echo "Warning: Java $JAVA_VERSION detected. Java 19+ recommended."
fi

# Check if Rubidium JAR exists
if [ ! -f "$RUBIDIUM_JAR" ]; then
    echo "Warning: Rubidium-1.0.0.jar not found in script directory"
    echo "Expected at: $RUBIDIUM_JAR"
fi

# Create a simple GUI using Java Swing
java -cp "$RUBIDIUM_JAR" -Djava.awt.headless=false -jar - <<'JAVA_CODE' 2>/dev/null || {
    # Fallback: Launch inline GUI if JAR doesn't have main class
    java -Djava.awt.headless=false <<'INLINE_GUI'
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class RubidiumTestGUI extends JFrame {
    private static final Color DARK_BG = new Color(30, 30, 35);
    private static final Color DARKER_BG = new Color(20, 20, 25);
    private static final Color ACCENT = new Color(138, 43, 226);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 170);
    private static final Color SUCCESS = new Color(50, 205, 50);
    private static final Color ERROR = new Color(255, 99, 71);
    
    private JTextField serverDirField, rubidiumJarField, hytalePathField;
    private JTextArea consoleOutput;
    private JButton launchBtn, stopBtn;
    private Process serverProcess;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running = false;
    private Properties config = new Properties();
    private Path configFile;
    
    public RubidiumTestGUI() {
        super("Rubidium Test Server");
        configFile = Paths.get(System.getProperty("user.dir"), "rubidium-test.properties");
        initUI();
        loadConfig();
    }
    
    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(DARK_BG);
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header
        JLabel title = new JLabel("Rubidium Test Server");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        main.add(title, BorderLayout.NORTH);
        
        // Config panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(DARK_BG);
        configPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60,60,70)), "Configuration",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), TEXT_PRIMARY));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,5,5);
        
        addField(configPanel, gbc, 0, "Hytale Path:", hytalePathField = createField());
        addField(configPanel, gbc, 1, "Rubidium JAR:", rubidiumJarField = createField());
        addField(configPanel, gbc, 2, "Server Directory:", serverDirField = createField());
        
        // Console
        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setBackground(DARKER_BG);
        consoleOutput.setForeground(TEXT_PRIMARY);
        consoleOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(consoleOutput);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60,60,70)), "Console",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), TEXT_PRIMARY));
        
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, configPanel, scroll);
        split.setDividerLocation(150);
        main.add(split, BorderLayout.CENTER);
        
        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(DARK_BG);
        
        JButton setupBtn = createButton("Setup Server", ACCENT);
        setupBtn.addActionListener(e -> setupServer());
        
        launchBtn = createButton("Launch", SUCCESS);
        launchBtn.addActionListener(e -> launchServer());
        
        stopBtn = createButton("Stop", ERROR);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopServer());
        
        btnPanel.add(setupBtn);
        btnPanel.add(launchBtn);
        btnPanel.add(stopBtn);
        main.add(btnPanel, BorderLayout.SOUTH);
        
        setContentPane(main);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { stopServer(); executor.shutdownNow(); }
        });
    }
    
    private void addField(JPanel p, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT_SECONDARY);
        p.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(field, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browse = new JButton("...");
        browse.addActionListener(e -> browseFor(field, label.contains("JAR")));
        p.add(browse, gbc);
    }
    
    private JTextField createField() {
        JTextField f = new JTextField();
        f.setBackground(DARKER_BG);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60,60,70)),
            BorderFactory.createEmptyBorder(5,8,5,8)));
        f.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { saveConfig(); }});
        return f;
    }
    
    private JButton createButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        return b;
    }
    
    private void browseFor(JTextField field, boolean isFile) {
        JFileChooser fc = new JFileChooser();
        if (isFile) {
            fc.setFileFilter(new FileNameExtensionFilter("JAR Files", "jar"));
        } else {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(fc.getSelectedFile().getAbsolutePath());
            saveConfig();
        }
    }
    
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            consoleOutput.append(msg + "\n");
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        });
    }
    
    private void loadConfig() {
        try {
            if (Files.exists(configFile)) {
                config.load(Files.newBufferedReader(configFile));
                hytalePathField.setText(config.getProperty("hytalePath", ""));
                rubidiumJarField.setText(config.getProperty("rubidiumJar", ""));
                serverDirField.setText(config.getProperty("serverDir", ""));
            }
        } catch (Exception e) { log("[WARN] Could not load config"); }
    }
    
    private void saveConfig() {
        config.setProperty("hytalePath", hytalePathField.getText());
        config.setProperty("rubidiumJar", rubidiumJarField.getText());
        config.setProperty("serverDir", serverDirField.getText());
        try { config.store(Files.newBufferedWriter(configFile), "Rubidium Test Config"); }
        catch (Exception e) { log("[WARN] Could not save config"); }
    }
    
    private void setupServer() {
        String serverDir = serverDirField.getText();
        String rubidiumJar = rubidiumJarField.getText();
        if (serverDir.isEmpty()) { log("[ERROR] Set server directory first"); return; }
        
        executor.submit(() -> {
            try {
                Path dir = Paths.get(serverDir);
                Path plugins = dir.resolve("earlyplugins");
                Files.createDirectories(plugins);
                log("[INFO] Created server directories");
                
                if (!rubidiumJar.isEmpty() && Files.exists(Paths.get(rubidiumJar))) {
                    Files.copy(Paths.get(rubidiumJar), plugins.resolve("Rubidium-1.0.0.jar"), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    log("[INFO] Installed Rubidium plugin");
                }
                
                Path props = dir.resolve("server.properties");
                if (!Files.exists(props)) {
                    Files.writeString(props, "server-port=5520\nmax-players=20\nmotd=Rubidium Test\n");
                    log("[INFO] Created server.properties");
                }
                
                log("[SUCCESS] Setup complete! Click Launch to start.");
            } catch (Exception e) { log("[ERROR] " + e.getMessage()); }
        });
    }
    
    private void launchServer() {
        if (running) { log("[WARN] Already running"); return; }
        String serverDir = serverDirField.getText();
        Path jar = Paths.get(serverDir, "HytaleServer.jar");
        if (!Files.exists(jar)) { log("[ERROR] HytaleServer.jar not found. Copy it to: " + serverDir); return; }
        
        executor.submit(() -> {
            try {
                running = true;
                SwingUtilities.invokeLater(() -> { launchBtn.setEnabled(false); stopBtn.setEnabled(true); });
                log("[INFO] Starting server...");
                
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", "HytaleServer.jar");
                pb.directory(new File(serverDir));
                pb.redirectErrorStream(true);
                serverProcess = pb.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) log(line);
                }
                
                int code = serverProcess.waitFor();
                log("[INFO] Server stopped (exit " + code + ")");
            } catch (Exception e) {
                log("[ERROR] " + e.getMessage());
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> { launchBtn.setEnabled(true); stopBtn.setEnabled(false); });
            }
        });
    }
    
    private void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            log("[INFO] Stopping server...");
            serverProcess.destroy();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RubidiumTestGUI().setVisible(true));
    }
}
INLINE_GUI
}
JAVA_CODE
