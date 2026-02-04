package api;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.nio.file.Files.walk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ui.showOptions;

public class LocalLibManager {
    private final Path LocalLib;
    private static final Logger logger = LogManager.getLogger(LocalLibManager.class);

    public LocalLibManager() {
        // user.home works anywhere!
    this.LocalLib = Paths.get(System.getProperty("user.home"), ".shiori", "user-library");
        try {
            Files.createDirectories(LocalLib);
        } catch (IOException e) {
            System.err.println("Failed to create local user lib directory: " + LocalLib);
            e.printStackTrace();
        }

    } public List<Path> getAllPDFs() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(LocalLib, "*.pdf")) {
            return StreamSupport.stream(stream.spliterator(), false).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list contents of local user lib directory: " + LocalLib);
            return List.of();
        }
    }

}
