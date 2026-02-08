package services;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.*;

public class DiscordRPCService {
    private static final Logger logger = LogManager.getLogger(DiscordRPCService.class);
    
    private Core core;
    private ScheduledExecutorService callBackExecutor;
    private boolean initialized = false;
    private boolean running = false;

    /**
     * Standard service needed for Discord Social SDK's RPC.
     * @param clientId - Client ID from Discord (i.e 123456789L)
     * */
    
    public void start(long clientId) {
        try {
            CreateParams params = new CreateParams();
            params.setClientID(clientId);
            params.setFlags(CreateParams.getDefaultFlags());
            
            core = new Core(params);
            callBackExecutor = Executors.newSingleThreadScheduledExecutor();
            
            // Start the callback loop
            callBackExecutor.scheduleAtFixedRate(
                    () -> {
                        try {
                            core.runCallbacks();
                        } catch (Exception e) {
                            logger.error("Error running Discord RPC callbacks: {}", e.getMessage());
                        }
                    },
                    0,
                    10,
                    TimeUnit.MILLISECONDS
            );
            
            initialized = true;
            running = true;
            logger.info("Discord RPC service started successfully");
            
        } catch (UnsatisfiedLinkError e) {
            logger.error("Discord native library not found. Make sure Discord is installed and the SDK library is properly configured: {}", e.getMessage());
            initialized = false;
        } catch (Exception e) {
            logger.error("Failed to initialize Discord RPC: {}", e.getMessage());
            initialized = false;
        }
    }

    /**
     * Initialize the RPC activity. Call this after start() to set the initial status.
     */
    public void initRpc() {
        if (!initialized || core == null) {
            logger.warn("Discord RPC not initialized, cannot init activity");
            return;
        }
        
        try(Activity activity = new Activity()) {
            activity.setDetails("Idling");
            activity.assets().setLargeImage("512");
            activity.timestamps().setStart(Instant.now());
            core.activityManager().updateActivity(activity);
            logger.info("Initialized Discord RPC activity");
        } catch (Exception e) {
            logger.error("Failed to initialize Discord RPC activity: {}", e.getMessage());
        }
    }

    /**
     * Update the RPC activity with manga reading information.
     * @param mangaTitle - The title of the manga being read
     * @param chapterInfo - Current chapter information (e.g., "Ch. 5")
     * @param isReading - Whether currently reading (true) or idle in reader (false)
     */
    public void updateActivity(String mangaTitle, String chapterInfo, boolean isReading) {
        if (!initialized || core == null) {
            logger.warn("Discord RPC not initialized, cannot update activity");
            return;
        }
        
        try(Activity activity = new Activity()) {
            if (isReading) {
                activity.setDetails(mangaTitle + " - " + chapterInfo);
            } else {
                activity.setDetails("Reading: " + mangaTitle);
            }
            activity.assets().setLargeImage("512");
            activity.timestamps().setStart(Instant.now());
            
            core.activityManager().updateActivity(activity);
            logger.debug("Updated Discord RPC activity: {} - {}", mangaTitle, chapterInfo);
        } catch (Exception e) {
            logger.error("Failed to update Discord RPC activity: {}", e.getMessage());
        }
    }

    /**
     * Clear the current activity (show no rich presence).
     */
    public void clearActivity() {
        if (!initialized || core == null) {
            return;
        }
        
        try {
            core.activityManager().clearActivity();
            logger.debug("Cleared Discord RPC activity");
        } catch (Exception e) {
            logger.error("Failed to clear Discord RPC activity: {}", e.getMessage());
        }
    }

    /**
     * Stop the Discord RPC service and cleanup resources.
     */
    public void stop() {
        if (callBackExecutor != null) {
            callBackExecutor.shutdownNow();
            try {
                if (!callBackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Discord RPC executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (core != null) {
            try {
                core.close();
                logger.info("Discord RPC core closed");
            } catch (Exception e) {
                logger.error("Error closing Discord RPC core: {}", e.getMessage());
            }
        }
        
        running = false;
        initialized = false;
        logger.info("Discord RPC service stopped");
    }

    /**
     * Check if the Discord RPC service is running and initialized.
     * @return true if RPC is active and ready
     */
    public boolean isReady() {
        return running && initialized && core != null;
    }
}

