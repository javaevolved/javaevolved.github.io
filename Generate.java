///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3

import module java.base;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Generate HTML detail pages from JSON snippet files and slug-template.html.
 * JBang equivalent of generate.py — produces identical output.
 *
 * Uses modern Java features up to Java 25:
 *   - Compact source file / void main()  (JEP 512)
 *   - Module imports                      (JEP 511)
 *   - Records                             (JEP 395)
 *   - Sealed interfaces                   (JEP 409)
 *   - Pattern matching for switch         (JEP 441)
 *   - Text blocks                         (JEP 378)
 *   - Unnamed variables (_)               (JEP 456)
 *   - Stream.toList()                     (JDK 16)
 *   - String.formatted()                  (JDK 15)
 *   - SequencedMap                        (JDK 21)
 */

static final String BASE_URL = "https://javaevolved.github.io";
static final String TEMPLATE_FILE = "templates/slug-template.html";
static final String WHY_CARD_TEMPLATE = "templates/why-card.html";
static final String RELATED_CARD_TEMPLATE = "templates/related-card.html";
static final String SOCIAL_SHARE_TEMPLATE = "templates/social-share.html";
static final String CONTENT_DIR = "content";
static final String SITE_DIR = "site";
static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");
static final ObjectMapper MAPPER = new ObjectMapper();

static final SequencedMap<String, String> CATEGORY_DISPLAY = buildCategoryDisplay();

static SequencedMap<String, String> buildCategoryDisplay() {
    var map = new LinkedHashMap<String, String>();
    map.put("language", "Language");
    map.put("collections", "Collections");
    map.put("strings", "Strings");
    map.put("streams", "Streams");
    map.put("concurrency", "Concurrency");
    map.put("io", "I/O");
    map.put("errors", "Errors");
    map.put("datetime", "Date/Time");
    map.put("security", "Security");
    map.put("tooling", "Tooling");
    return map;
}

static final Set<String> EXCLUDED_KEYS = Set.of("_path", "prev", "next", "related");

// -- Records for structured data -----------------------------------------

record Snippet(JsonNode node) {
    String get(String field) { return node.get(field).asText(); }
    String slug()       { return get("slug"); }
    String category()   { return get("category"); }
    String title()      { return get("title"); }
    String summary()    { return get("summary"); }
    String difficulty()  { return get("difficulty"); }
    String jdkVersion() { return get("jdkVersion"); }
    String oldLabel()   { return get("oldLabel"); }
    String modernLabel(){ return get("modernLabel"); }
    String oldCode()    { return get("oldCode"); }
    String modernCode() { return get("modernCode"); }
    String oldApproach(){ return get("oldApproach"); }
    String modernApproach() { return get("modernApproach"); }
    String explanation(){ return get("explanation"); }
    String support()    { return get("support"); }
    String key()        { return "%s/%s".formatted(category(), slug()); }
    String catDisplay() { return CATEGORY_DISPLAY.get(category()); }

    Optional<String> prev() {
        return node.has("prev") && !node.get("prev").isNull()
                ? Optional.of(node.get("prev").asText()) : Optional.empty();
    }

    Optional<String> next() {
        return node.has("next") && !node.get("next").isNull()
                ? Optional.of(node.get("next").asText()) : Optional.empty();
    }

    List<String> related() {
        var rel = node.get("related");
        if (rel == null) return List.of();
        List<String> paths = new ArrayList<>();
        rel.forEach(n -> paths.add(n.asText()));
        return paths;
    }

    JsonNode whyModernWins() { return node.get("whyModernWins"); }
}

// -- Sealed interface for nav arrow rendering ----------------------------

sealed interface NavArrow {
    record Link(String href) implements NavArrow {}
    record Disabled()        implements NavArrow {}
    record Empty()           implements NavArrow {}
}

// -- Entry point (compact source file, JEP 512) -------------------------

