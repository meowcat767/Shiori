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

    public CacheManager() {
        this.options = options;
        // Use proper Path API for cross-platform compatibility
        this.cacheDir = Paths.get(
            System.getProperty("user.home"),
            ".shiori",
            "cache"
        );

        
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            System.err.println("Failed to create cache directory: " + cacheDir);
            e.printStackTrace();
        }
    }

    public File getCachedFile(String url) {
        String filename = generateFilename(url);
        return cacheDir.resolve(filename).toFile();
    }

    public boolean isCached(String url) {
        if(!options.isCachingEnabled()) {return false;}
        return getCachedFile(url).exists();
    }

    public void saveToCache(String url, byte[] data) throws IOException {
        if(!options.isCachingEnabled()) {return;}
        Files.write(getCachedFile(url).toPath(), data);
    }

    public byte[] getFromCache(String url) throws IOException {
        if(!options.isCachingEnabled()) {return null;}
        return Files.readAllBytes(getCachedFile(url).toPath());
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
