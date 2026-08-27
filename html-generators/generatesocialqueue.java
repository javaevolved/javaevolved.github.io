///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3
//DEPS org.yaml:snakeyaml:2.4

import module java.base;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Reconcile the pending social queue or generate a tweet draft for one pattern.
 *
 * Default behavior preserves pending order, prunes deleted patterns, and appends
 * new patterns. Use --file content/category/slug.yaml to write one tweet draft,
 * or --reshuffle to begin a fresh cycle containing every pattern.
 */

static final String CONTENT_DIR = "content";
static final String SOCIAL_DIR = "social";
static final String QUEUE_FILE = SOCIAL_DIR + "/queue.txt";
static final String TWEETS_DIR = SOCIAL_DIR + "/tweets";
static final String STATE_FILE = SOCIAL_DIR + "/state.yaml";
static final String BASE_URL = "https://javaevolved.github.io";
static final int MAX_TWEET_LENGTH = 280;

static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
static final ObjectMapper JSON_MAPPER = new ObjectMapper();
static final ObjectMapper YAML_WRITER = new ObjectMapper(
    new YAMLFactory()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
);

record PatternInfo(String category, String slug, String title, String summary,
                   String oldApproach, String modernApproach, String jdkVersion) {
    String key() { return category + "/" + slug; }
}

void main(String... args) throws Exception {
    var allPatterns = scanContentFiles();
    System.out.println("Found " + allPatterns.size() + " patterns in content/");

    if (args.length == 2 && args[0].equals("--file")) {
        var file = Path.of(args[1]).normalize();
        var pattern = readPattern(file);
        if (!allPatterns.containsKey(pattern.key())) {
            throw new IllegalArgumentException("Not a registered content pattern: " + file);
        }
        writeTweetDraft(pattern);
        return;
    }
    if (args.length > 1 || (args.length == 1 && !args[0].equals("--reshuffle"))) {
        throw new IllegalArgumentException(
            "Usage: generatesocialqueue.java [--reshuffle | --file content/category/slug.yaml]");
    }

    var state = loadState();
    var posted = loadPostedKeys(state).stream()
        .filter(allPatterns::containsKey)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    var pending = loadExistingQueue().stream()
        .filter(allPatterns::containsKey)
        .filter(key -> !posted.contains(key))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (args.length == 1) {
        pending.clear();
        posted.clear();
        var keys = new ArrayList<>(allPatterns.keySet());
        Collections.shuffle(keys);
        pending.addAll(keys);
        System.out.println("Started a fresh shuffled cycle");
    } else if (pending.isEmpty() && posted.containsAll(allPatterns.keySet())) {
        posted.clear();
        var keys = new ArrayList<>(allPatterns.keySet());
        Collections.shuffle(keys);
        pending.addAll(keys);
        System.out.println("Completed cycle; started a fresh shuffled cycle");
    } else {
        var known = new LinkedHashSet<>(posted);
        known.addAll(pending);
        var newKeys = allPatterns.keySet().stream()
            .filter(key -> !known.contains(key))
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(newKeys);
        pending.addAll(newKeys);
        if (!newKeys.isEmpty()) {
            System.out.println("Appended " + newKeys.size() + " new patterns: " + newKeys);
        }
    }

    Files.createDirectories(Path.of(SOCIAL_DIR));
    Files.writeString(Path.of(QUEUE_FILE), String.join("\n", pending) + "\n");
    state.put("postedKeys", new ArrayList<>(posted));
    YAML_WRITER.writerWithDefaultPrettyPrinter().writeValue(Path.of(STATE_FILE).toFile(), state);
    System.out.println("Queue reconciled: " + pending.size() + " pending, " + posted.size() + " posted");
}

Map<String, PatternInfo> scanContentFiles() throws Exception {
    var patterns = new LinkedHashMap<String, PatternInfo>();
    var contentDir = Path.of(CONTENT_DIR);

    try (var categories = Files.list(contentDir)) {
        for (var catDir : categories.filter(Files::isDirectory).sorted().toList()) {
            var category = catDir.getFileName().toString();
            try (var files = Files.list(catDir)) {
                for (var file : files.filter(f -> isContentFile(f)).sorted().toList()) {
                    var info = readPattern(file);
                    if (!info.category().equals(category)) {
                        throw new IllegalArgumentException(
                            file + " category must match its parent directory");
                    }
                    patterns.put(info.key(), info);
                }
            }
        }
    }
    return patterns;
}

