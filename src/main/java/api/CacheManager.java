package api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static java.nio.file.Files.walk;

public class CacheManager {

    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".shiori" + File.separator + "cache";

    public CacheManager() {
        File dir = new File(CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public File getCachedFile(String url) {
        String filename = generateFilename(url);
        return new File(CACHE_DIR, filename);
    }

    public boolean isCached(String url) {
        return getCachedFile(url).exists();
    }

    public void saveToCache(String url, byte[] data) throws IOException {
        Files.write(getCachedFile(url).toPath(), data);
    }

    public byte[] getFromCache(String url) throws IOException {
        return Files.readAllBytes(getCachedFile(url).toPath());
    }

    public void clearCache() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (Files.exists(cachePath)) {
                walk(cachePath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                Files.createDirectories(cachePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateFilename(String url) {
        return String.valueOf(url.hashCode());
    }
}
