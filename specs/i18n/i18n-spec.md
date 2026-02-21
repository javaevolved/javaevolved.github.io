# Internationalization Specification

## Overview

This document specifies how java.evolved supports multiple languages.
Internationalization is implemented via two distinct layers:

1. **UI strings layer** — every piece of hard-coded copy in the templates
   (labels, button text, nav, footer, etc.) is extracted into a per-locale
   `translations/strings/{locale}.json` file and injected at build time.

2. **Content translation layer** — translated pattern JSON files are complete,
   stand-alone replacements stored under `translations/content/{locale}/`.
   The generator falls back to the English file for any pattern that has not yet
   been translated.

English is a first-class locale. All locales — including English — go through
the same build pipeline.

---

## Directory Layout

```
content/                              # English content (source of truth)
  language/
    type-inference-with-var.json
  collections/
    ...

translations/                         # All i18n artefacts
  strings/
    en.json                           # English UI strings (extracted from templates)
    pt-BR.json                        # Partial — missing keys fall back to en.json
    ja.json
  content/
    pt-BR/
      language/
        type-inference-with-var.json  # Full translated JSON (all fields)
      collections/
        ...
    ja/
      language/
        ...

templates/                            # Templates use {{…}} tokens for every UI string
  slug-template.html
  index.html
  ...

html-generators/
  locales.properties                  # Ordered list of supported locales + display names
  generate.java                        # Extended to iterate all locales

site/                                 # Generated output
  index.html                          # English home (path = /)
  language/
    type-inference-with-var.html
  data/
    snippets.json
  pt-BR/
    index.html
    language/
      type-inference-with-var.html
    data/
      snippets.json
  ja/
    ...
```

---

## `locales.properties` — Supported Locales Registry

```properties
# html-generators/locales.properties
# format: locale=Display name  (first entry is the default/primary locale)
en=English
pt-BR=Português (Brasil)
ja=日本語
```

The generator reads this file to know which locales to build and what label
to show in the language selector.

---

## `translations/strings/{locale}.json` Schema

Every user-visible string in the templates is assigned a dot-separated key.
The English file is the complete reference; locale files are partial and only
need to include keys that differ from English.

```json
// translations/strings/en.json
{
  "site": {
    "title": "java.evolved",
    "tagline": "Java has evolved. Your code can too.",
    "description": "A collection of modern Java code snippets. Every old Java pattern next to its clean, modern replacement — side by side."
  },
  "nav": {
    "allPatterns": "← All patterns",
    "toggleTheme": "Toggle theme",
    "viewOnGitHub": "View on GitHub"
  },
  "sections": {
    "codeComparison": "Code Comparison",
    "whyModernWins": "Why the modern way wins",
    "oldApproach": "Old Approach",
    "modernApproach": "Modern Approach",
    "sinceJdk": "Since JDK",
    "difficulty": "Difficulty",
    "jdkSupport": "JDK Support",
    "howItWorks": "How it works",
    "relatedDocs": "Related Documentation",
    "relatedPatterns": "Related patterns"
  },
  "filters": {
    "show": "Show:",
    "all": "All",
    "difficulty": {
      "beginner": "Beginner",
      "intermediate": "Intermediate",
      "advanced": "Advanced"
    }
  },
  "search": {
    "placeholder": "Search patterns…",
    "noResults": "No results found.",
    "esc": "ESC"
  },
  "copy": {
    "copy": "Copy",
    "copied": "Copied!"
  },
  "footer": {
    "madeWith": "Made with ❤️ by",
    "inspiredBy": "Inspired by",
    "viewOnGitHub": "View on GitHub"
  },
  "support": {
    "available": "Available",
    "preview": "Preview",
    "experimental": "Experimental"
  }
}
```

```json
// translations/strings/pt-BR.json  (partial — only translated keys required)
{
  "site": {
    "tagline": "O Java evoluiu. Seu código também pode.",
    "description": "Uma coleção de snippets modernos de Java..."
  },
  "nav": {
    "allPatterns": "← Todos os padrões",
    "toggleTheme": "Alternar tema"
  },
  "sections": {
    "codeComparison": "Comparação de código",
    "whyModernWins": "Por que a forma moderna ganha",
    "howItWorks": "Como funciona",
    "relatedDocs": "Documentação relacionada",
    "relatedPatterns": "Padrões relacionados"
  }
}
```

