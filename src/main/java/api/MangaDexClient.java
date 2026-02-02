package api;

import com.fasterxml.jackson.databind.*;
import java.net.http.*;
import java.net.*;
import java.nio.channels.ScatteringByteChannel;
import java.util.*;
import model.*;

public class MangaDexClient {
    private static final String API = "https://api.mangadex.org";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> res =
                client.send(req, HttpResponse.BodyHandlers.ofString());

        return mapper.readTree(res.body());
    }

    public List<Manga> searchManga(String title) throws Exception {
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
     * Get a manga by its ID
     * @param mangaId the manga ID
     * @return Optional containing the Manga if found
     */
    public java.util.Optional<Manga> getManga(String mangaId) throws Exception {
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

    public List<Chapter> getChapters(String mangaId) throws Exception {
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

    public List<String> getPageUrls(String chapterId) throws Exception {
        JsonNode root = get(API + "/at-home/server/" + chapterId);

        String base = root.get("baseUrl").asText();
        String hash = root.get("chapter").get("hash").asText();

        List<String> urls = new ArrayList<>();
        for (JsonNode file : root.get("chapter").get("data")) {
            urls.add(base + "/data/" + hash + "/" + file.asText());
        }
        return urls;
    }

    public JsonNode getMangaStats(String mangaId) throws Exception {
        String url = API + "/statistics/manga/" + mangaId;
        JsonNode root = get(url);

        return root
                .path("statistics")
                .path(mangaId);
    }
}