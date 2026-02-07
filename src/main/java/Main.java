import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import plugin.LibraryManager;
import plugin.PluginManager;
import ui.MainFrame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    // Plugin system instances
    private static PluginManager pluginManager;
    private static LibraryManager libraryManager;

    public static void main(String[] args) {
        // Ensure logs directory exists for Log4j (cross-platform compatibility)
        try {
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
                System.out.println("Created logs directory: " + logsDir.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not create logs directory: " + e.getMessage());
        }

        // Initialize plugin system
        initializePlugins();


        logger.info("Starting Shiori manga reader application");
        SwingUtilities.invokeLater(() -> {
            try {
                new MainFrame(pluginManager, libraryManager).setVisible(true);
                logger.info("ui.MainFrame initialized and displayed");
            } catch (Exception e) {
                logger.error("Failed to initialize ui.MainFrame", e);
            }
        });
    }

    /**
     * Initialize the plugin system.
     */
    private static void initializePlugins() {
        try {
            logger.info("Initializing plugin system...");

            // Create plugin manager
            pluginManager = new PluginManager();

            // Initialize library manager
            Path pluginsDir = Paths.get(System.getProperty("user.home"), ".shiori", "plugins");
            libraryManager = new LibraryManager(pluginsDir);

            // Load all plugins
            boolean success = pluginManager.initialize();

            if (success) {
                logger.info("Plugin system initialized successfully");
                logger.info("Loaded {} plugin(s)", pluginManager.getPluginCount());
            } else {
                logger.warn("Plugin system initialization had issues, continuing without plugins");
            }

        } catch (Exception e) {
            logger.error("Failed to initialize plugin system: {}", e.getMessage(), e);
            // Continue without plugins
            pluginManager = new PluginManager();
            Path pluginsDir = Paths.get(System.getProperty("user.home"), ".shiori", "plugins");
            libraryManager = new LibraryManager(pluginsDir);
        }
    }

    /**
     * Get the plugin manager.
     * @return PluginManager instance, or null if not initialized
     */
    public static PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Get the library manager.
     * @return LibraryManager instance, or null if not initialized
     */
    public static LibraryManager getLibraryManager() {
        return libraryManager;
    }

    /**
     * Shutdown the plugin system. Call this when the application exits.
     */
    public static void shutdownPlugins() {
        if (pluginManager != null) {
            pluginManager.shutdown();
        }
        logger.info("Plugin system shutdown complete");
    }
}

