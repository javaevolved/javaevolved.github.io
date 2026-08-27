///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3
//DEPS org.yaml:snakeyaml:2.4
//SOURCES og/Palette.java
//SOURCES og/Layout.java
//SOURCES og/ContentLoader.java
//SOURCES og/SyntaxHighlighter.java
//SOURCES og/SvgRenderer.java
//SOURCES og/PngRenderer.java
//SOURCES og/FontManager.java

import module java.base;
import og.*;
import og.ContentLoader.Snippet;

/**
 * Generate Open Graph SVG + PNG cards (1200×630) for each pattern.
 * PNG is rendered directly via Graphics2D (no Batik).
 *
 * Usage: jbang html-generators/generateog.java [category/slug]
 *        No arguments → generate all patterns.
 */
static final String OUTPUT_DIR = "site/og";

void main(String... args) throws Exception {
    FontManager.ensureFonts();

    var allSnippets = ContentLoader.loadAllSnippets();
    IO.println("Loaded %d snippets".formatted(allSnippets.size()));

    // Filter to a single slug if provided
    Collection<Snippet> targets;
    if (args.length > 0) {
        var key = args[0];
        if (!allSnippets.containsKey(key)) {
            IO.println("Unknown pattern: " + key);
            IO.println("Available: " + String.join(", ", allSnippets.keySet()));
            System.exit(1);
        }
        targets = List.of(allSnippets.get(key));
    } else {
        targets = allSnippets.values();
    }

    int count = 0;
    for (var s : targets) {
        var dir = Path.of(OUTPUT_DIR, s.category());
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(s.slug() + ".svg"), SvgRenderer.generate(s));
        PngRenderer.generate(s, dir.resolve(s.slug() + ".png"));
        count++;
    }
    IO.println("Generated %d SVG+PNG card(s) in %s/".formatted(count, OUTPUT_DIR));
}
