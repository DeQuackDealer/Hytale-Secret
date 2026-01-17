package launcher;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class TestServerLauncher extends JFrame {
    
    private static final String CONFIG_FILE = "launcher-config.properties";
    private static final Color DARK_BG = new Color(30, 30, 35);
    private static final Color DARKER_BG = new Color(20, 20, 25);
    private static final Color ACCENT = new Color(138, 43, 226);
    private static final Color ACCENT_HOVER = new Color(158, 63, 246);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 170);
    private static final Color SUCCESS = new Color(50, 205, 50);
    private static final Color ERROR = new Color(255, 99, 71);
    
    private JTextField hytalePathField;
    private JTextField rubidiumJarField;
    private JTextField pluginsFolderField;
    private JTextField serverDirField;
    private JTextField serverPortField;
    private JTextArea consoleOutput;
    private JButton launchButton;
    private JButton stopButton;
    private JList<String> pluginsList;
    private DefaultListModel<String> pluginsListModel;
    
    private Process serverProcess;
    private ExecutorService executor;
    private volatile boolean serverRunning = false;
    private Path launcherDir;
    
    public TestServerLauncher() {
        super("Rubidium Test Server Launcher");
        executor = Executors.newCachedThreadPool();
        launcherDir = resolveLauncherDir();
        initUI();
        loadConfig();
    }
    
    private Path resolveLauncherDir() {
        try {
            String jarPath = TestServerLauncher.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            Path jarFile = Paths.get(jarPath);
            if (Files.isRegularFile(jarFile)) {
                return jarFile.getParent();
            }
        } catch (Exception ignored) {}
        return Paths.get(System.getProperty("user.dir"));
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(DARK_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
                executor.shutdownNow();
            }
        });
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK_BG);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel title = new JLabel("Rubidium Test Server");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JLabel subtitle = new JLabel("Local Hytale Development Environment");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_SECONDARY);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(DARK_BG);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        
        header.add(titlePanel, BorderLayout.WEST);
        
        return header;
    }
    
    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(DARK_BG);
        
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(DARK_BG);
        leftPanel.setPreferredSize(new Dimension(400, 0));
        
        leftPanel.add(createConfigPanel(), BorderLayout.NORTH);
        leftPanel.add(createPluginsPanel(), BorderLayout.CENTER);
        
        center.add(leftPanel, BorderLayout.WEST);
        center.add(createConsolePanel(), BorderLayout.CENTER);
        
        return center;
    }
    
    private JPanel createConfigPanel() {
        JPanel config = createStyledPanel("Server Configuration");
        config.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        config.add(createLabel("Hytale Path:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        hytalePathField = createTextField();
        hytalePathField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { saveConfig(); }
        });
        config.add(hytalePathField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseHytale = createSmallButton("Browse");
        browseHytale.addActionListener(e -> browseHytalePath());
        config.add(browseHytale, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        config.add(createLabel("Plugins Folder:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        pluginsFolderField = createTextField();
        pluginsFolderField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { saveConfig(); refreshPluginsList(); }
        });
        config.add(pluginsFolderField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browsePlugins = createSmallButton("Browse");
        browsePlugins.addActionListener(e -> browsePluginsFolder());
        config.add(browsePlugins, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        config.add(createLabel("Rubidium JAR:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        rubidiumJarField = createTextField();
        rubidiumJarField.setToolTipText("Path to Rubidium-1.0.0.jar (optional)");
        rubidiumJarField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { saveConfig(); }
        });
        config.add(rubidiumJarField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseRubidium = createSmallButton("Browse");
        browseRubidium.addActionListener(e -> browseRubidiumJar());
        config.add(browseRubidium, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.gridwidth = 1;
        config.add(createLabel("Server Directory:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        serverDirField = createTextField();
        serverDirField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { saveConfig(); }
        });
        config.add(serverDirField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseServerDir = createSmallButton("Browse");
        browseServerDir.addActionListener(e -> browseServerDir());
        config.add(browseServerDir, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        config.add(createLabel("Server Port:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        serverPortField = createTextField();
        serverPortField.setText("5520");
        serverPortField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { saveConfig(); }
        });
        config.add(serverPortField, gbc);
        
        return config;
    }
    
    private void browseRubidiumJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Rubidium JAR");
        chooser.setFileFilter(new FileNameExtensionFilter("JAR Files", "jar"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rubidiumJarField.setText(chooser.getSelectedFile().getAbsolutePath());
            saveConfig();
        }
    }
    
    private void browseServerDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Server Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!serverDirField.getText().isEmpty()) {
            chooser.setCurrentDirectory(new File(serverDirField.getText()));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            serverDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            saveConfig();
        }
    }
    
    private JPanel createPluginsPanel() {
        JPanel plugins = createStyledPanel("Loaded Plugins");
        plugins.setLayout(new BorderLayout(5, 5));
        
        pluginsListModel = new DefaultListModel<>();
        pluginsList = new JList<>(pluginsListModel);
        pluginsList.setBackground(DARKER_BG);
        pluginsList.setForeground(TEXT_PRIMARY);
        pluginsList.setSelectionBackground(ACCENT);
        pluginsList.setFont(new Font("Consolas", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(pluginsList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
        scrollPane.getViewport().setBackground(DARKER_BG);
        plugins.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setBackground(DARK_BG);
        
        JButton refreshBtn = createSmallButton("Refresh");
        refreshBtn.addActionListener(e -> refreshPluginsList());
        buttonPanel.add(refreshBtn);
        
        JButton addPluginBtn = createSmallButton("Add Plugin");
        addPluginBtn.addActionListener(e -> addPlugin());
        buttonPanel.add(addPluginBtn);
        
        plugins.add(buttonPanel, BorderLayout.SOUTH);
        
        return plugins;
    }
    
    private JPanel createConsolePanel() {
        JPanel console = createStyledPanel("Server Console");
        console.setLayout(new BorderLayout(5, 5));
        
        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setBackground(DARKER_BG);
        consoleOutput.setForeground(TEXT_PRIMARY);
        consoleOutput.setCaretColor(TEXT_PRIMARY);
        consoleOutput.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleOutput.setLineWrap(true);
        consoleOutput.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
        scrollPane.getViewport().setBackground(DARKER_BG);
        console.add(scrollPane, BorderLayout.CENTER);
        
        JButton clearBtn = createSmallButton("Clear");
        clearBtn.addActionListener(e -> consoleOutput.setText(""));
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(DARK_BG);
        btnPanel.add(clearBtn);
        console.add(btnPanel, BorderLayout.SOUTH);
        
        return console;
    }
    
    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottom.setBackground(DARK_BG);
        
        launchButton = createActionButton("Launch Server", SUCCESS);
        launchButton.addActionListener(e -> launchServer());
        
        stopButton = createActionButton("Stop Server", ERROR);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());
        
        JButton setupButton = createActionButton("Setup Server", ACCENT);
        setupButton.addActionListener(e -> setupServer());
        
        bottom.add(setupButton);
        bottom.add(launchButton);
        bottom.add(stopButton);
        
        return bottom;
    }
    
    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(DARK_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 70)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_PRIMARY
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }
    
    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setBackground(DARKER_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 70)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return field;
    }
    
    private JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(50, 50, 60));
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(70, 70, 80));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(50, 50, 60));
            }
        });
        
        return button;
    }
    
    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color hoverColor = color.brighter();
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) button.setBackground(hoverColor);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }
    
    private void browseHytalePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Hytale Installation Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            hytalePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            saveConfig();
        }
    }
    
    private void browsePluginsFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Plugins Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pluginsFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
            saveConfig();
            refreshPluginsList();
        }
    }
    
    private void refreshPluginsList() {
        pluginsListModel.clear();
        String pluginsPath = pluginsFolderField.getText();
        
        if (pluginsPath.isEmpty()) {
            pluginsListModel.addElement("(No plugins folder selected)");
            return;
        }
        
        Path folder = Paths.get(pluginsPath);
        if (!Files.exists(folder)) {
            pluginsListModel.addElement("(Folder does not exist)");
            return;
        }
        
        try {
            Files.list(folder)
                .filter(p -> p.toString().endsWith(".jar"))
                .forEach(p -> pluginsListModel.addElement(p.getFileName().toString()));
            
            if (pluginsListModel.isEmpty()) {
                pluginsListModel.addElement("(No plugins found)");
            }
        } catch (IOException e) {
            pluginsListModel.addElement("(Error reading folder)");
        }
    }
    
    private void addPlugin() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Plugin JAR");
        chooser.setFileFilter(new FileNameExtensionFilter("JAR Files", "jar"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String pluginsPath = pluginsFolderField.getText();
            if (pluginsPath.isEmpty()) {
                log("[ERROR] Please select a plugins folder first");
                return;
            }
            
            try {
                Path source = chooser.getSelectedFile().toPath();
                Path dest = Paths.get(pluginsPath, source.getFileName().toString());
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                log("[INFO] Added plugin: " + source.getFileName());
                refreshPluginsList();
            } catch (IOException e) {
                log("[ERROR] Failed to add plugin: " + e.getMessage());
            }
        }
    }
    
    private void setupServer() {
        String hytalePath = hytalePathField.getText();
        String pluginsPath = pluginsFolderField.getText();
        String serverDirPath = serverDirField.getText();
        
        if (hytalePath.isEmpty()) {
            log("[ERROR] Please select Hytale installation path");
            return;
        }
        
        if (serverDirPath.isEmpty()) {
            log("[ERROR] Please specify server directory");
            return;
        }
        
        saveConfig();
        
        executor.submit(() -> {
            log("[INFO] Setting up test server in: " + serverDirPath);
            
            try {
                Path serverDir = Paths.get(serverDirPath);
                Path earlyplugins = serverDir.resolve("earlyplugins");
                Files.createDirectories(earlyplugins);
                
                Path hytaleServer = Paths.get(hytalePath, "server", "HytaleServer.jar");
                if (!Files.exists(hytaleServer)) {
                    hytaleServer = Paths.get(hytalePath, "HytaleServer.jar");
                }
                
                if (Files.exists(hytaleServer)) {
                    Path dest = serverDir.resolve("HytaleServer.jar");
                    Files.copy(hytaleServer, dest, StandardCopyOption.REPLACE_EXISTING);
                    log("[INFO] Copied HytaleServer.jar");
                } else {
                    log("[WARN] HytaleServer.jar not found in: " + hytalePath);
                    log("[INFO] You may need to manually copy it to test-server/");
                }
                
                String rubidiumJarPath = rubidiumJarField.getText();
                if (!rubidiumJarPath.isEmpty()) {
                    Path rubidiumJar = Paths.get(rubidiumJarPath);
                    if (Files.exists(rubidiumJar)) {
                        Files.copy(rubidiumJar, earlyplugins.resolve(rubidiumJar.getFileName()), 
                            StandardCopyOption.REPLACE_EXISTING);
                        log("[INFO] Installed Rubidium: " + rubidiumJar.getFileName());
                    } else {
                        log("[WARN] Rubidium JAR not found: " + rubidiumJarPath);
                    }
                }
                
                if (!pluginsPath.isEmpty()) {
                    Path pluginsFolder = Paths.get(pluginsPath);
                    if (Files.exists(pluginsFolder)) {
                        Files.list(pluginsFolder)
                            .filter(p -> p.toString().endsWith(".jar"))
                            .forEach(p -> {
                                try {
                                    Files.copy(p, earlyplugins.resolve(p.getFileName()), 
                                        StandardCopyOption.REPLACE_EXISTING);
                                    log("[INFO] Copied plugin: " + p.getFileName());
                                } catch (IOException e) {
                                    log("[WARN] Failed to copy: " + p.getFileName());
                                }
                            });
                    }
                }
                
                Path configFile = serverDir.resolve("server.properties");
                if (!Files.exists(configFile)) {
                    String config = String.join("\n",
                        "server-port=" + serverPortField.getText(),
                        "max-players=20",
                        "view-distance=10",
                        "motd=Rubidium Test Server",
                        "enable-query=true"
                    );
                    Files.writeString(configFile, config);
                    log("[INFO] Created server.properties");
                }
                
                log("[SUCCESS] Server setup complete!");
                log("[INFO] Run 'Launch Server' to start");
                
                SwingUtilities.invokeLater(this::refreshPluginsList);
                
            } catch (Exception e) {
                log("[ERROR] Setup failed: " + e.getMessage());
            }
        });
    }
    
    private void launchServer() {
        if (serverRunning) {
            log("[WARN] Server is already running");
            return;
        }
        
        String serverDirPath = serverDirField.getText();
        if (serverDirPath.isEmpty()) {
            log("[ERROR] Please specify server directory");
            return;
        }
        
        Path serverDir = Paths.get(serverDirPath);
        Path serverJar = serverDir.resolve("HytaleServer.jar");
        if (!Files.exists(serverJar)) {
            log("[ERROR] HytaleServer.jar not found at: " + serverJar);
            log("[INFO] Run 'Setup Server' first.");
            return;
        }
        
        saveConfig();
        
        executor.submit(() -> {
            try {
                log("[INFO] Starting Hytale server from: " + serverDirPath);
                
                ProcessBuilder pb = new ProcessBuilder(
                    "java", "-Xmx2G", "-Xms1G",
                    "-jar", "HytaleServer.jar",
                    "--port", serverPortField.getText()
                );
                pb.directory(serverDir.toFile());
                pb.redirectErrorStream(true);
                
                serverProcess = pb.start();
                serverRunning = true;
                
                SwingUtilities.invokeLater(() -> {
                    launchButton.setEnabled(false);
                    stopButton.setEnabled(true);
                });
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && serverRunning) {
                        final String logLine = line;
                        SwingUtilities.invokeLater(() -> log(logLine));
                    }
                }
                
                serverProcess.waitFor();
                log("[INFO] Server stopped");
                
            } catch (Exception e) {
                log("[ERROR] Failed to start server: " + e.getMessage());
            } finally {
                serverRunning = false;
                SwingUtilities.invokeLater(() -> {
                    launchButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        });
    }
    
    private void stopServer() {
        if (!serverRunning || serverProcess == null) {
            return;
        }
        
        log("[INFO] Stopping server...");
        serverRunning = false;
        serverProcess.destroy();
        
        executor.submit(() -> {
            try {
                if (!serverProcess.waitFor(10, TimeUnit.SECONDS)) {
                    serverProcess.destroyForcibly();
                    log("[WARN] Server forcibly terminated");
                }
            } catch (InterruptedException ignored) {}
        });
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleOutput.append(message + "\n");
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        });
    }
    
    private void saveConfig() {
        try {
            Properties props = new Properties();
            props.setProperty("hytalePath", hytalePathField.getText());
            props.setProperty("rubidiumJar", rubidiumJarField.getText());
            props.setProperty("pluginsFolder", pluginsFolderField.getText());
            props.setProperty("serverDir", serverDirField.getText());
            props.setProperty("serverPort", serverPortField.getText());
            
            Path configPath = launcherDir.resolve(CONFIG_FILE);
            try (FileOutputStream out = new FileOutputStream(configPath.toFile())) {
                props.store(out, "Rubidium Test Server Launcher Configuration");
            }
        } catch (IOException e) {
            log("[WARN] Failed to save config: " + e.getMessage());
        }
    }
    
    private void loadConfig() {
        try {
            Path configPath = launcherDir.resolve(CONFIG_FILE);
            if (Files.exists(configPath)) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(configPath.toFile())) {
                    props.load(in);
                }
                
                hytalePathField.setText(props.getProperty("hytalePath", ""));
                rubidiumJarField.setText(props.getProperty("rubidiumJar", ""));
                pluginsFolderField.setText(props.getProperty("pluginsFolder", ""));
                String savedServerDir = props.getProperty("serverDir", "");
                if (!savedServerDir.isEmpty()) {
                    serverDirField.setText(savedServerDir);
                }
                serverPortField.setText(props.getProperty("serverPort", "5520"));
                
                refreshPluginsList();
                log("[INFO] Configuration loaded");
            } else {
                serverDirField.setText(launcherDir.resolve("test-server").toString());
            }
        } catch (IOException e) {
            log("[WARN] Failed to load config: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TestServerLauncher launcher = new TestServerLauncher();
            launcher.setVisible(true);
        });
    }
}
