///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3

import module java.base;
import java.net.http.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Post the next tweet from the social queue to Twitter/X.
 *
 * Reads state from social/state.yaml, posts via Twitter API v2,
 * and updates state only after confirmed API success.
 *
 * Required environment variables:
 *   TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_KEY_SECRET,
 *   TWITTER_ACCESS_TOKEN, TWITTER_ACCESS_TOKEN_SECRET
 *
 * Options:
 *   --dry-run   Print the tweet without posting
 */

static final String SOCIAL_DIR = "social";
static final String QUEUE_FILE = SOCIAL_DIR + "/queue.txt";
static final String TWEETS_DIR = SOCIAL_DIR + "/tweets";
static final String STATE_FILE = SOCIAL_DIR + "/state.yaml";
static final String TWITTER_API_URL = "https://api.twitter.com/2/tweets";

static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
static final ObjectMapper YAML_WRITER = new ObjectMapper(
    new YAMLFactory()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
);
static final ObjectMapper JSON_MAPPER = new ObjectMapper();

void main(String... args) throws Exception {
    boolean dryRun = List.of(args).contains("--dry-run");

    // 1. Load pending queue and state
    var queue = new ArrayList<>(loadQueue());
    var state = loadState();
    var posted = new ArrayList<>(loadPostedKeys(state));
    System.out.println("Queue has " + queue.size() + " pending entries");

    // 2. Get the first pending pattern and its draft
    var key = queue.getFirst();
    if (posted.contains(key)) {
        throw new IllegalStateException("Pending pattern is already marked posted: " + key);
    }
    var tweetText = loadTweet(key);

    System.out.println("Pattern: " + key);
    System.out.println("Tweet (" + tweetText.length() + " chars):");
    System.out.println("---");
    System.out.println(tweetText);
    System.out.println("---");

    if (dryRun) {
        System.out.println("DRY RUN — not posting.");
        return;
    }

    // 4. Read Twitter credentials from environment
    var consumerKey = requireEnv("TWITTER_CONSUMER_KEY");
    var consumerSecret = requireEnv("TWITTER_CONSUMER_KEY_SECRET");
    var accessToken = requireEnv("TWITTER_ACCESS_TOKEN");
    var accessTokenSecret = requireEnv("TWITTER_ACCESS_TOKEN_SECRET");

    // 5. Post to Twitter
    var tweetId = postTweet(tweetText, consumerKey, consumerSecret, accessToken, accessTokenSecret);
    System.out.println("Posted! Tweet ID: " + tweetId);

    // 6. Record success before removing the pending entry. If the queue write
    // fails, reconciliation will remove the posted key before the next run.
    posted.add(key);
    state.put("lastPostedKey", key);
    state.put("lastTweetId", tweetId);
    state.put("lastPostedAt", java.time.Instant.now().toString());
    state.put("postedKeys", posted);
    YAML_WRITER.writerWithDefaultPrettyPrinter().writeValue(Path.of(STATE_FILE).toFile(), state);
    queue.removeFirst();
    Files.writeString(Path.of(QUEUE_FILE), String.join("\n", queue) + "\n");
    System.out.println("State updated: " + queue.size() + " pending entries remain");
}

// --- Twitter API v2 with OAuth 1.0a ---

String postTweet(String text, String consumerKey, String consumerSecret,
                 String token, String tokenSecret) throws Exception {
    var method = "POST";
    var url = TWITTER_API_URL;

    // OAuth parameters
    var oauthParams = new TreeMap<String, String>();
    oauthParams.put("oauth_consumer_key", consumerKey);
    oauthParams.put("oauth_nonce", generateNonce());
    oauthParams.put("oauth_signature_method", "HMAC-SHA1");
    oauthParams.put("oauth_timestamp", String.valueOf(Instant.now().getEpochSecond()));
    oauthParams.put("oauth_token", token);
    oauthParams.put("oauth_version", "1.0");

    // Build signature base string (no body params for JSON content type)
    var paramString = oauthParams.entrySet().stream()
        .map(e -> percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()))
        .collect(Collectors.joining("&"));

    var baseString = method + "&" + percentEncode(url) + "&" + percentEncode(paramString);
    var signingKey = percentEncode(consumerSecret) + "&" + percentEncode(tokenSecret);

    var signature = hmacSha1(signingKey, baseString);
    oauthParams.put("oauth_signature", signature);

    // Build Authorization header
    var authHeader = "OAuth " + oauthParams.entrySet().stream()
        .map(e -> percentEncode(e.getKey()) + "=\"" + percentEncode(e.getValue()) + "\"")
        .collect(Collectors.joining(", "));

    // Build JSON body
    var bodyMap = Map.of("text", text);
    var body = JSON_MAPPER.writeValueAsString(bodyMap);

    // Send request
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Authorization", authHeader)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 201) {
        System.err.println("Twitter API error (HTTP " + response.statusCode() + "):");
        System.err.println(response.body());
        System.exit(1);
    }

    var responseNode = JSON_MAPPER.readTree(response.body());
    return responseNode.path("data").path("id").asText();
}

String generateNonce() {
    var bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
}

String hmacSha1(String key, String data) throws Exception {
    var mac = javax.crypto.Mac.getInstance("HmacSHA1");
    mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
    var raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(raw);
}

String percentEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8)
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~");
}

// --- File helpers ---

List<String> loadQueue() throws Exception {
    var lines = Files.readAllLines(Path.of(QUEUE_FILE)).stream()
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .toList();
    if (lines.isEmpty()) {
        System.err.println("ERROR: " + QUEUE_FILE + " is empty. Run the queue generator first.");
        System.exit(1);
    }
    return lines;
}

String loadTweet(String key) throws Exception {
    var parts = key.split("/", 2);
    if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid pattern key in queue: " + key);
    }
    var path = Path.of(TWEETS_DIR, parts[0], parts[1] + ".yaml");
    if (!Files.isRegularFile(path)) {
        throw new IllegalStateException("Missing tweet draft: " + path);
    }
    var text = YAML_MAPPER.readTree(path.toFile()).path("text");
    if (!text.isTextual() || text.asText().isBlank()) {
        throw new IllegalStateException(path + " must define non-empty text");
    }
    if (text.asText().length() > 280) {
        throw new IllegalStateException(path + " exceeds 280 characters");
    }
    return text.asText();
}

@SuppressWarnings("unchecked")
Map<String, Object> loadState() throws Exception {
    return YAML_MAPPER.readValue(Path.of(STATE_FILE).toFile(), LinkedHashMap.class);
}

List<String> loadPostedKeys(Map<String, Object> state) {
    var value = state.get("postedKeys");
    if (!(value instanceof List<?> values)
            || values.stream().anyMatch(item -> !(item instanceof String))) {
        throw new IllegalArgumentException("social/state.yaml postedKeys must be a string list");
    }
    return values.stream().map(String.class::cast).toList();
}

String requireEnv(String name) {
    var value = System.getenv(name);
    if (value == null || value.isBlank()) {
        System.err.println("ERROR: Missing environment variable: " + name);
        System.exit(1);
    }
    return value;
}
