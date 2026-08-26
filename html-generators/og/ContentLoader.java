package og;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;

/** Loads content JSON/YAML files and category properties. */
public final class ContentLoader {

    static final String CONTENT_DIR = "content";
    static final String CATEGORIES_FILE = "html-generators/categories.properties";

    static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    static final Map<String, ObjectMapper> MAPPERS = Map.of(
        "json", JSON_MAPPER, "yaml", YAML_MAPPER, "yml", YAML_MAPPER
    );

    public static final SequencedMap<String, String> CATEGORY_DISPLAY = loadProperties(CATEGORIES_FILE);

    public record Snippet(JsonNode node) {
        public String get(String f)        { return node.get(f).asText(); }
        public String slug()               { return get("slug"); }
        public String category()           { return get("category"); }
        public String title()              { return get("title"); }
        public String jdkVersion()         { return get("jdkVersion"); }
        public String oldCode()            { return get("oldCode"); }
        public String modernCode()         { return get("modernCode"); }
        public String oldApproach()        { return get("oldApproach"); }
        public String modernApproach()     { return get("modernApproach"); }
        public String oldLabel()           { return get("oldLabel"); }
        public String modernLabel()        { return get("modernLabel"); }
        public String key()                { return category() + "/" + slug(); }
        public String catDisplay()         { return CATEGORY_DISPLAY.get(category()); }
    }

    public static SequencedMap<String, Snippet> loadAllSnippets() throws IOException {
        var snippets = new LinkedHashMap<String, Snippet>();
        var ids = new HashSet<String>();
        for (var cat : CATEGORY_DISPLAY.sequencedKeySet()) {
            var catDir = Path.of(CONTENT_DIR, cat);
            if (!Files.isDirectory(catDir)) continue;
            var sorted = new ArrayList<Path>();
            for (var ext : MAPPERS.keySet()) {
                try (var stream = Files.newDirectoryStream(catDir, "*." + ext)) {
                    stream.forEach(sorted::add);
                }
            }
            sorted.sort(Path::compareTo);
            for (var path : sorted) {
                var ext = path.getFileName().toString();
                ext = ext.substring(ext.lastIndexOf('.') + 1);
                var node = MAPPERS.get(ext).readTree(Files.readString(path));
                var id = requireUuidId(node, path);
                if (!ids.add(id)) {
                    throw new IllegalArgumentException("Duplicate UUID id \"" + id + "\" in " + path);
                }
                var snippet = new Snippet(node);
                snippets.put(snippet.key(), snippet);
            }
        }
        return snippets;
    }

    static String requireUuidId(JsonNode node, Path path) {
        var idNode = node.get("id");
        if (idNode == null || !idNode.isTextual()) {
            throw new IllegalArgumentException("Missing or non-string UUID id in " + path);
        }

        var id = idNode.asText().strip();
        try {
            var parsed = UUID.fromString(id);
            if (!parsed.toString().equals(id)) {
                throw new IllegalArgumentException("UUID id must use canonical lowercase form");
            }
            return id;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID id \"" + id + "\" in " + path, e);
        }
    }

    public static SequencedMap<String, String> loadProperties(String file) {
        try {
            var map = new LinkedHashMap<String, String>();
            for (var line : Files.readAllLines(Path.of(file))) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                var idx = line.indexOf('=');
                if (idx > 0) map.put(line.substring(0, idx).strip(), line.substring(idx + 1).strip());
            }
            return map;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static String xmlEscape(String s) {
        return s == null ? ""
            : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
               .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private ContentLoader() {}
}