void main() throws IOException {
    var template = Files.readString(Path.of(TEMPLATE_FILE));
    var whyCardTemplate = Files.readString(Path.of(WHY_CARD_TEMPLATE));
    var relatedCardTemplate = Files.readString(Path.of(RELATED_CARD_TEMPLATE));
    var socialShareTemplate = Files.readString(Path.of(SOCIAL_SHARE_TEMPLATE));
    var allSnippets = loadAllSnippets();
    IO.println("Loaded %d snippets".formatted(allSnippets.size()));

    // Generate HTML files
    for (var snippet : allSnippets.values()) {
        var html = generateHtml(template, whyCardTemplate, relatedCardTemplate, socialShareTemplate,
                snippet, allSnippets).strip();
        Files.createDirectories(Path.of(SITE_DIR, snippet.category()));
        Files.writeString(Path.of(SITE_DIR, snippet.category(), snippet.slug() + ".html"), html);
    }
    IO.println("Generated %d HTML files".formatted(allSnippets.size()));

    // Rebuild data/snippets.json
    var snippetsList = allSnippets.values().stream()
            .map(s -> {
                Map<String, Object> map = MAPPER.convertValue(s.node(),
                        new TypeReference<LinkedHashMap<String, Object>>() {});
                EXCLUDED_KEYS.forEach(map::remove);
                return map;
            })
            .toList();

    Files.createDirectories(Path.of(SITE_DIR, "data"));
    var prettyMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    Files.writeString(Path.of(SITE_DIR, "data", "snippets.json"),
            prettyMapper.writeValueAsString(snippetsList) + "\n");
    IO.println("Rebuilt data/snippets.json with %d entries".formatted(snippetsList.size()));

    // Patch index.html with the current snippet count
    int count = allSnippets.size();
    var indexPath = Path.of(SITE_DIR, "index.html");
    var indexContent = Files.readString(indexPath)
            .replace("{{snippetCount}}", String.valueOf(count));
    Files.writeString(indexPath, indexContent);
    IO.println("Patched index.html with snippet count: %d".formatted(count));
}

// -- Loading snippets ----------------------------------------------------

SequencedMap<String, Snippet> loadAllSnippets() throws IOException {
    SequencedMap<String, Snippet> snippets = new LinkedHashMap<>();
    for (var cat : CATEGORY_DISPLAY.sequencedKeySet()) {
        var catDir = Path.of(CONTENT_DIR, cat);
        if (!Files.isDirectory(catDir)) continue;

        try (var stream = Files.newDirectoryStream(catDir, "*.json")) {
            var sorted = new ArrayList<Path>();
            stream.forEach(sorted::add);
            sorted.sort(Path::compareTo);

            for (var path : sorted) {
                var snippet = new Snippet(MAPPER.readTree(path.toFile()));
                snippets.put(snippet.key(), snippet);
            }
        }
    }
    return snippets;
}

// -- HTML escaping -------------------------------------------------------

String escape(String text) {
    return switch (text) {
        case null -> "";
        case String s -> s.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;")
                          .replace("\"", "&quot;")
                          .replace("'", "&#x27;");
    };
}

