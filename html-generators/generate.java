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
static final String TRANSLATIONS_DIR = "translations";
static final Pattern TOKEN = Pattern.compile("\\{\\{([\\w.]+)}}");
static final ObjectMapper MAPPER = new ObjectMapper();

static final String CATEGORIES_FILE = "html-generators/categories.properties";
static final String LOCALES_FILE = "html-generators/locales.properties";
static final SequencedMap<String, String> CATEGORY_DISPLAY = loadCategoryDisplay();
static final SequencedMap<String, String> LOCALES = loadLocales();

static SequencedMap<String, String> loadCategoryDisplay() {
    try {
        var map = new LinkedHashMap<String, String>();
        for (var line : Files.readAllLines(Path.of(CATEGORIES_FILE))) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var idx = line.indexOf('=');
            if (idx > 0) map.put(line.substring(0, idx).strip(), line.substring(idx + 1).strip());
        }
        return map;
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}

static SequencedMap<String, String> loadLocales() {
    try {
        var map = new LinkedHashMap<String, String>();
        for (var line : Files.readAllLines(Path.of(LOCALES_FILE))) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var idx = line.indexOf('=');
            if (idx > 0) map.put(line.substring(0, idx).strip(), line.substring(idx + 1).strip());
        }
        return map;
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}

/** Flatten nested JSON into dot-separated keys: {"a":{"b":"c"}} → {"a.b":"c"} */
static Map<String, String> flattenJson(JsonNode node, String prefix) {
    var map = new LinkedHashMap<String, String>();
    var it = node.fields();
    while (it.hasNext()) {
        var entry = it.next();
        var key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
        if (entry.getValue().isObject()) {
            map.putAll(flattenJson(entry.getValue(), key));
        } else {
            map.put(key, entry.getValue().asText());
        }
    }
    return map;
}

/** Load UI strings for a locale, falling back to en.json for missing keys */
static Map<String, String> loadStrings(String locale) throws IOException {
    var enPath = Path.of(TRANSLATIONS_DIR, "strings", "en.json");
    var enStrings = flattenJson(MAPPER.readTree(enPath.toFile()), "");

    if (locale.equals("en")) return enStrings;

    var localePath = Path.of(TRANSLATIONS_DIR, "strings", locale + ".json");
    if (!Files.exists(localePath)) {
        IO.println("[WARN] strings/%s.json not found — using all English strings".formatted(locale));
        return enStrings;
    }

    var localeStrings = flattenJson(MAPPER.readTree(localePath.toFile()), "");
    var merged = new LinkedHashMap<>(enStrings);
    for (var entry : localeStrings.entrySet()) {
        if (enStrings.containsKey(entry.getKey())) {
            merged.put(entry.getKey(), entry.getValue());
        }
    }
    // Warn about missing keys
    for (var key : enStrings.keySet()) {
        if (!localeStrings.containsKey(key)) {
            IO.println("[WARN] strings/%s.json: missing key \"%s\" — using English fallback".formatted(locale, key));
        }
    }
    return merged;
}

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

void main(String... args) throws IOException {
    var templates = Templates.load();
    var allSnippets = loadAllSnippets();
    IO.println("Loaded %d snippets".formatted(allSnippets.size()));

    // Determine which locales to build
    List<String> localesToBuild;
    if (args.length > 0 && args[0].equals("--all-locales")) {
        localesToBuild = new ArrayList<>(LOCALES.sequencedKeySet());
    } else if (args.length > 1 && args[0].equals("--locale")) {
        localesToBuild = List.of(args[1]);
    } else {
        localesToBuild = new ArrayList<>(LOCALES.sequencedKeySet());
    }

    for (var locale : localesToBuild) {
        buildLocale(locale, templates, allSnippets);
    }
}

