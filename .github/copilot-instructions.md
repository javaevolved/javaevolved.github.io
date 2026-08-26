# Copilot Instructions for java.evolved

## Build & Serve

```bash
jbang html-generators/generate.java                # Rebuild all locales
jbang html-generators/generate.java --locale es     # Rebuild single locale
jwebserver -b 0.0.0.0 -d site -p 8090              # Serve locally
```

Requires **Java 25+** and **JBang**. No npm, Maven, or Gradle.

**Validation:** There is no test suite. Validate changes by running the generator and confirming it completes without errors. The `proof/` directory contains one JBang script per pattern that proves the modern code compiles:

```bash
jbang proof/language/TypeInferenceWithVar.java      # Run single proof
```

## Architecture

Static site generator: YAML content → JBang generator → HTML pages.

- **`content/`** — Source of truth. One YAML/JSON file per pattern, organized by category.
- **`templates/`** — HTML templates with `{{placeholder}}` tokens. The generator replaces tokens with content fields and UI strings.
- **`html-generators/generate.java`** — JBang script that reads content + translations + templates and produces all HTML under `site/`.
- **`site/app.js`** and **`site/styles.css`** — Manually maintained client-side code (vanilla JS, no frameworks).
- **`translations/strings/{locale}.yaml`** — UI string translations (labels, nav, footer). Tokens use dotted keys: `{{nav.allPatterns}}`.
- **`translations/content/{locale}/`** — Partial content translations (only translatable fields; structural data always comes from English source).
- **`proof/{category}/{PascalCaseSlug}.java`** — JBang scripts proving each pattern's modern code compiles on Java 25.

### Generated files — DO NOT EDIT directly

Everything under `site/` except `app.js` and `styles.css` is generated:
- `site/index.html`, `site/{category}/*.html`, `site/data/snippets.json`
- `site/{locale}/index.html`, `site/{locale}/{category}/*.html`, `site/{locale}/data/snippets.json`

Run the generator to rebuild after any content, template, or translation change.

## Content Schema

Content files are YAML (preferred) or JSON under `content/{category}/{slug}.yaml`. The generator auto-detects format by extension (`.yaml`, `.yml`, `.json`).

### Required fields

| Field | Constraint |
|-------|-----------|
| `slug` | Must match filename (without extension) |
| `category` | Must match parent folder name |
| `whyModernWins` | Exactly **3** entries, each with `icon`, `title`, `desc` |
| `related` | Exactly **3** entries as `category/slug` paths (cross-category OK) |
| `tags` | Non-empty list of slugs registered in `html-generators/tags.properties` |
| `docs` | At least **1** entry with `title` and `href` |
| `prev` / `next` | `category/slug` path or `null` for first/last in the global chain |
| `jdkVersion` | The JDK version where the feature became **final** (not preview) |
| `difficulty` | One of: `beginner`, `intermediate`, `advanced` |
| `support.state` | One of: `available`, `preview`, `experimental` |

### Adding a new pattern

1. Create `content/{category}/new-slug.yaml` with all required fields (use `content/template.json` as reference).
2. Add a non-empty `tags` list. Every tag slug must already exist in `html-generators/tags.properties`; add new `slug=Display Name` entries there in the same change.
3. Update `prev`/`next` in the adjacent patterns to maintain the navigation chain.
4. Create `proof/{category}/{PascalCaseSlug}.java` — JBang script wrapping the modern code.
5. Run `jbang html-generators/generate.java` and verify it completes. The generator rejects missing, empty, malformed, and unregistered tags.
6. Translations are optional — the AI translation workflow handles them, or create partial files under `translations/content/{locale}/`.

### Removing or reordering a pattern

Update `prev`/`next` in adjacent patterns. Search for the slug in other patterns' `related` arrays and replace with an appropriate alternative.

## Internationalization

Full spec: `specs/i18n/i18n-spec.md`. Key rules:

- All locales (including English) go through the same build pipeline.
- UI strings: `translations/strings/{locale}.yaml`. Missing keys fall back to English with a build-time warning.
- Content translations contain **only** translatable fields: `title`, `summary`, `explanation`, `oldApproach`, `modernApproach`, `whyModernWins`, `support.description`. Code, slugs, navigation, and docs are never translated.
- `oldCode`/`modernCode` in translation files are **always overwritten** with English values at build time to prevent hallucinated code.
- Locale registry: `html-generators/locales.properties` (format: `locale=Display Name`).
- When adding a new UI string key, add it to `en.yaml` first, then to all other locale files. The generator warns on missing keys but doesn't fail.
- YAML colons in string values must be quoted (Jackson parser is stricter than PyYAML).

## Proof Files

Each pattern has a corresponding proof file: `proof/{category}/{PascalCaseSlug}.java`.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+

/// Proof: slug-name
/// Source: content/category/slug-name.yaml
void main() {
    // modern code only — no old code, no assertions
}
```

Uses Java 25 implicit classes (`void main()`, not `static void main`). Add minimal scaffolding (imports, dummy variables) to make the modern code compile.

## Key Conventions

- **Vanilla JS only** — `site/app.js` uses no frameworks or build tools.
- **Category display names** are defined in `html-generators/categories.properties`, not hardcoded.
- **JDK filter ranges** in `app.js` map LTS versions to ranges: `11→[9-11]`, `17→[12-17]`, `21→[18-21]`, `25→[22-25]`.
- **JetBrains Mono ligatures** are disabled on `.code-text` elements to prevent operators like `->` from rendering as special characters.
- **Dark theme** uses CSS custom properties (`--modern-bg`, `--old-bg`). Theme state is in `localStorage.theme` and `data-theme` on `<html>`.
- **RTL support** — Arabic (`ar`) locale sets `dir="rtl"` on the page.
- When both old and modern approaches are from the same JDK version, use descriptive labels (e.g., "Full syntax" / "Compact") instead of version numbers.
