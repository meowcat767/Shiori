/**
 * NOTE: THIS REQUIRES PYTHON TO WORK AS IT SHOULD.
 * THERE IS A FALLBACK, THOUGH THIS EDGE CASE HAS NEVER BEEN TESTED.
 * update: it has been tested and it works, totally not my bad code or anything caused an "unexpected test"
 * */

package api;

import com.fasterxml.jackson.databind.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.python.util.PythonInterpreter;
import org.python.core.*;
import java.net.http.*;
import java.net.*;
import java.io.*;
import java.util.*;
import model.*;

public class MangaDexClient {
    private static final Logger logger = LogManager.getLogger(MangaDexClient.class);
    private static final String API = "https://api.mangadex.org";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Jython interpreter and Python module
    private final PythonInterpreter interpreter;
    private final boolean pythonAvailable;

    public MangaDexClient() {
        // Initialize Jython interpreter
        this.interpreter = new PythonInterpreter();
        this.pythonAvailable = initializePythonModule();
    }

    /**
     * Initialize the Python interpreter and load the mangadex_api module.
     * @return true if Python module loaded successfully
     */
    private boolean initializePythonModule() {
        try {
            // Add the python module path to Python's path
            interpreter.exec("import sys");
            logger.debug("Python module initialized");


            // Get the resource path for the Python module
            String pythonPath = getPythonPath();
            if (!pythonPath.isEmpty()) {
                interpreter.exec("sys.path.insert(0, '" + pythonPath + "')");
            }

            // Import the mangadex_api module
            interpreter.exec("import mangadex_api");
            logger.debug("LOG-PYTHON: Attempted to import mangadex_api");

            return true;
        } catch (Exception e) {
            System.err.println("Failed to initialize Python module: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the appropriate Python module path.
     * Handles both IDE and JAR execution modes.
     */
    private String getPythonPath() {
        // First, check if running from IDE (file system)
        File pythonFile = new File("src/main/resources/python/mangadex_api.py");
        if (pythonFile.exists()) {
            logger.debug("yeah it exists");
            return pythonFile.getParentFile().getAbsolutePath();
        }

        // Check if running from working directory
        File cwdFile = new File("src/main/resources/python/mangadex_api.py");
        if (cwdFile.exists()) {
            return cwdFile.getParentFile().getAbsolutePath();
        }

        // Running from JAR - resources are in classpath at /python/
        // Return empty string, Jython will use classpath resources
        return "";
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> res =
                client.send(req, HttpResponse.BodyHandlers.ofString());

        return mapper.readTree(res.body());
    }

    /**

    /**
     * Search manga using Python implementation if available, fallback to Java.
     */
    public List<Manga> searchManga(String title) throws Exception {
        return searchManga(title, false);
    }

    public List<Manga> searchManga(String title, boolean nsfwEnabled) throws Exception {
        if (pythonAvailable) {
            try {

                String jsonStr = interpreter.get("results_json").toString();
                if (jsonStr != null && !jsonStr.isEmpty()) {
                    JsonNode root = mapper.readTree(jsonStr);
                    List<Manga> result = new ArrayList<>();

                    for (JsonNode node : root) {
                        String id = node.get("id").asText();
                        String mangaTitle = node.get("title").asText();
                        result.add(new Manga(id, mangaTitle));
                    }

                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("Python search failed, falling back to Java: " + e.getMessage());
            }
        }

        // Fallback to Java implementation
        logger.debug("looks like java took control...");
        String url = API + "/manga?limit=20&title=" +
                URLEncoder.encode(title, "UTF-8");


        JsonNode root = get(url);
        List<Manga> result = new ArrayList<>();

        for (JsonNode node : root.get("data")) {
            String id = node.get("id").asText();
            JsonNode titles = node.get("attributes").get("title");

            String name = titles.has("en")
                    ? titles.get("en").asText()
                    : titles.elements().next().asText();

            result.add(new Manga(id, name));
        }
        return result;
    }

    /**
     * Get a manga by its ID using Python implementation if available.
     */
    public java.util.Optional<Manga> getManga(String mangaId) throws Exception {
        if (pythonAvailable) {
            try {
                interpreter.set("manga_id", mangaId);
                interpreter.exec(
                    "result = mangadex_api.get_manga(manga_id)\n" +
                    "import json\n" +
                    "result_json = json.dumps(result) if result else None"
                );

                String jsonStr = interpreter.get("result_json").toString();
                if (jsonStr != null && !jsonStr.isEmpty() && !jsonStr.equals("None")) {
                    JsonNode node = mapper.readTree(jsonStr);
                    String id = node.get("id").asText();
                    String mangaTitle = node.get("title").asText();
                    return java.util.Optional.of(new Manga(id, mangaTitle));
                }
            } catch (Exception e) {
                System.err.println("Python get_manga failed, falling back to Java: " + e.getMessage());
            }
        }

        // Fallback to Java implementation
        String url = API + "/manga/" + mangaId;

        JsonNode root = get(url);
        JsonNode data = root.get("data");

        String id = data.get("id").asText();
        JsonNode titles = data.get("attributes").get("title");

        String name = titles.has("en")
                ? titles.get("en").asText()
                : titles.elements().next().asText();

        return java.util.Optional.of(new Manga(id, name));
    }

    /**
     * Get chapters for a manga using Python implementation if available.
     */
    public List<Chapter> getChapters(String mangaId) throws Exception {
        return getChapters(mangaId, false);
    }

    /**
     * Get chapters for a manga with optional NSFW content rating.
     */
    public List<Chapter> getChapters(String mangaId, boolean nsfwEnabled) throws Exception {
        if (pythonAvailable) {
            try {

                String jsonStr = interpreter.get("chapters_json").toString();
                if (jsonStr != null && !jsonStr.isEmpty()) {
                    JsonNode root = mapper.readTree(jsonStr);
                    List<Chapter> result = new ArrayList<>();

                    for (JsonNode node : root) {
                        String id = node.get("id").asText();
                        String chapterTitle = node.get("title").asText("");
                        String chapterNumber = node.get("number").asText("");
                        result.add(new Chapter(id, chapterTitle, chapterNumber));
                    }

                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("Python get_chapters failed, falling back to Java: " + e.getMessage());
            }
        }

        // Fallback to Java implementation
        String url = API + "/chapter?manga=" + mangaId +
                "&translatedLanguage[]=en" +
                "&order[chapter]=asc";


        JsonNode root = get(url);
        List<Chapter> chapters = new ArrayList<>();

        for (JsonNode node : root.get("data")) {
            JsonNode attr = node.get("attributes");

            chapters.add(new Chapter(
                    node.get("id").asText(),
                    attr.get("chapter").asText(""),
                    attr.get("title").asText("")
            ));
        }
        return chapters;
    }

    /**
     * Get page URLs for a chapter using Python implementation if available.
     */
    public List<String> getPageUrls(String chapterId) throws Exception {
        if (pythonAvailable) {
            try {
                interpreter.set("chapter_id", chapterId);
                interpreter.exec(
                    "page_urls = mangadex_api.get_page_urls(chapter_id)\n" +
                    "import json\n" +
                    "page_urls_json = json.dumps(page_urls)"
                );

                String jsonStr = interpreter.get("page_urls_json").toString();
                if (jsonStr != null && !jsonStr.isEmpty()) {
                    JsonNode root = mapper.readTree(jsonStr);
                    List<String> urls = new ArrayList<>();

                    for (JsonNode node : root) {
                        urls.add(node.asText());
                    }

                    if (!urls.isEmpty()) {
                        return urls;
                    }
                }
            } catch (Exception e) {
                System.err.println("Python get_page_urls failed, falling back to Java: " + e.getMessage());
            }
        }

        // Fallback to Java implementation
        JsonNode root = get(API + "/at-home/server/" + chapterId);

        String base = root.get("baseUrl").asText();
        String hash = root.get("chapter").get("hash").asText();

        List<String> urls = new ArrayList<>();
        for (JsonNode file : root.get("chapter").get("data")) {
            urls.add(base + "/data/" + hash + "/" + file.asText());
        }
        return urls;
    }

    /**
     * Get manga statistics using Python implementation if available.
     */
    public JsonNode getMangaStats(String mangaId) throws Exception {
        if (pythonAvailable) {
            try {
                interpreter.set("manga_id", mangaId);
                interpreter.exec(
                    "stats = mangadex_api.get_manga_stats(manga_id)\n" +
                    "import json\n" +
                    "stats_json = json.dumps(stats) if stats else None"
                );

                String jsonStr = interpreter.get("stats_json").toString();
                if (jsonStr != null && !jsonStr.isEmpty() && !jsonStr.equals("None")) {
                    return mapper.readTree(jsonStr);
                }
            } catch (Exception e) {
                System.err.println("Python get_manga_stats failed, falling back to Java: " + e.getMessage());
            }
        }

        // Fallback to Java implementation
        String url = API + "/statistics/manga/" + mangaId;
        JsonNode root = get(url);

        return root
                .path("statistics")
                .path(mangaId);
    }
}

