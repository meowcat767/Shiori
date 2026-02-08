package plugin.ui;

import plugin.*;
import plugin.exception.PluginException;
import plugin.exception.PluginLoadException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Optional;

/**
 * Main dialog for managing plugins.
 * Allows viewing, enabling, disabling, adding, and removing plugins.
 */
public class PluginManagerDialog extends JDialog {
    
    private final PluginManager pluginManager;
    private final LibraryManager libraryManager;
    private final Path pluginsDirectory;
    
    private JPanel pluginListPanel;
    private JPanel detailsPanel;
    private JLabel statusLabel;
    
    public PluginManagerDialog(Frame parent, PluginManager pluginManager, LibraryManager libraryManager) {
        super(parent, "Plugin Manager", true);
        this.pluginManager = pluginManager;
        this.libraryManager = libraryManager;
        this.pluginsDirectory = pluginManager.getPluginLoader().getPluginsDirectory();
        
        initializeUI();
        loadPlugins();
    }
    
    /**
     * Initialize the UI components.
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(800, 500));
        
        // Main content panel with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        
        // Left panel - Plugin list
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Installed Plugins"));
        
        pluginListPanel = new JPanel();
        pluginListPanel.setLayout(new BoxLayout(pluginListPanel, BoxLayout.Y_AXIS));
        pluginListPanel.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(pluginListPanel);
        scrollPane.setPreferredSize(new Dimension(280, 300));
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel for left side
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add Plugin...");
        addButton.addActionListener(e -> addPlugin());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadPlugins());
        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Right panel - Plugin details
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Plugin Details"));
        
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(Color.WHITE);
        
        JScrollPane detailsScroll = new JScrollPane(detailsPanel);
        detailsScroll.setPreferredSize(new Dimension(400, 300));
        rightPanel.add(detailsScroll, BorderLayout.CENTER);
        
        // Details button panel
        JPanel detailsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton enableButton = new JButton("Enable");
        enableButton.addActionListener(e -> togglePluginSelection());
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelectedPlugin());
        detailsButtonPanel.add(enableButton);
        detailsButtonPanel.add(removeButton);
        rightPanel.add(detailsButtonPanel, BorderLayout.SOUTH);
        
        // Add panels to split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Status bar at bottom
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(5, 5, 5, 5));
        statusLabel = new JLabel("Loading plugins...");
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
        
        // Buttons at bottom
        JPanel buttonBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton librariesButton = new JButton("Manage Libraries...");
        librariesButton.addActionListener(e -> showLibrariesDialog());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonBox.add(librariesButton);
        buttonBox.add(closeButton);
        add(buttonBox, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(getParent());
    }
    
    /**
     * Load and display all plugins.
     */
    private void loadPlugins() {
        pluginListPanel.removeAll();
        
        Collection<ShioriPlugin> plugins = pluginManager.getAllPlugins();
        Collection<PluginDescriptor> descriptors = pluginManager.getAllDescriptors();
        
        if (plugins.isEmpty()) {
            JLabel emptyLabel = new JLabel("No plugins installed.\n\nClick 'Add Plugin' to install a new plugin.");
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            pluginListPanel.add(Box.createVerticalStrut(50));
            pluginListPanel.add(emptyLabel);
            statusLabel.setText("No plugins installed");
        } else {
            for (PluginDescriptor descriptor : descriptors) {
                ShioriPlugin plugin = plugins.stream()
                        .filter(p -> p.getId().equals(descriptor.getId()))
                        .findFirst()
                        .orElse(null);
                
                if (plugin != null) {
                    PluginListItem item = new PluginListItem(plugin, descriptor, pluginManager);
                    item.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            showPluginDetails(descriptor, plugin);
                        }
                    });
                    pluginListPanel.add(item);
                    pluginListPanel.add(Box.createVerticalStrut(5));
                }
            }
            
            statusLabel.setText(String.format("%d plugin(s) installed (%d enabled)", 
                    plugins.size(), pluginManager.getEnabledCount()));
        }
        
        pluginListPanel.revalidate();
        pluginListPanel.repaint();
    }
    
    /**
     * Display details for a specific plugin.
     */
    private void showPluginDetails(PluginDescriptor descriptor, ShioriPlugin plugin) {
        detailsPanel.removeAll();
        
        // Plugin name and version
        JLabel nameLabel = new JLabel(descriptor.getName() + " v" + descriptor.getVersion());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 16f));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(nameLabel);
        detailsPanel.add(Box.createVerticalStrut(10));
        
        // Status
        boolean enabled = pluginManager.isEnabled(descriptor.getId());
        JLabel statusLabelLocal = new JLabel("Status: " + (enabled ? "Enabled" : "Disabled"));
        statusLabelLocal.setForeground(enabled ? Color.GREEN : Color.GRAY);
        statusLabelLocal.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(statusLabelLocal);
        detailsPanel.add(Box.createVerticalStrut(10));
        
        // Author
        JLabel authorLabel = new JLabel("Author: " + descriptor.getAuthor());
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(authorLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        
        // Capability
        JLabel capabilityLabel = new JLabel("Type: " + descriptor.getCapability());
        capabilityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(capabilityLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        
        // Description
        JTextArea descArea = new JTextArea(descriptor.getDescription());
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setBackground(detailsPanel.getBackground());
        descArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(descArea);
        detailsPanel.add(Box.createVerticalStrut(10));
        
        // License
        if (!descriptor.getLicense().isEmpty()) {
            JLabel licenseLabel = new JLabel("License: " + descriptor.getLicense());
            licenseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(licenseLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
        }
        
        // Website
        if (!descriptor.getWebsite().isEmpty()) {
            JLabel websiteLabel = new JLabel("Website: " + descriptor.getWebsite());
            websiteLabel.setForeground(Color.BLUE);
            websiteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            websiteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(websiteLabel);
        }
        
        // Dependencies
        if (!descriptor.getDependencies().isEmpty()) {
            detailsPanel.add(Box.createVerticalStrut(10));
            JLabel depsLabel = new JLabel("Dependencies: " + String.join(", ", descriptor.getDependencies()));
            depsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(depsLabel);
        }
        
        // Plugin ID
        detailsPanel.add(Box.createVerticalStrut(10));
        JLabel idLabel = new JLabel("Plugin ID: " + descriptor.getId());
        idLabel.setFont(idLabel.getFont().deriveFont(Font.ITALIC, 10f));
        idLabel.setForeground(Color.GRAY);
        idLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(idLabel);
        
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }
    
    /**
     * Toggle the enabled state of the selected plugin.
     */
    private void togglePluginSelection() {
        // Get selected plugin from the list panel
        if (pluginListPanel.getComponentCount() > 0) {
            Component comp = pluginListPanel.getComponent(0);
            if (comp instanceof PluginListItem) {
                PluginListItem item = (PluginListItem) comp;
                String pluginId = item.getPluginId();
                boolean currentlyEnabled = pluginManager.isEnabled(pluginId);
                
                if (currentlyEnabled) {
                    pluginManager.disablePlugin(pluginId);
                } else {
                    pluginManager.enablePlugin(pluginId);
                }
                
                loadPlugins();
                showPluginDetails(item.getDescriptor(), item.getPlugin());
            }
        }
    }
    
    /**
     * Remove the selected plugin.
     */
    private void removeSelectedPlugin() {
        if (pluginListPanel.getComponentCount() > 0) {
            Component comp = pluginListPanel.getComponent(0);
            if (comp instanceof PluginListItem) {
                PluginListItem item = (PluginListItem) comp;
                String pluginId = item.getPluginId();
                String pluginName = item.getPluginName();
                
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to remove '" + pluginName + "'?\n" +
                        "This will delete the plugin files.",
                        "Confirm Plugin Removal",
                        JOptionPane.YES_NO_OPTION
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    pluginManager.disablePlugin(pluginId);
                    loadPlugins();
                    detailsPanel.removeAll();
                    detailsPanel.repaint();
                }
            }
        }
    }
    
    /**
     * Add a new plugin.
     */
    private void addPlugin() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Plugin");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Plugin files (*.jar, *.zip)", "jar", "zip"));
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            Path sourcePath = selectedFile.toPath();
            
            // Create a plugin directory
            String pluginName = selectedFile.getName().replaceAll("\\.(jar|zip)$", "");
            Path pluginDir = pluginsDirectory.resolve(pluginName);
            
            try {
                // Create plugin directory
                if (!Files.exists(pluginDir)) {
                    Files.createDirectories(pluginDir);
                }
                
                // Copy the file to plugin directory
                Path destPath = pluginDir.resolve(selectedFile.getName());
                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                
                // If it's a ZIP, extract it
                if (selectedFile.getName().endsWith(".zip")) {
                    extractZip(destPath, pluginDir);
                    Files.delete(destPath);
                }
                
                JOptionPane.showMessageDialog(
                        this,
                        "Plugin installed successfully!\n\n" +
                        "The plugin will be loaded after restarting the application.",
                        "Plugin Installed",
                        JOptionPane.INFORMATION_MESSAGE
                );
                
                // Reload plugins
                loadPlugins();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to install plugin: " + e.getMessage(),
                        "Installation Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    /**
     * Extract a ZIP file.
     */
    private void extractZip(Path zipPath, Path destDir) throws Exception {
        // Normalize destination directory once to prevent Zip Slip (directory traversal) attacks
        Path normalizedDestDir = destDir.toAbsolutePath().normalize();
        try (java.util.zip.ZipInputStream zis =
                new java.util.zip.ZipInputStream(Files.newInputStream(zipPath))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Resolve the entry path against the destination directory and normalize it
                Path entryPath = normalizedDestDir.resolve(entry.getName()).normalize();

                // Ensure that the normalized entry path is still within the destination directory
                if (!entryPath.startsWith(normalizedDestDir)) {
                    throw new Exception("Bad zip entry (potential Zip Slip): " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Path parent = entryPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
    
    /**
     * Show the libraries management dialog.
     */
    private void showLibrariesDialog() {
        LibraryDialog dialog = new LibraryDialog(this, libraryManager);
        dialog.setVisible(true);
    }
    
    /**
     * Individual plugin list item component.
     */
    private static class PluginListItem extends JPanel {
        private final ShioriPlugin plugin;
        private final PluginDescriptor descriptor;
        private final PluginManager pluginManager;
        
        public PluginListItem(ShioriPlugin plugin, PluginDescriptor descriptor, PluginManager pluginManager) {
            this.plugin = plugin;
            this.descriptor = descriptor;
            this.pluginManager = pluginManager;
            
            setLayout(new BorderLayout(10, 5));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            setBackground(Color.WHITE);
            setMaximumSize(new Dimension(Short.MAX_VALUE, 60));
            
            // Plugin icon/name
            JPanel leftPanel = new JPanel(new BorderLayout(5, 0));
            leftPanel.setBackground(Color.WHITE);
            
            // Capability icon
            String iconText = getCapabilityIcon(descriptor.getCapability());
            JLabel iconLabel = new JLabel(iconText);
            iconLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            iconLabel.setForeground(getCapabilityColor(descriptor.getCapability()));
            leftPanel.add(iconLabel, BorderLayout.WEST);
            
            JLabel nameLabel = new JLabel(descriptor.getName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            leftPanel.add(nameLabel, BorderLayout.CENTER);
            
            add(leftPanel, BorderLayout.CENTER);
            
            // Status indicator
            boolean enabled = pluginManager.isEnabled(descriptor.getId());
            JLabel statusLabel = new JLabel(enabled ? "✓" : "○");
            statusLabel.setForeground(enabled ? Color.GREEN : Color.GRAY);
            add(statusLabel, BorderLayout.EAST);
        }
        
        private String getCapabilityIcon(PluginCapability capability) {
            return switch (capability) {
                case DATA_SOURCE -> "📚";
                case IMAGE_PROCESSING -> "🎨";
                case UI_EXTENSION -> "🖼️";
                case ANALYTICS -> "📊";
                case EXPORT -> "📤";
                case NOTIFICATION -> "🔔";
                case SYNC -> "🔄";
                case GENERAL -> "🔧";
            };
        }
        
        private Color getCapabilityColor(PluginCapability capability) {
            return switch (capability) {
                case DATA_SOURCE -> Color.BLUE;
                case IMAGE_PROCESSING -> new Color(128, 0, 128);
                case UI_EXTENSION -> Color.ORANGE;
                case ANALYTICS -> Color.DARK_GRAY;
                case EXPORT -> Color.GREEN;
                case NOTIFICATION -> Color.RED;
                case SYNC -> Color.CYAN;
                case GENERAL -> Color.GRAY;
            };
        }
        
        public String getPluginId() {
            return plugin.getId();
        }
        
        public String getPluginName() {
            return descriptor.getName();
        }
        
        public PluginDescriptor getDescriptor() {
            return descriptor;
        }
        
        public ShioriPlugin getPlugin() {
            return plugin;
        }
    }
    
    /**
     * Dialog for managing shared libraries.
     */
    private static class LibraryDialog extends JDialog {
        private final LibraryManager libraryManager;
        
        public LibraryDialog(JDialog parent, LibraryManager libraryManager) {
            super(parent, "Manage Libraries", true);
            this.libraryManager = libraryManager;
            initializeUI();
        }
        
        private void initializeUI() {
            setLayout(new BorderLayout(10, 10));
            setPreferredSize(new Dimension(500, 400));
            
            // Library list
            JPanel listPanel = new JPanel(new BorderLayout(5, 5));
            listPanel.setBorder(BorderFactory.createTitledBorder("Shared Libraries"));
            
            DefaultListModel<String> libModel = new DefaultListModel<>();
            for (String lib : libraryManager.listAvailableLibraries()) {
                libModel.addElement(lib);
            }
            JList<String> libList = new JList<>(libModel);
            JScrollPane scrollPane = new JScrollPane(libList);
            listPanel.add(scrollPane, BorderLayout.CENTER);
            
            add(listPanel, BorderLayout.CENTER);
            
            // Info text
            JTextArea infoArea = new JTextArea(libraryManager.getLibraryInfo());
            infoArea.setEditable(false);
            infoArea.setBackground(getContentPane().getBackground());
            add(new JScrollPane(infoArea), BorderLayout.SOUTH);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addButton = new JButton("Add Library...");
            addButton.addActionListener(e -> addLibrary());
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dispose());
            buttonPanel.add(addButton);
            buttonPanel.add(closeButton);
            
            add(buttonPanel, BorderLayout.SOUTH);
            
            pack();
            setLocationRelativeTo(getParent());
        }
        
        private void addLibrary() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Library");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "JAR files (*.jar)", "jar"));
            
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                boolean success = libraryManager.addLibrary(selectedFile.toPath());
                
                if (success) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Library added successfully!\n\n" +
                            "Plugins may need to be restarted to use the new library.",
                            "Library Added",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Failed to add library.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
    }
}