**Key-level fallback rule:** if a key present in `en.json` is absent from a
locale file, the generator uses the English value and emits a build-time warning:

```
[WARN] strings/pt-BR.json: missing key "footer.builtWith" — using English fallback
```

The page is always rendered completely; no key is ever silently blank. The warning
is purely informational and does **not** abort the build.

---

## Content Translation Files

Translated content files are **complete** copies of the English pattern JSON
with translatable fields rendered in the target language. This avoids
partial-merge edge cases and makes each file self-contained.

```json
// translations/content/pt-BR/language/type-inference-with-var.json
{
  "id": 1,
  "slug": "type-inference-with-var",
  "title": "Inferência de tipo com var",
  "category": "language",
  "difficulty": "beginner",
  "jdkVersion": "10",
  "oldLabel": "Java 8",
  "modernLabel": "Java 10+",
  "oldApproach": "Tipos explícitos",
  "modernApproach": "Palavra-chave var",
  "oldCode": "...",
  "modernCode": "...",
  "summary": "Use var para deixar o compilador inferir o tipo local.",
  "explanation": "...",
  "whyModernWins": [
    { "icon": "⚡", "title": "Menos ruído", "desc": "..." },
    { "icon": "👁",  "title": "Mais legível", "desc": "..." },
    { "icon": "🔒", "title": "Seguro", "desc": "..." }
  ],
  "support": {
    "state": "available",
    "description": "Amplamente disponível desde o JDK 10 (março de 2018)"
  },
  "prev": "language/...",
  "next": "language/...",
  "related": ["..."],
  "docs": [{ "title": "...", "href": "..." }]
}
```

`oldCode` and `modernCode` are **always overwritten** with the English values at
build time, regardless of what appears in the translation file. Translators may
leave those fields empty or copy the English values verbatim — neither causes
any harm.

---

## Generator — Resolution Order

For each pattern and locale the generator:

1. Loads the English baseline from `content/<cat>/<slug>.json`.
2. Checks whether `translations/content/<locale>/<cat>/<slug>.json` exists.
   - **Yes** → use the translated file, then overwrite `oldCode`/`modernCode`
     with the English values.
   - **No** → use the English file and inject an "untranslated" banner
     (see next section).
3. Loads `translations/strings/<locale>.json` deep-merged over `en.json`.
   Any key present in `en.json` but absent from the locale file falls back to
   the English value; the generator logs a `[WARN]` for each missing key and
   continues without aborting.
4. Renders the template, substituting content tokens (`{{title}}`, …) and
   UI-string tokens (`{{nav.allPatterns}}`, …).
5. Writes output to `site/<locale>/<cat>/<slug>.html`
   (or `site/<cat>/<slug>.html` for English).

### Untranslated Pattern Banner

When falling back to English content for a non-English locale, the generator
injects:

```html
<div class="untranslated-notice" lang="en">
  This page has not yet been translated into Português (Brasil).
  <a href="/language/type-inference-with-var.html">View in English</a>
</div>
```

The banner is suppressed when the locale is `en` or a translation file exists.

---

## Template Changes

Every hard-coded English string in the templates is replaced with a token whose
name mirrors the dot-separated key path in `strings/{locale}.json`:

| Before | After |
|---|---|
| `Code Comparison` | `{{sections.codeComparison}}` |
| `Why the modern way wins` | `{{sections.whyModernWins}}` |
| `How it works` | `{{sections.howItWorks}}` |
| `← All patterns` | `{{nav.allPatterns}}` |
| `Copy` | `{{copy.copy}}` |
| `Copied!` | `{{copy.copied}}` |
| `Search patterns…` | `{{search.placeholder}}` |

The `<html>` opening tag becomes `<html lang="{{locale}}">`.

---

## HTML `<head>` Changes

`hreflang` alternate links are generated for every supported locale:

```html
<link rel="alternate" hreflang="en"        href="https://javaevolved.github.io/language/type-inference-with-var.html">
<link rel="alternate" hreflang="pt-BR"     href="https://javaevolved.github.io/pt-BR/language/type-inference-with-var.html">
<link rel="alternate" hreflang="x-default" href="https://javaevolved.github.io/language/type-inference-with-var.html">
```

