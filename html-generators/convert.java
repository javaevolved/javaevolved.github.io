///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3

import module java.base;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

static final String BASE_URL = "https://javaevolved.github.io";
static final String CONTENT_DIR = "content";
static final String SITE_DIR = "site";
static final Pattern TOKEN = Pattern.compile("\\{\\{(\\w+)}}");
static final ObjectMapper JSON_MAPPER = new ObjectMapper();
static final YAMLFactory YAML_FACTORY = new YAMLFactory()
        .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
        .disable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAML_FACTORY);
static final Map<String, ObjectMapper> MAPPERS = Map.of(
        "json", JSON_MAPPER,
        "yaml", YAML_MAPPER,
        "yml", YAML_MAPPER);

record Options(
        Path sourceDir,
        Path targetDir,
        boolean verbose) {
}

void main(String[] args) throws IOException {
    var argList = List.of(args);
    int outputDirPos = Math.max(argList.lastIndexOf("--output-directory"), argList.lastIndexOf("-od"));
    convertAndCheckAllSnippets(new Options(
            Path.of(CONTENT_DIR),
            outputDirPos >= 0 ? Path.of(argList.get(outputDirPos + 1)) : null,
            argList.contains("--verbose") || argList.contains("-v")));
}

void convertAndCheckAllSnippets(Options options) throws IOException {
    try (var categories = Files.newDirectoryStream(options.sourceDir, "*")) {
        categories.forEach(categoryPath -> {
            if (Files.isDirectory(categoryPath)) {
                System.out.println("Processing category folder: " + categoryPath);
                try (var categoryFiles = Files.newDirectoryStream(categoryPath, "*.json")) {
                    categoryFiles.forEach(jsonFile -> {
                        Path yamlPath = null;
                        if (options.targetDir != null) {
                            var yamlPathString = jsonFile.toString()
                                    .replace(options.sourceDir.toString(), options.targetDir.toString())
                                    .replaceFirst("\\.json$", ".yaml");
                            yamlPath = Path.of(yamlPathString);
                            try {
                                Files.createDirectories(yamlPath.getParent());
                            } catch (IOException e) {
                                System.err
                                        .println("Error creating directories for " + yamlPath + ": " + e.getMessage());
                                yamlPath = null;
                            }
                        }
                        convertAndCheckSnippet(jsonFile, yamlPath, options);
                    });
                } catch (Exception e) {
                    System.err
                            .println("Error processing category " + categoryPath.getFileName() + ": " + e.getMessage());
                }
            }
        });
    }
}

void convertAndCheckSnippet(Path jsonPath, Path yamlPath, Options options) {
    try {
        var json = JSON_MAPPER.readTree(jsonPath.toFile());
        var yaml = YAML_MAPPER.writeValueAsString(json);
        var yamlJson = YAML_MAPPER.readTree(yaml);
        if (!json.equals(yamlJson)) {
            if (options.verbose) {
                System.err.println("Mismatch in file: " + jsonPath.getFileName());
                System.err.println(" - original JSON:");
                System.err.println(json.toPrettyString());
                System.err.println(" - generated YAML:");
                System.err.println(yaml);
                System.err.println(" - YAML converted back to JSON:");
                System.err.println(yamlJson.toPrettyString());
            }
        } else {
            if (options.verbose) {
                System.out.println(" - successfully converted and verified: " + jsonPath.getFileName());
            }
            if (yamlPath != null) {
                Files.writeString(yamlPath, yaml);
                if (options.verbose) {
                    System.out.println("   - YAML written to: " + yamlPath);
                }
            }
        }
    } catch (Exception e) {
        System.err.println(" ! error processing file " + jsonPath.getFileName() + ": " + e.getMessage());
        e.printStackTrace();
    }
}