PatternInfo readPattern(Path file) throws Exception {
    if (!Files.isRegularFile(file) || !isContentFile(file)) {
        throw new IllegalArgumentException("Pattern file does not exist: " + file);
    }
    var node = file.getFileName().toString().endsWith(".json")
        ? JSON_MAPPER.readTree(file.toFile())
        : YAML_MAPPER.readTree(file.toFile());
    return new PatternInfo(
        node.path("category").asText(),
        node.path("slug").asText(),
        node.path("title").asText(),
        node.path("summary").asText(),
        node.path("oldApproach").asText(),
        node.path("modernApproach").asText(),
        node.path("jdkVersion").asText()
    );
}

boolean isContentFile(Path p) {
    var name = p.getFileName().toString();
    return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
}

List<String> loadExistingQueue() throws Exception {
    var path = Path.of(QUEUE_FILE);
    if (!Files.exists(path)) return List.of();
    return Files.readAllLines(path).stream()
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .toList();
}

@SuppressWarnings("unchecked")
Map<String, Object> loadState() throws Exception {
    var path = Path.of(STATE_FILE);
    if (Files.exists(path)) {
        Map<String, Object> state = YAML_MAPPER.readValue(path.toFile(), LinkedHashMap.class);
        if (!state.containsKey("postedKeys")) {
            throw new IllegalArgumentException(
                "Existing social/state.yaml must define postedKeys; migrate it before reconciliation");
        }
        return state;
    }
    var state = new LinkedHashMap<String, Object>();
    state.put("lastPostedKey", null);
    state.put("lastTweetId", null);
    state.put("lastPostedAt", null);
    state.put("postedKeys", new ArrayList<String>());
    return state;
}

List<String> loadPostedKeys(Map<String, Object> state) {
    var value = state.get("postedKeys");
    if (!(value instanceof List<?> values)
            || values.stream().anyMatch(item -> !(item instanceof String))) {
        throw new IllegalArgumentException("social/state.yaml postedKeys must be a string list");
    }
    return values.stream().map(String.class::cast).toList();
}

void writeTweetDraft(PatternInfo pattern) throws Exception {
    var tweet = buildTweet(pattern);
    if (tweet.length() > MAX_TWEET_LENGTH) {
        tweet = buildTweetTruncated(pattern);
    }
    if (tweet.length() > MAX_TWEET_LENGTH) {
        throw new IllegalArgumentException(
            pattern.key() + " tweet is " + tweet.length() + " characters");
    }
    var path = Path.of(TWEETS_DIR, pattern.category(), pattern.slug() + ".yaml");
    Files.createDirectories(path.getParent());
    YAML_WRITER.writerWithDefaultPrettyPrinter()
        .writeValue(path.toFile(), Map.of("text", tweet));
    System.out.println("Wrote " + path + " (" + tweet.length() + " characters)");
}

String buildTweet(PatternInfo p) {
    return """
        ☕ %s
        
        %s
        
        %s → %s (JDK %s+)
        
        🔗 %s/%s/%s.html
        
        #Java #JavaEvolved""".formatted(
            p.title(), p.summary(),
            p.oldApproach(), p.modernApproach(), p.jdkVersion(),
            BASE_URL, p.category(), p.slug()
        ).stripIndent().strip();
}

String buildTweetTruncated(PatternInfo p) {
    // Calculate budget: total minus everything except summary
    var template = """
        ☕ %s
        
        %s
        
        %s → %s (JDK %s+)
        
        🔗 %s/%s/%s.html
        
        #Java #JavaEvolved""".stripIndent().strip();

    var withoutSummary = template.formatted(
        p.title(), "",
        p.oldApproach(), p.modernApproach(), p.jdkVersion(),
        BASE_URL, p.category(), p.slug()
    );
    int budget = MAX_TWEET_LENGTH - withoutSummary.length();
    var summary = p.summary();
    if (summary.length() > budget && budget > 3) {
        summary = summary.substring(0, budget - 1) + "…";
    }
    return template.formatted(
        p.title(), summary,
        p.oldApproach(), p.modernApproach(), p.jdkVersion(),
        BASE_URL, p.category(), p.slug()
    );
}
