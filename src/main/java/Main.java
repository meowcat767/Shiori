import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ui.MainFrame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

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
        
        logger.info("Starting Shiori manga reader application");
        SwingUtilities.invokeLater(() -> {
            try {
                new MainFrame().setVisible(true);
                logger.info("ui.MainFrame initialized and displayed");
            } catch (Exception e) {
                logger.error("Failed to initialize ui.MainFrame", e);
            }
        });
    }
}
