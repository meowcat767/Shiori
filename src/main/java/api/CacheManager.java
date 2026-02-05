package api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import static java.nio.file.Files.walk;
import ui.showOptions;

public class CacheManager {

    private final Path cacheDir;
    private showOptions options;
    private boolean cachingEnabled = true; // Default to enabled

    public CacheManager() {
        // Use proper Path API for cross-platform compatibility
        this.cacheDir = Paths.get(
            System.getProperty("user.home"),
            ".yomikomu",
            "cache"
        );

        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            System.err.println("Failed to create cache directory: " + cacheDir);
            e.printStackTrace();
        }
    }

    /**
     * Set the options handler for cache configuration.
     * @param options the showOptions instance
     */
    public void setOptions(showOptions options) {
        this.options = options;
        if (options != null) {
            this.cachingEnabled = options.isCachingEnabled();
        }
    }

    /**
     * Check if caching is enabled.
     * @return true if caching is enabled, false otherwise
     */
    private boolean isCachingEnabled() {
        if (options != null) {
            return options.isCachingEnabled();
        }
        return cachingEnabled;
    }

    public File getCachedFile(String url) {
        String filename = generateFilename(url);
        return cacheDir.resolve(filename).toFile();
    }

    public boolean isCached(String url) {
        // If caching is disabled (either via options or null options), return false
        if (!isCachingEnabled()) {
            return false;
        }
        return getCachedFile(url).exists();
    }

    public byte[] getFromCache(String url) throws IOException {
        // If caching is disabled, return null to fetch fresh data
        if (!isCachingEnabled()) {
            return null;
        }
        return Files.readAllBytes(getCachedFile(url).toPath());
    }

    public void saveToCache(String url, byte[] data) throws IOException {
        // If caching is disabled, skip saving
        if (!isCachingEnabled()) {
            return;
        }
        Files.write(getCachedFile(url).toPath(), data);
    }

    public void clearCache() {
        try {
            if (Files.exists(cacheDir)) {
                try (java.util.stream.Stream<Path> pathStream = walk(cacheDir)) {
                    pathStream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(file -> {
                                if (!file.delete()) {
                                    System.err.println("Failed to delete: " + file.getAbsolutePath());
                                }
                            });
                }
                Files.createDirectories(cacheDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateFilename(String url) {
        return String.valueOf(url.hashCode());
    }
}
