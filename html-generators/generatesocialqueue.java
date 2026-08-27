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
 * Generate social media queue and pre-drafted tweets from content YAML files.
 *
 * Produces:
 *   social/queue.txt    — shuffled posting order (one category/slug per line)
 *   social/tweets.yaml  — pre-drafted tweet text for each pattern
 *
 * Re-run behavior:
 *   - New patterns are appended to the end of the existing queue
 *   - Deleted/renamed patterns are pruned
 *   - Existing order and tweet edits are preserved
 *   - Use --reshuffle to force a full reshuffle
 */

static final String CONTENT_DIR = "content";
static final String SOCIAL_DIR = "social";
static final String QUEUE_FILE = SOCIAL_DIR + "/queue.txt";
static final String TWEETS_FILE = SOCIAL_DIR + "/tweets.yaml";
static final String STATE_FILE = SOCIAL_DIR + "/state.yaml";
static final String BASE_URL = "https://javaevolved.github.io";
static final int MAX_TWEET_LENGTH = 280;

static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
static final ObjectMapper YAML_WRITER = new ObjectMapper(
    new YAMLFactory()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
);

record PatternInfo(String category, String slug, String title, String summary,
                   String oldApproach, String modernApproach, String jdkVersion) {
    String key() { return category + "/" + slug; }
}

void main(String... args) throws Exception {
    boolean reshuffle = List.of(args).contains("--reshuffle");

    // 1. Scan all content files
    var allPatterns = scanContentFiles();
    System.out.println("Found " + allPatterns.size() + " patterns in content/");

    // 2. Load existing queue and tweets (if any)
    var existingQueue = loadExistingQueue();
    var existingTweets = loadExistingTweets();

    // 3. Determine new queue order
    List<String> queue;
    if (reshuffle || existingQueue.isEmpty()) {
        // Full shuffle
        var keys = new ArrayList<>(allPatterns.keySet());
        Collections.shuffle(keys);
        queue = keys;
        System.out.println(reshuffle ? "Reshuffled all patterns" : "Generated new queue");
    } else {
        // Preserve existing order, prune deleted, append new
        queue = new ArrayList<>();
        for (var key : existingQueue) {
            if (allPatterns.containsKey(key)) queue.add(key);
            else System.out.println("  Pruned (removed): " + key);
        }
        var existingSet = new LinkedHashSet<>(queue);
        var newKeys = new ArrayList<String>();
        for (var key : allPatterns.keySet()) {
            if (!existingSet.contains(key)) newKeys.add(key);
        }
        if (!newKeys.isEmpty()) {
            Collections.shuffle(newKeys);
            queue.addAll(newKeys);
            System.out.println("  Appended " + newKeys.size() + " new patterns: " + newKeys);
        }
    }

    // 4. Generate tweet drafts
    var tweets = new LinkedHashMap<String, String>();
    int truncated = 0;
    for (var key : queue) {
        // Preserve manually edited tweets
        if (!reshuffle && existingTweets.containsKey(key)) {
            tweets.put(key, existingTweets.get(key));
        } else {
            var p = allPatterns.get(key);
            var tweet = buildTweet(p);
            tweets.put(key, tweet);
            if (tweet.length() > MAX_TWEET_LENGTH) {
                // Retry with truncated summary
                tweet = buildTweetTruncated(p);
                tweets.put(key, tweet);
                truncated++;
            }
        }
    }

    // 5. Validate lengths
    int overLength = 0;
    for (var entry : tweets.entrySet()) {
        int len = entry.getValue().length();
        if (len > MAX_TWEET_LENGTH) {
            System.err.println("  WARNING: " + entry.getKey() + " tweet is " + len + " chars (max " + MAX_TWEET_LENGTH + ")");
            overLength++;
        }
    }

    // 6. Write queue file
    Files.createDirectories(Path.of(SOCIAL_DIR));
    Files.writeString(Path.of(QUEUE_FILE), String.join("\n", queue) + "\n");
    System.out.println("Wrote " + QUEUE_FILE + " (" + queue.size() + " entries)");

    // 7. Write tweets file
    YAML_WRITER.writerWithDefaultPrettyPrinter().writeValue(Path.of(TWEETS_FILE).toFile(), tweets);
    System.out.println("Wrote " + TWEETS_FILE + " (" + tweets.size() + " entries)");

    // 8. Create state file if it doesn't exist
    if (!Files.exists(Path.of(STATE_FILE))) {
        var state = new LinkedHashMap<String, Object>();
        state.put("currentIndex", 1);
        state.put("lastPostedKey", null);
        state.put("lastTweetId", null);
        state.put("lastPostedAt", null);
        YAML_WRITER.writerWithDefaultPrettyPrinter().writeValue(Path.of(STATE_FILE).toFile(), state);
        System.out.println("Created " + STATE_FILE);
    }

    if (truncated > 0) System.out.println(truncated + " tweets were truncated to fit 280 chars");
    if (overLength > 0) System.err.println("WARNING: " + overLength + " tweets still exceed 280 chars — edit manually in " + TWEETS_FILE);
    System.out.println("Done!");
}

Map<String, PatternInfo> scanContentFiles() throws Exception {
    var patterns = new LinkedHashMap<String, PatternInfo>();
    var contentDir = Path.of(CONTENT_DIR);

    try (var categories = Files.list(contentDir)) {
        for (var catDir : categories.filter(Files::isDirectory).sorted().toList()) {
            var category = catDir.getFileName().toString();
            try (var files = Files.list(catDir)) {
                for (var file : files.filter(f -> isContentFile(f)).sorted().toList()) {
                    var node = YAML_MAPPER.readTree(file.toFile());
                    var slug = node.path("slug").asText();
                    var info = new PatternInfo(
                        category, slug,
                        node.path("title").asText(),
                        node.path("summary").asText(),
                        node.path("oldApproach").asText(),
                        node.path("modernApproach").asText(),
                        node.path("jdkVersion").asText()
                    );
                    patterns.put(info.key(), info);
                }
            }
        }
    }
    return patterns;
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
Map<String, String> loadExistingTweets() throws Exception {
    var path = Path.of(TWEETS_FILE);
    if (!Files.exists(path)) return Map.of();
    return YAML_MAPPER.readValue(path.toFile(), LinkedHashMap.class);
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