---

## Navigation — Language Selector

A `<select>` dropdown in the nav bar lets users switch locales.
The list of locales is rendered at build time from `locales.properties`:

```html
<select id="localePicker" aria-label="Select language">
  <option value="en">English</option>
  <option value="pt-BR" selected>Português (Brasil)</option>
</select>
```

`app.js` rewrites the current URL path to the equivalent page in the selected
locale when the user changes the selection.

---

## `app.js` Changes

The search index path and locale picker must both be locale-aware:

```js
// Detect current locale from path prefix
const locale = location.pathname.startsWith('/pt-BR/') ? 'pt-BR'
             : location.pathname.startsWith('/ja/')    ? 'ja'
             : 'en';

// Load the correct snippets index
const indexPath = locale === 'en'
  ? '/data/snippets.json'
  : `/${locale}/data/snippets.json`;
```

Localised strings consumed by JavaScript are embedded as a `<script>` block by
the generator so `app.js` doesn't need to fetch them separately:

```html
<script>
  window.i18n = {
    searchPlaceholder: "Buscar padrões...",
    noResults:         "Nenhum resultado encontrado.",
    copied:            "Copiado!"
  };
</script>
```

`app.js` reads from `window.i18n` instead of hard-coded literals.

---

## GitHub Actions Changes

The deploy workflow iterates all entries in `locales.properties`:

```yaml
- name: Build site
  run: jbang html-generators/generate.java --all-locales
```

Or explicitly, to support incremental locale addition:

```yaml
- name: Build site
  run: |
    jbang html-generators/generate.java
    jbang html-generators/generate.java --locale pt-BR
    jbang html-generators/generate.java --locale ja
```

---

## AI-Driven Translation Workflow

When a new slug is added, AI generates translations automatically:

```
New English slug  →  AI prompt  →  Translated JSON file  →  Schema validation  →  Commit
```

### Why this architecture suits AI translation

- The AI receives the full English JSON and returns a complete translated JSON —
  no special field-filtering rules in the prompt.
- `oldCode`/`modernCode` are overwritten by the build tooling, so AI can copy
  them verbatim without risk of hallucinated code shipping to users.
- The translated file passes the same JSON schema validation as English files —
  no separate validation logic needed.
- If the AI file does not exist yet, the fallback is an explicit "untranslated"
  banner rather than a silent gap.

### Automation steps

1. **Trigger** — GitHub Actions detects a new or modified
   `content/<cat>/<slug>.json` (push event or workflow dispatch).
2. **Translate** — For each supported locale, call the translation model with:
   ```
   Translate the following Java pattern JSON from English to {locale}.
   - Keep unchanged: slug, id, category, difficulty, jdkVersion, oldLabel,
     modernLabel, oldCode, modernCode, docs, related, prev, next, support.state
   - Translate: title, summary, explanation, oldApproach, modernApproach,
     whyModernWins[*].title, whyModernWins[*].desc, support.description
   - Return valid JSON only.
   ```
3. **Validate** — Run JSON schema validation (same rules as English content).
4. **Commit** — Write the output to
   `translations/content/{locale}/<cat>/<slug>.json` and commit.
5. **Deploy** — The generator picks it up on next build; the "untranslated"
   banner disappears automatically.

### Keeping translations in sync

When an English file is **modified**, the same automation regenerates the
translated file or opens a PR flagging the diff for human review. A CI check
can compare `id`, `slug`, and `jdkVersion` between the English and translated
files to detect stale translations.

---

## Migration Path

| Phase | Work |
|---|---|
| 1 | Extract every hard-coded string from templates into `translations/strings/en.json`; replace literals with `{{…}}` tokens; verify English output is unchanged |
| 2 | Add `locales.properties`; extend generator to load strings, support `--locale`, and fall back gracefully |
| 3 | Add language selector to nav; implement `app.js` locale detection and path rewrite |
| 4 | Translate `strings/pt-BR.json` and 2–3 content files as a proof-of-concept; verify fallback banner |
| 5 | Update GitHub Actions; add `hreflang` alternate links |
| 6 | Wire up AI translation automation; add `translations/` schema documentation |
