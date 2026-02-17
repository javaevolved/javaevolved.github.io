///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3

import module java.base;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

/**
 * Generate HTML detail pages from JSON snippet files and slug-template.html.
 * JBang equivalent of generate.py — produces identical output.
 */
static final String BASE_URL = "https://javaevolved.github.io";
static final String CONTENT_DIR = "content";
static final String SITE_DIR = "site";
static final Pattern TOKEN = Pattern.compile("\\{\\{(\\w+)}}");
static final ObjectMapper MAPPER = new ObjectMapper();

static final SequencedMap<String, String> CATEGORY_DISPLAY = new LinkedHashMap<>() {{
    put("language", "Language"); put("collections", "Collections");
    put("strings", "Strings");   put("streams", "Streams");
    put("concurrency", "Concurrency"); put("io", "I/O");
    put("errors", "Errors");     put("datetime", "Date/Time");
    put("security", "Security"); put("tooling", "Tooling");
}};

static final Set<String> EXCLUDED_KEYS = Set.of("_path", "prev", "next", "related");

record Snippet(JsonNode node) {
    String get(String f)    { return node.get(f).asText(); }
    String slug()           { return get("slug"); }
    String category()       { return get("category"); }
    String title()          { return get("title"); }
    String summary()        { return get("summary"); }
    String difficulty()     { return get("difficulty"); }
    String jdkVersion()     { return get("jdkVersion"); }
    String oldLabel()       { return get("oldLabel"); }
    String modernLabel()    { return get("modernLabel"); }
    String oldCode()        { return get("oldCode"); }
    String modernCode()     { return get("modernCode"); }
    String oldApproach()    { return get("oldApproach"); }
    String modernApproach() { return get("modernApproach"); }
    String explanation()    { return get("explanation"); }
    String supportState()   { return node.get("support").get("state").asText(); }
    String supportDesc()    { return node.get("support").get("description").asText(); }
    String key()            { return category() + "/" + slug(); }
    String catDisplay()     { return CATEGORY_DISPLAY.get(category()); }
    JsonNode whyModernWins() { return node.get("whyModernWins"); }

    Optional<String> optText(String f) {
        var n = node.get(f);
        return n != null && !n.isNull() ? Optional.of(n.asText()) : Optional.empty();
    }

    List<String> related() {
        var rel = node.get("related");
        if (rel == null) return List.of();
        var paths = new ArrayList<String>();
        rel.forEach(n -> paths.add(n.asText()));
        return paths;
    }
}

record Templates(String page, String whyCard, String relatedCard, String socialShare,
                 String index, String indexCard, String docLink) {
    static Templates load() throws IOException {
        return new Templates(
            Files.readString(Path.of("templates/slug-template.html")),
            Files.readString(Path.of("templates/why-card.html")),
            Files.readString(Path.of("templates/related-card.html")),
            Files.readString(Path.of("templates/social-share.html")),
            Files.readString(Path.of("templates/index.html")),
            Files.readString(Path.of("templates/index-card.html")),
            Files.readString(Path.of("templates/doc-link.html")));
    }
}

void main() throws IOException {
    var templates = Templates.load();
    var allSnippets = loadAllSnippets();
    IO.println("Loaded %d snippets".formatted(allSnippets.size()));

    for (var snippet : allSnippets.values()) {
        var html = generateHtml(templates, snippet, allSnippets).strip();
        Files.createDirectories(Path.of(SITE_DIR, snippet.category()));
        Files.writeString(Path.of(SITE_DIR, snippet.category(), snippet.slug() + ".html"), html);
    }
    IO.println("Generated %d HTML files".formatted(allSnippets.size()));

    // Rebuild data/snippets.json
    var snippetsList = allSnippets.values().stream()
            .map(s -> {
                Map<String, Object> map = MAPPER.convertValue(s.node(), new TypeReference<LinkedHashMap<String, Object>>() {});
                EXCLUDED_KEYS.forEach(map::remove);
                return map;
            })
            .toList();

    Files.createDirectories(Path.of(SITE_DIR, "data"));
    var prettyMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    Files.writeString(Path.of(SITE_DIR, "data", "snippets.json"), prettyMapper.writeValueAsString(snippetsList) + "\n");
    IO.println("Rebuilt data/snippets.json with %d entries".formatted(snippetsList.size()));

    // Generate index.html from template
    var tipCards = allSnippets.values().stream()
            .map(s -> renderIndexCard(templates.indexCard(), s))
            .collect(Collectors.joining("\n"));
    var indexHtml = replaceTokens(templates.index(), Map.of(
            "tipCards", tipCards,
            "snippetCount", String.valueOf(allSnippets.size())));
    Files.writeString(Path.of(SITE_DIR, "index.html"), indexHtml);
    IO.println("Generated index.html with %d cards".formatted(allSnippets.size()));
}

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

String escape(String text) {
    return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
}

String jsonEscape(String text) throws IOException {
    var quoted = MAPPER.writeValueAsString(text);
    var inner = quoted.substring(1, quoted.length() - 1);
    var sb = new StringBuilder(inner.length());
    for (int i = 0; i < inner.length(); i++) {
        char c = inner.charAt(i);
        sb.append(c > 127 ? "\\u%04x".formatted((int) c) : String.valueOf(c));
    }
    return sb.toString();
}

String urlEncode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
}

String supportBadge(String state) {
    return switch (state) {
        case "preview" -> "Preview";
        case "experimental" -> "Experimental";
        default -> "Available";
    };
}

