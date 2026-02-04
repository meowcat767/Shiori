package api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalPDFStore {

    private final Path jsonPath;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> paths = new ArrayList<>();

    public LocalPDFStore(Path jsonPath) {
        this.jsonPath = jsonPath;
        load();
    }

    public void add(File pdf) {
        String path = pdf.getAbsolutePath();
        if (!paths.contains(path)) {
            paths.add(path);
            save();
        }
    }

    public List<File> getAll() {
        List<File> files = new ArrayList<>();
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) files.add(f);
        }
        return files;
    }

    private void load() {
        try {
            if (jsonPath.toFile().exists()) {
                paths.addAll(
                        mapper.readValue(jsonPath.toFile(),
                                new TypeReference<List<String>>() {})
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            jsonPath.getParent().toFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(jsonPath.toFile(), paths);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}