void buildLocale(String locale, Templates templates, SequencedMap<String, Snippet> allSnippets) throws IOException {
    var isEnglish = locale.equals("en");
    var strings = loadStrings(locale);
    var localeName = LOCALES.getOrDefault(locale, locale);
    var sitePrefix = isEnglish ? "" : locale + "/";
    // basePrefix is the relative path from a detail page back to site root
    var basePrefix = isEnglish ? "../" : "../../";
    var homeUrl = isEnglish ? "/" : "/%s/".formatted(locale);

    IO.println("Building locale: %s (%s)".formatted(locale, localeName));

    // Build locale picker HTML
    var localePickerHtml = renderLocalePicker(locale);
    // Build hreflang links for index
    var indexHreflang = renderHreflangLinks("", "index");
    // Build i18n script block
    var i18nScript = renderI18nScript(strings, locale);

    // Load translated content if available
    for (var snippet : allSnippets.values()) {
        var resolved = resolveSnippet(snippet, locale);
        var detailHreflang = renderHreflangLinks(snippet.category() + "/", snippet.slug());

        var extraTokens = new LinkedHashMap<String, String>();
        extraTokens.putAll(strings);
        extraTokens.put("locale", locale);
        extraTokens.put("ogLocale", locale.replace("-", "_"));
        extraTokens.put("basePrefix", basePrefix);
        extraTokens.put("homeUrl", homeUrl);
        extraTokens.put("localePicker", localePickerHtml);
        extraTokens.put("hreflangLinks", detailHreflang);
        extraTokens.put("i18nScript", i18nScript);

        var html = generateHtml(templates, resolved, allSnippets, extraTokens, locale).strip();

        if (isEnglish) {
            Files.createDirectories(Path.of(SITE_DIR, snippet.category()));
            Files.writeString(Path.of(SITE_DIR, snippet.category(), snippet.slug() + ".html"), html);
        } else {
            Files.createDirectories(Path.of(SITE_DIR, locale, snippet.category()));
            Files.writeString(Path.of(SITE_DIR, locale, snippet.category(), snippet.slug() + ".html"), html);
        }
    }
    IO.println("Generated %d HTML files for %s".formatted(allSnippets.size(), locale));

    // Rebuild data/snippets.json
    var snippetsList = allSnippets.values().stream()
            .map(s -> {
                var resolved = resolveSnippet(s, locale);
                Map<String, Object> map = MAPPER.convertValue(resolved.node(), new TypeReference<LinkedHashMap<String, Object>>() {});
                EXCLUDED_KEYS.forEach(map::remove);
                return map;
            })
            .toList();

    var dataDir = isEnglish ? Path.of(SITE_DIR, "data") : Path.of(SITE_DIR, locale, "data");
    Files.createDirectories(dataDir);
    var prettyMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    Files.writeString(dataDir.resolve("snippets.json"), prettyMapper.writeValueAsString(snippetsList) + "\n");
    IO.println("Rebuilt data/snippets.json for %s with %d entries".formatted(locale, snippetsList.size()));

    // Generate index.html from template
    var tipCards = allSnippets.values().stream()
            .map(s -> renderIndexCard(templates.indexCard(), s, locale, strings))
            .collect(Collectors.joining("\n"));

    var indexTokens = new LinkedHashMap<String, String>();
    indexTokens.putAll(strings);
    indexTokens.put("tipCards", tipCards);
    indexTokens.put("snippetCount", String.valueOf(allSnippets.size()));
    indexTokens.put("locale", locale);
    indexTokens.put("ogLocale", locale.replace("-", "_"));
    indexTokens.put("canonicalUrl", isEnglish ? BASE_URL : BASE_URL + "/" + locale);
    indexTokens.put("homeUrl", homeUrl);
    indexTokens.put("indexBasePrefix", isEnglish ? "" : "../");
    indexTokens.put("localePicker", localePickerHtml);
    indexTokens.put("hreflangLinks", indexHreflang);
    indexTokens.put("i18nScript", i18nScript);

    var indexHtml = replaceTokens(templates.index(), indexTokens);
    var indexPath = isEnglish ? Path.of(SITE_DIR, "index.html") : Path.of(SITE_DIR, locale, "index.html");
    if (!isEnglish) Files.createDirectories(indexPath.getParent());
    Files.writeString(indexPath, indexHtml);
    IO.println("Generated index.html for %s with %d cards".formatted(locale, allSnippets.size()));
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

String supportBadge(String state, Map<String, String> strings) {
    return switch (state) {
        case "preview" -> strings.getOrDefault("support.preview", "Preview");
        case "experimental" -> strings.getOrDefault("support.experimental", "Experimental");
        default -> strings.getOrDefault("support.available", "Available");
    };
}

String supportBadgeClass(String state) {
    return switch (state) {
        case "preview" -> "preview";
        case "experimental" -> "experimental";
        default -> "widely";
    };
}

String renderNavArrows(Snippet snippet, String locale) {
    var prefix = locale.equals("en") ? "" : "/" + locale;
    var prev = snippet.optText("prev")
            .map(p -> "<a href=\"%s/%s.html\" aria-label=\"Previous pattern\">←</a>".formatted(prefix, p))
            .orElse("<span class=\"nav-arrow-disabled\">←</span>");
    var next = snippet.optText("next")
            .map(n -> "<a href=\"%s/%s.html\" aria-label=\"Next pattern\">→</a>".formatted(prefix, n))
            .orElse("");
    return prev + "\n          " + next;
}

String renderIndexCard(String tpl, Snippet s, String locale, Map<String, String> strings) {
    var cardHref = locale.equals("en")
            ? "/%s/%s.html".formatted(s.category(), s.slug())
            : "/%s/%s/%s.html".formatted(locale, s.category(), s.slug());
    return replaceTokens(tpl, Map.ofEntries(
            Map.entry("category", s.category()), Map.entry("slug", s.slug()),
            Map.entry("catDisplay", s.catDisplay()), Map.entry("title", escape(s.title())),
            Map.entry("oldCode", escape(s.oldCode())), Map.entry("modernCode", escape(s.modernCode())),
            Map.entry("jdkVersion", s.jdkVersion()), Map.entry("cardHref", cardHref),
            Map.entry("cards.old", strings.getOrDefault("cards.old", "Old")),
            Map.entry("cards.modern", strings.getOrDefault("cards.modern", "Modern")),
            Map.entry("cards.hoverHint", strings.getOrDefault("cards.hoverHint", "hover to see modern →"))));
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

String renderRelatedCard(String tpl, Snippet rel, String locale, Map<String, String> strings) {
    var relatedHref = locale.equals("en")
            ? "/%s/%s.html".formatted(rel.category(), rel.slug())
            : "/%s/%s/%s.html".formatted(locale, rel.category(), rel.slug());
    return replaceTokens(tpl, Map.ofEntries(
            Map.entry("category", rel.category()), Map.entry("slug", rel.slug()),
            Map.entry("catDisplay", rel.catDisplay()), Map.entry("difficulty", rel.difficulty()),
            Map.entry("title", escape(rel.title())),
            Map.entry("oldLabel", escape(rel.oldLabel())), Map.entry("oldCode", escape(rel.oldCode())),
            Map.entry("modernLabel", escape(rel.modernLabel())), Map.entry("modernCode", escape(rel.modernCode())),
            Map.entry("jdkVersion", rel.jdkVersion()), Map.entry("relatedHref", relatedHref),
            Map.entry("cards.hoverHintRelated", strings.getOrDefault("cards.hoverHintRelated", "Hover to see modern ➜"))));
}

String renderDocLinks(String tpl, JsonNode docs) {
    var links = new ArrayList<String>();
    for (var d : docs)
        links.add(replaceTokens(tpl, Map.of(
                "docTitle", escape(d.get("title").asText()),
                "docHref", d.get("href").asText())));
    return String.join("\n", links);
}

String renderRelatedSection(String tpl, Snippet snippet, Map<String, Snippet> all, String locale, Map<String, String> strings) {
    return snippet.related().stream().filter(all::containsKey)
            .map(p -> renderRelatedCard(tpl, all.get(p), locale, strings))
            .collect(Collectors.joining("\n"));
}

String renderSocialShare(String tpl, String slug, String title, Map<String, String> strings) {
    var encodedUrl = urlEncode("%s/%s.html".formatted(BASE_URL, slug));
    var encodedText = urlEncode("%s \u2013 java.evolved".formatted(title));
    return replaceTokens(tpl, Map.of("encodedUrl", encodedUrl, "encodedText", encodedText,
            "share.label", strings.getOrDefault("share.label", "Share")));
}

String generateHtml(Templates tpl, Snippet s, Map<String, Snippet> all, Map<String, String> extraTokens, String locale) throws IOException {
    var isEnglish = locale.equals("en");
    var canonicalUrl = isEnglish
            ? "%s/%s/%s.html".formatted(BASE_URL, s.category(), s.slug())
            : "%s/%s/%s/%s.html".formatted(BASE_URL, locale, s.category(), s.slug());

    var tokens = new LinkedHashMap<>(extraTokens);
    tokens.putAll(Map.ofEntries(
            Map.entry("title", escape(s.title())), Map.entry("summary", escape(s.summary())),
            Map.entry("slug", s.slug()), Map.entry("category", s.category()),
            Map.entry("categoryDisplay", s.catDisplay()), Map.entry("difficulty", s.difficulty()),
            Map.entry("jdkVersion", s.jdkVersion()),
            Map.entry("oldLabel", escape(s.oldLabel())), Map.entry("modernLabel", escape(s.modernLabel())),
            Map.entry("oldCode", escape(s.oldCode())), Map.entry("modernCode", escape(s.modernCode())),
            Map.entry("oldApproach", escape(s.oldApproach())), Map.entry("modernApproach", escape(s.modernApproach())),
            Map.entry("explanation", escape(s.explanation())),
            Map.entry("supportDescription", escape(s.supportDesc())),
            Map.entry("supportBadge", supportBadge(s.supportState(), extraTokens)),
            Map.entry("supportBadgeClass", supportBadgeClass(s.supportState())),
            Map.entry("canonicalUrl", canonicalUrl),
            Map.entry("flatUrl", "%s/%s.html".formatted(BASE_URL, s.slug())),
            Map.entry("titleJson", jsonEscape(s.title())), Map.entry("summaryJson", jsonEscape(s.summary())),
            Map.entry("categoryDisplayJson", jsonEscape(s.catDisplay())),
            Map.entry("navArrows", renderNavArrows(s, locale)),
            Map.entry("whyCards", renderWhyCards(tpl.whyCard(), s.whyModernWins())),
            Map.entry("docLinks", renderDocLinks(tpl.docLink(), s.node().withArray("docs"))),
            Map.entry("relatedCards", renderRelatedSection(tpl.relatedCard(), s, all, locale, extraTokens)),
            Map.entry("socialShare", renderSocialShare(tpl.socialShare(), s.slug(), s.title(), extraTokens))));
    return replaceTokens(tpl.page(), tokens);
}

/** Load translated content or fall back to English; overwrite code fields from English */
Snippet resolveSnippet(Snippet englishSnippet, String locale) {
    if (locale.equals("en")) return englishSnippet;

    var translatedPath = Path.of(TRANSLATIONS_DIR, "content", locale,
            englishSnippet.category(), englishSnippet.slug() + ".json");
    if (!Files.exists(translatedPath)) return englishSnippet;

    try {
        var translatedNode = (com.fasterxml.jackson.databind.node.ObjectNode) MAPPER.readTree(translatedPath.toFile());
        // Overwrite code fields with English values
        translatedNode.put("oldCode", englishSnippet.oldCode());
        translatedNode.put("modernCode", englishSnippet.modernCode());
        return new Snippet(translatedNode);
    } catch (IOException e) {
        IO.println("[WARN] Failed to load %s — using English".formatted(translatedPath));
        return englishSnippet;
    }
}

/** Render hreflang <link> tags for all locales */
String renderHreflangLinks(String pathPart, String slug) {
    var sb = new StringBuilder();
    for (var entry : LOCALES.entrySet()) {
        var loc = entry.getKey();
        String href;
        if (slug.equals("index")) {
            href = loc.equals("en") ? BASE_URL + "/" : BASE_URL + "/" + loc + "/";
        } else {
            href = loc.equals("en")
                    ? "%s/%s%s.html".formatted(BASE_URL, pathPart, slug)
                    : "%s/%s/%s%s.html".formatted(BASE_URL, loc, pathPart, slug);
        }
        sb.append("  <link rel=\"alternate\" hreflang=\"%s\" href=\"%s\">\n".formatted(loc, href));
    }
    // x-default points to English
    var defaultHref = slug.equals("index")
            ? BASE_URL + "/"
            : "%s/%s%s.html".formatted(BASE_URL, pathPart, slug);
    sb.append("  <link rel=\"alternate\" hreflang=\"x-default\" href=\"%s\">".formatted(defaultHref));
    return sb.toString();
}

/** Render the locale picker dropdown HTML */
String renderLocalePicker(String currentLocale) {
    var sb = new StringBuilder();
    sb.append("        <div class=\"locale-picker\" id=\"localePicker\">\n");
    sb.append("          <button type=\"button\" class=\"locale-toggle\" aria-haspopup=\"listbox\" aria-expanded=\"false\"\n");
    sb.append("                  aria-label=\"Select language\">🌐</button>\n");
    sb.append("          <ul role=\"listbox\" aria-label=\"Language\">\n");
    for (var entry : LOCALES.entrySet()) {
        var selected = entry.getKey().equals(currentLocale);
        sb.append("            <li role=\"option\" data-locale=\"%s\" aria-selected=\"%s\"%s>%s</li>\n"
                .formatted(entry.getKey(), selected, selected ? " class=\"active\"" : "", entry.getValue()));
    }
    sb.append("          </ul>\n");
    sb.append("        </div>");
    return sb.toString();
}

/** Render the i18n script block for client-side JS */
String renderI18nScript(Map<String, String> strings, String locale) {
    var localeArray = LOCALES.keySet().stream()
        .map(l -> "\"" + l + "\"")
        .collect(java.util.stream.Collectors.joining(", "));
    return """
      <script>
        window.i18n = {
          locale: "%s",
          availableLocales: [%s],
          searchPlaceholder: "%s",
          noResults: "%s",
          copied: "%s",
          expandAll: "%s",
          collapseAll: "%s",
          hoverHint: "%s",
          touchHint: "%s"
        };
      </script>""".formatted(
            locale,
            localeArray,
            jsEscape(strings.getOrDefault("search.placeholder", "Search snippets…")),
            jsEscape(strings.getOrDefault("search.noResults", "No results found.")),
            jsEscape(strings.getOrDefault("copy.copied", "Copied!")),
            jsEscape(strings.getOrDefault("view.expandAll", "Expand All")),
            jsEscape(strings.getOrDefault("view.collapseAll", "Collapse All")),
            jsEscape(strings.getOrDefault("cards.hoverHint", "hover to see modern →")),
            jsEscape(strings.getOrDefault("cards.touchHint", "👆 tap or swipe →")));
}

String jsEscape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
}

String replaceTokens(String template, Map<String, String> replacements) {
    // Loop to handle tokens within replacement values (e.g., {{snippetCount}} inside i18n strings)
    var result = template;
    for (int pass = 0; pass < 3; pass++) {
        var m = TOKEN.matcher(result);
        var sb = new StringBuilder();
        boolean found = false;
        while (m.find()) {
            var replacement = replacements.get(m.group(1));
            if (replacement != null) {
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                found = true;
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        result = sb.toString();
        if (!found) break;
    }
    return result;
}
