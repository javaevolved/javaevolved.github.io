///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3
//DEPS org.yaml:snakeyaml:2.4

import module java.base;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

static final Path CONTENT_DIR = Path.of("content");
static final Path LOCALES_FILE = Path.of("html-generators/locales.properties");
static final Path TWEETS_DIR = Path.of("social/tweets");
static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
static final ObjectMapper JSON = new ObjectMapper();
static final Set<String> CONTENT_EXTENSIONS = Set.of("yaml", "yml", "json");
static final Set<String> TRANSLATION_FIELDS = Set.of(
    "title", "oldApproach", "modernApproach", "summary", "explanation",
    "whyModernWins", "support"
);
static final Set<String> BENEFIT_FIELDS = Set.of("icon", "title", "desc");

void main(String... args) throws Exception {
    var files = parseFiles(args);
    var errors = new ArrayList<String>();
    var allPatterns = loadPatterns(errors);
    var locales = loadLocales();
    validateSocialDrafts(allPatterns, errors);

    for (var file : files) {
        validatePattern(file, allPatterns, locales, errors);
    }

    if (!errors.isEmpty()) {
        System.err.println("Pattern contribution validation failed:");
        errors.forEach(error -> System.err.println("  - " + error));
        throw new IllegalStateException(errors.size() + " validation error(s)");
    }
    IO.println("Validated %d new pattern(s) and %d social draft(s)."
        .formatted(files.size(), allPatterns.size()));
}

List<Path> parseFiles(String[] args) throws Exception {
    var files = new LinkedHashSet<Path>();
    String base = null;
    String head = "HEAD";

    for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
            case "--file" -> files.add(Path.of(requireValue(args, ++i, "--file")));
            case "--base" -> base = requireValue(args, ++i, "--base");
            case "--head" -> head = requireValue(args, ++i, "--head");
            default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
        }
    }
    if (base != null) {
        files.addAll(addedContentFiles(base, head));
    }
    return files.stream()
        .map(Path::normalize)
        .filter(this::isPatternPath)
        .toList();
}

String requireValue(String[] args, int index, String option) {
    if (index >= args.length) {
        throw new IllegalArgumentException("Missing value for " + option);
    }
    return args[index];
}

List<Path> addedContentFiles(String base, String head) throws Exception {
    var process = new ProcessBuilder(
        "git", "diff", "--name-only", "--no-renames", "--diff-filter=A",
        base + "..." + head, "--", "content/"
    ).redirectErrorStream(true).start();
    var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) {
        throw new IllegalStateException("git diff failed:\n" + output);
    }
    return output.lines().filter(line -> !line.isBlank()).map(Path::of).toList();
}

boolean isPatternPath(Path path) {
    if (path.getNameCount() != 3 || !path.startsWith(CONTENT_DIR)) return false;
    var name = path.getFileName().toString();
    var dot = name.lastIndexOf('.');
    return dot > 0 && CONTENT_EXTENSIONS.contains(name.substring(dot + 1));
}

Map<String, JsonNode> loadPatterns(List<String> errors) throws IOException {
    var patterns = new LinkedHashMap<String, JsonNode>();
    try (var paths = Files.walk(CONTENT_DIR)) {
        for (var path : paths.filter(Files::isRegularFile).filter(this::isPatternPath).sorted().toList()) {
            var node = readContent(path);
            var category = text(node, "category");
            var slug = text(node, "slug");
            if (category == null || slug == null) {
                errors.add(path + " must define category and slug");
                continue;
            }
            var previous = patterns.put(category + "/" + slug, node);
            if (previous != null) {
                errors.add("duplicate pattern key: " + category + "/" + slug);
            }
        }
    }
    return patterns;
}

JsonNode readContent(Path path) throws IOException {
    var name = path.getFileName().toString();
    return name.endsWith(".json") ? JSON.readTree(path.toFile()) : YAML.readTree(path.toFile());
}

List<String> loadLocales() throws IOException {
    return Files.readAllLines(LOCALES_FILE).stream()
        .map(String::strip)
        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
        .map(line -> line.substring(0, line.indexOf('=')).strip())
        .filter(locale -> !locale.equals("en"))
        .toList();
}

void validatePattern(Path file, Map<String, JsonNode> allPatterns, List<String> locales,
                     List<String> errors) throws IOException {
    if (!Files.isRegularFile(file)) {
        errors.add("added pattern file is missing: " + file);
        return;
    }

    var node = readContent(file);
    var category = text(node, "category");
    var slug = text(node, "slug");
    if (category == null || slug == null) {
        errors.add(file + " must define category and slug");
        return;
    }
    var key = category + "/" + slug;
    var expectedPath = CONTENT_DIR.resolve(category).resolve(slug + extension(file));
    if (!file.equals(expectedPath)) {
        errors.add(file + " path must match category and slug (expected " + expectedPath + ")");
    }

    validateProof(file, category, slug, errors);
    validateTranslations(category, slug, locales, errors);
    validateNavigationOrder(key, node, errors);
    validateRelated(key, node, allPatterns, errors);
}

String extension(Path file) {
    var name = file.getFileName().toString();
    return name.substring(name.lastIndexOf('.'));
}

void validateProof(Path contentFile, String category, String slug, List<String> errors)
        throws IOException {
    var className = Arrays.stream(slug.split("-"))
        .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
        .collect(Collectors.joining());
    var proof = Path.of("proof", category, className + ".java");
    if (!Files.isRegularFile(proof, LinkOption.NOFOLLOW_LINKS)) {
        errors.add("missing proof for " + category + "/" + slug + ": " + proof);
        return;
    }
    var lines = Files.readAllLines(proof);
    if (lines.stream().noneMatch(line -> line.equals("/// Proof: " + slug))) {
        errors.add(proof + " must declare /// Proof: " + slug);
    }
    if (lines.stream().noneMatch(line -> line.equals("/// Source: " + contentFile))) {
        errors.add(proof + " must declare /// Source: " + contentFile);
    }
}