String jsonEscape(String text) {
    try {
        var quoted = MAPPER.writeValueAsString(text);
        var inner = quoted.substring(1, quoted.length() - 1);
        var sb = new StringBuilder(inner.length());
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c > 127) {
                sb.append("\\u%04x".formatted((int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    } catch (IOException _) {
        throw new RuntimeException("Failed to JSON-escape: " + text);
    }
}

String urlEncode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
}

// -- Rendering helpers ---------------------------------------------------

String renderNavArrows(Snippet snippet) {
    NavArrow prev = snippet.prev()
            .<NavArrow>map(p -> new NavArrow.Link("/" + p + ".html"))
            .orElse(new NavArrow.Disabled());
    NavArrow next = snippet.next()
            .<NavArrow>map(n -> new NavArrow.Link("/" + n + ".html"))
            .orElse(new NavArrow.Empty());

    return renderArrow(prev, "Previous pattern", "←")
            + "\n          "
            + renderArrow(next, "Next pattern", "→");
}

String renderArrow(NavArrow arrow, String label, String symbol) {
    return switch (arrow) {
        case NavArrow.Link(var href) ->
                "<a href=\"%s\" aria-label=\"%s\">%s</a>".formatted(href, label, symbol);
        case NavArrow.Disabled() ->
                "<span class=\"nav-arrow-disabled\">%s</span>".formatted(symbol);
        case NavArrow.Empty() -> "";
    };
}

String renderWhyCards(String whyCardTemplate, JsonNode whyList) {
    var cards = new ArrayList<String>();
    for (var w : whyList) {
        var replacements = Map.of(
                "icon",  w.get("icon").asText(),
                "title", escape(w.get("title").asText()),
                "desc",  escape(w.get("desc").asText()));
        cards.add(replaceTokens(whyCardTemplate, replacements));
    }
    return String.join("\n", cards);
}

String renderRelatedCard(String relatedCardTemplate, Snippet rel) {
    var replacements = Map.ofEntries(
            Map.entry("category",    rel.category()),
            Map.entry("slug",        rel.slug()),
            Map.entry("catDisplay",  rel.catDisplay()),
            Map.entry("difficulty",  rel.difficulty()),
            Map.entry("title",       escape(rel.title())),
            Map.entry("oldLabel",    escape(rel.oldLabel())),
            Map.entry("oldCode",     escape(rel.oldCode())),
            Map.entry("modernLabel", escape(rel.modernLabel())),
            Map.entry("modernCode",  escape(rel.modernCode())),
            Map.entry("jdkVersion",  rel.jdkVersion()));
    return replaceTokens(relatedCardTemplate, replacements);
}

String renderRelatedSection(String relatedCardTemplate, Snippet snippet, Map<String, Snippet> allSnippets) {
    return snippet.related().stream()
            .filter(allSnippets::containsKey)
            .map(path -> renderRelatedCard(relatedCardTemplate, allSnippets.get(path)))
            .collect(Collectors.joining("\n"));
}

String renderSocialShare(String socialShareTemplate, String slug, String title) {
    var pageUrl = "%s/%s.html".formatted(BASE_URL, slug);
    var shareText = "%s \u2013 java.evolved".formatted(title);
    var encodedUrl = urlEncode(pageUrl);
    var encodedText = urlEncode(shareText);

    var replacements = Map.of(
            "encodedUrl",  encodedUrl,
            "encodedText", encodedText);
    return replaceTokens(socialShareTemplate, replacements);
}

// -- Main generation logic -----------------------------------------------

String generateHtml(String template, String whyCardTemplate, String relatedCardTemplate,
        String socialShareTemplate, Snippet snippet, Map<String, Snippet> allSnippets) {
    var replacements = Map.ofEntries(
            Map.entry("title",              escape(snippet.title())),
            Map.entry("summary",            escape(snippet.summary())),
            Map.entry("slug",               snippet.slug()),
            Map.entry("category",           snippet.category()),
            Map.entry("categoryDisplay",    snippet.catDisplay()),
            Map.entry("difficulty",         snippet.difficulty()),
            Map.entry("jdkVersion",         snippet.jdkVersion()),
            Map.entry("oldLabel",           escape(snippet.oldLabel())),
            Map.entry("modernLabel",        escape(snippet.modernLabel())),
            Map.entry("oldCode",            escape(snippet.oldCode())),
            Map.entry("modernCode",         escape(snippet.modernCode())),
            Map.entry("oldApproach",        escape(snippet.oldApproach())),
            Map.entry("modernApproach",     escape(snippet.modernApproach())),
            Map.entry("explanation",        escape(snippet.explanation())),
            Map.entry("support",            escape(snippet.support())),
            Map.entry("canonicalUrl",       "%s/%s/%s.html".formatted(BASE_URL, snippet.category(), snippet.slug())),
            Map.entry("flatUrl",            "%s/%s.html".formatted(BASE_URL, snippet.slug())),
            Map.entry("titleJson",          jsonEscape(snippet.title())),
            Map.entry("summaryJson",        jsonEscape(snippet.summary())),
            Map.entry("categoryDisplayJson", jsonEscape(snippet.catDisplay())),
            Map.entry("navArrows",          renderNavArrows(snippet)),
            Map.entry("whyCards",           renderWhyCards(whyCardTemplate, snippet.whyModernWins())),
            Map.entry("relatedCards",       renderRelatedSection(relatedCardTemplate, snippet, allSnippets)),
            Map.entry("socialShare",        renderSocialShare(socialShareTemplate, snippet.slug(), snippet.title()))
    );

    return replaceTokens(template, replacements);
}

String replaceTokens(String template, Map<String, String> replacements) {
    var m = TOKEN_PATTERN.matcher(template);
    var sb = new StringBuilder();
    while (m.find()) {
        var key = m.group(1);
        m.appendReplacement(sb, Matcher.quoteReplacement(
                replacements.getOrDefault(key, m.group(0))));
    }
    m.appendTail(sb);
    return sb.toString();
}