String supportBadgeClass(String state) {
    return switch (state) {
        case "preview" -> "preview";
        case "experimental" -> "experimental";
        default -> "widely";
    };
}

String renderNavArrows(Snippet snippet) {
    var prev = snippet.optText("prev")
            .map(p -> "<a href=\"/%s.html\" aria-label=\"Previous pattern\">←</a>".formatted(p))
            .orElse("<span class=\"nav-arrow-disabled\">←</span>");
    var next = snippet.optText("next")
            .map(n -> "<a href=\"/%s.html\" aria-label=\"Next pattern\">→</a>".formatted(n))
            .orElse("");
    return prev + "\n          " + next;
}

String renderIndexCard(String tpl, Snippet s) {
    return replaceTokens(tpl, Map.ofEntries(
            Map.entry("category", s.category()), Map.entry("slug", s.slug()),
            Map.entry("catDisplay", s.catDisplay()), Map.entry("title", escape(s.title())),
            Map.entry("oldCode", escape(s.oldCode())), Map.entry("modernCode", escape(s.modernCode())),
            Map.entry("jdkVersion", s.jdkVersion())));
}

String renderWhyCards(String tpl, JsonNode whyList) {
    var cards = new ArrayList<String>();
    for (var w : whyList)
        cards.add(replaceTokens(tpl, Map.of(
                "icon", w.get("icon").asText(),
                "title", escape(w.get("title").asText()),
                "desc", escape(w.get("desc").asText()))));
    return String.join("\n", cards);
}

String renderRelatedCard(String tpl, Snippet rel) {
    return replaceTokens(tpl, Map.ofEntries(
            Map.entry("category", rel.category()), Map.entry("slug", rel.slug()),
            Map.entry("catDisplay", rel.catDisplay()), Map.entry("difficulty", rel.difficulty()),
            Map.entry("title", escape(rel.title())),
            Map.entry("oldLabel", escape(rel.oldLabel())), Map.entry("oldCode", escape(rel.oldCode())),
            Map.entry("modernLabel", escape(rel.modernLabel())), Map.entry("modernCode", escape(rel.modernCode())),
            Map.entry("jdkVersion", rel.jdkVersion())));
}

String renderDocLinks(String tpl, JsonNode docs) {
    var links = new ArrayList<String>();
    for (var d : docs)
        links.add(replaceTokens(tpl, Map.of(
                "docTitle", escape(d.get("title").asText()),
                "docHref", d.get("href").asText())));
    return String.join("\n", links);
}

String renderRelatedSection(String tpl, Snippet snippet, Map<String, Snippet> all) {
    return snippet.related().stream().filter(all::containsKey)
            .map(p -> renderRelatedCard(tpl, all.get(p)))
            .collect(Collectors.joining("\n"));
}

String renderSocialShare(String tpl, String slug, String title) {
    var encodedUrl = urlEncode("%s/%s.html".formatted(BASE_URL, slug));
    var encodedText = urlEncode("%s \u2013 java.evolved".formatted(title));
    return replaceTokens(tpl, Map.of("encodedUrl", encodedUrl, "encodedText", encodedText));
}

String generateHtml(Templates tpl, Snippet s, Map<String, Snippet> all) throws IOException {
    return replaceTokens(tpl.page(), Map.ofEntries(
            Map.entry("title", escape(s.title())), Map.entry("summary", escape(s.summary())),
            Map.entry("slug", s.slug()), Map.entry("category", s.category()),
            Map.entry("categoryDisplay", s.catDisplay()), Map.entry("difficulty", s.difficulty()),
            Map.entry("jdkVersion", s.jdkVersion()),
            Map.entry("oldLabel", escape(s.oldLabel())), Map.entry("modernLabel", escape(s.modernLabel())),
            Map.entry("oldCode", escape(s.oldCode())), Map.entry("modernCode", escape(s.modernCode())),
            Map.entry("oldApproach", escape(s.oldApproach())), Map.entry("modernApproach", escape(s.modernApproach())),
            Map.entry("explanation", escape(s.explanation())),
            Map.entry("supportDescription", escape(s.supportDesc())),
            Map.entry("supportBadge", supportBadge(s.supportState())),
            Map.entry("supportBadgeClass", supportBadgeClass(s.supportState())),
            Map.entry("canonicalUrl", "%s/%s/%s.html".formatted(BASE_URL, s.category(), s.slug())),
            Map.entry("flatUrl", "%s/%s.html".formatted(BASE_URL, s.slug())),
            Map.entry("titleJson", jsonEscape(s.title())), Map.entry("summaryJson", jsonEscape(s.summary())),
            Map.entry("categoryDisplayJson", jsonEscape(s.catDisplay())),
            Map.entry("navArrows", renderNavArrows(s)),
            Map.entry("whyCards", renderWhyCards(tpl.whyCard(), s.whyModernWins())),
            Map.entry("docLinks", renderDocLinks(tpl.docLink(), s.node().withArray("docs"))),
            Map.entry("relatedCards", renderRelatedSection(tpl.relatedCard(), s, all)),
            Map.entry("socialShare", renderSocialShare(tpl.socialShare(), s.slug(), s.title()))));
}

String replaceTokens(String template, Map<String, String> replacements) {
    var m = TOKEN.matcher(template);
    var sb = new StringBuilder();
    while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(replacements.getOrDefault(m.group(1), m.group(0))));
    m.appendTail(sb);
    return sb.toString();
}