void validateTranslations(String category, String slug, List<String> locales,
                          List<String> errors) throws IOException {
    for (var locale : locales) {
        var base = Path.of("translations", "content", locale, category);
        var translation = findContentFile(base, slug);
        if (translation.isEmpty()) {
            errors.add("missing " + locale + " translation for " + category + "/" + slug);
            continue;
        }
        validateTranslation(locale, translation.get(), errors);
    }
}

Optional<Path> findContentFile(Path directory, String slug) {
    return CONTENT_EXTENSIONS.stream()
        .map(extension -> directory.resolve(slug + "." + extension))
        .filter(Files::isRegularFile)
        .findFirst();
}

void validateTranslation(String locale, Path file, List<String> errors) throws IOException {
    var node = readContent(file);
    var fields = new LinkedHashSet<String>();
    node.fieldNames().forEachRemaining(fields::add);
    if (!fields.equals(TRANSLATION_FIELDS)) {
        errors.add(file + " must contain exactly the translated fields " + TRANSLATION_FIELDS);
    }
    for (var field : List.of("title", "oldApproach", "modernApproach", "summary", "explanation")) {
        if (text(node, field) == null) errors.add(file + " has missing or empty " + field);
    }

    var benefits = node.path("whyModernWins");
    if (!benefits.isArray() || benefits.size() != 3) {
        errors.add(file + " must contain exactly three whyModernWins entries");
    } else {
        for (var benefit : benefits) {
            var benefitFields = new LinkedHashSet<String>();
            benefit.fieldNames().forEachRemaining(benefitFields::add);
            if (!benefitFields.equals(BENEFIT_FIELDS)
                    || BENEFIT_FIELDS.stream().anyMatch(field -> text(benefit, field) == null)) {
                errors.add(file + " whyModernWins entries must contain non-empty icon, title, and desc");
                break;
            }
        }
    }

    var support = node.path("support");
    var supportFields = new LinkedHashSet<String>();
    if (support.isObject()) support.fieldNames().forEachRemaining(supportFields::add);
    if (!supportFields.equals(Set.of("description")) || text(support, "description") == null) {
        errors.add(file + " support must contain only a non-empty description");
    }
}

void validateNavigationOrder(String key, JsonNode node, List<String> errors) {
    if (node.has("prev") || node.has("next")) {
        errors.add(key + " must not define legacy prev or next fields");
    }
    var order = node.get("navigationOrder");
    if (order == null || !order.isIntegralNumber()
            || !order.canConvertToInt() || order.intValue() < 0) {
        errors.add(key + " must define a non-negative integer navigationOrder");
    }
}

void validateRelated(String key, JsonNode node, Map<String, JsonNode> allPatterns,
                     List<String> errors) {
    var related = node.path("related");
    if (!related.isArray() || related.size() != 3) {
        errors.add(key + " must define exactly three related patterns");
        return;
    }
    var distinct = new LinkedHashSet<String>();
    for (var item : related) {
        if (!item.isTextual()) {
            errors.add(key + " related entries must be pattern keys");
        } else if (item.asText().equals(key)) {
            errors.add(key + " must not relate to itself");
        } else if (!distinct.add(item.asText())) {
            errors.add(key + " related patterns must be distinct: " + item.asText());
        } else if (!allPatterns.containsKey(item.asText())) {
            errors.add(key + " related target does not exist: " + item.asText());
        }
    }
}

void validateSocialDrafts(Map<String, JsonNode> allPatterns, List<String> errors)
        throws IOException {
    for (var key : allPatterns.keySet()) {
        var parts = key.split("/", 2);
        validateSocial(parts[0], parts[1], errors);
    }
    if (!Files.isDirectory(TWEETS_DIR)) return;
    try (var files = Files.walk(TWEETS_DIR)) {
        for (var path : files.filter(Files::isRegularFile).toList()) {
            var relative = TWEETS_DIR.relativize(path);
            if (relative.getNameCount() != 2 || !path.getFileName().toString().endsWith(".yaml")) {
                errors.add("unexpected social draft path: " + path);
                continue;
            }
            var slug = path.getFileName().toString().replaceFirst("\\.yaml$", "");
            var key = relative.getName(0) + "/" + slug;
            if (!allPatterns.containsKey(key)) {
                errors.add("social draft has no matching pattern: " + path);
            }
        }
    }
}

void validateSocial(String category, String slug, List<String> errors) throws IOException {
    var path = TWEETS_DIR.resolve(category).resolve(slug + ".yaml");
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
        errors.add("missing tweet draft: " + path);
        return;
    }
    var draft = YAML.readTree(path.toFile());
    var fields = new LinkedHashSet<String>();
    draft.fieldNames().forEachRemaining(fields::add);
    if (!fields.equals(Set.of("text"))) {
        errors.add(path + " must contain only the text field");
    }
    var tweet = draft.get("text");
    if (tweet == null || !tweet.isTextual() || tweet.asText().isBlank()) {
        errors.add(path + " must define non-empty text");
    } else if (tweet.asText().length() > 280) {
        errors.add(path + " exceeds 280 characters");
    }
}

String text(JsonNode node, String field) {
    if (node == null) return null;
    var value = node.get(field);
    if (value == null || !value.isTextual() || value.asText().isBlank()) return null;
    return value.asText();
}
