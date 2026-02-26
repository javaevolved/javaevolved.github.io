# Internationalization Specification

## Overview

This document specifies how java.evolved supports multiple languages.
Internationalization is implemented via two distinct layers:

1. **UI strings layer** — every piece of hard-coded copy in the templates
   (labels, button text, nav, footer, etc.) is extracted into a per-locale
   `translations/strings/{locale}.yaml` file and injected at build time.

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
  collections/
  strings/
  streams/
  concurrency/
  io/
  errors/
  datetime/
  security/
  tooling/
  enterprise/

translations/                         # All i18n artifacts
  strings/
    en.yaml                           # English UI strings (extracted from templates)
    pt-BR.yaml                        # Partial — missing keys fall back to en.yaml
    ja.yaml
  content/
    pt-BR/
      language/
        type-inference-with-var.json  # Full translated JSON (all fields)
      collections/
      strings/
      streams/
      concurrency/
      io/
      errors/
      datetime/
      security/
      tooling/
      enterprise/
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
en=🇬🇧 English
de=🇩🇪 Deutsch
es=🇪🇸 Español
pt-BR=🇧🇷 Português (Brasil)
zh-CN=🇨🇳 中文 (简体)
ar=🇸🇦 العربية
fr=🇫🇷 Français
ja=🇯🇵 日本語
ko=🇰🇷 한국어
```

The generator reads this file to know which locales to build and what label
to show in the language selector.

---

## `translations/strings/{locale}.yaml` Schema

Every user-visible string in the templates is assigned a dot-separated key.
The English file is the complete reference; locale files are partial and only
need to include keys that differ from English.

```yaml
# translations/strings/en.yaml
site:
  title: java.evolved
  tagline: Java has evolved. Your code can too.
  tagline_line1: Java has evolved.
  tagline_line2: Your code can too.
  description: A collection of modern Java code snippets. Every old Java pattern next
    to its clean, modern replacement — side by side.
  heroSnippetCount: '✦ {{snippetCount}} modern patterns · Java 8 → Java 25'
  heroOld: Old
  heroModern: Modern
  allComparisons: All comparisons
  snippetsBadge: '{{snippetCount}} snippets'
nav:
  allPatterns: ← All patterns
  toggleTheme: Toggle theme
  viewOnGitHub: View on GitHub
  selectLanguage: Select language
breadcrumb:
  home: Home
sections:
  codeComparison: Code Comparison
  whyModernWins: Why the modern way wins
  oldApproach: Old Approach
  modernApproach: Modern Approach
  sinceJdk: Since JDK
  difficulty: Difficulty
  jdkSupport: JDK Support
  howItWorks: How it works
  relatedDocs: Related Documentation
  relatedPatterns: Related patterns
filters:
  show: 'Show:'
  all: All
difficulty:
  beginner: Beginner
  intermediate: Intermediate
  advanced: Advanced
search:
  placeholder: Search snippets…
  noResults: No results found.
  esc: ESC
  searchTrigger: Search…
  navigate: navigate
  open: open
  close: close
cards:
  old: Old
  modern: Modern
  hoverHint: hover to see modern →
  hoverHintRelated: Hover to see modern ➜
  touchHint: 👆 tap or swipe →
copy:
  copy: Copy
  copied: Copied!
share:
  label: Share
view:
  expandAll: Expand All
  collapseAll: Collapse All
stats:
  modernPatterns: Modern Patterns
  jdkVersions: JDK Versions Covered
  categories: Categories
  linesOfPython: Lines of Python Required
footer:
  tagline: Java has evolved. Your code can too.
  madeWith: Made with ❤️ by
  and: and
  inspiredBy: Inspired by
  viewOnGitHub: View on GitHub
copilot:
  headline: Modernize your Java codebase with GitHub Copilot.
  description: Let Copilot help you migrate legacy patterns to modern Java — automatically.
  appModernization: App Modernization →
  javaGuide: Java Guide →
support:
  available: Available
  preview: Preview
  experimental: Experimental
contribute:
  button: Contribute
  codeIssue: Report a code issue
  translationIssue: Report a translation issue
  suggestPattern: Suggest a new pattern
  seeIssue: "See a problem with this code?"
  reportIt: "Let us know."
untranslated:
  notice: This page has not yet been translated into {{localeName}}.
  viewInEnglish: View in English
```

```yaml
# translations/strings/pt-BR.yaml  (partial — only translated keys required)
site:
  tagline: O Java evoluiu. Seu código também pode.
  description: Uma coleção de snippets modernos de Java...
nav:
  allPatterns: ← Todos os padrões
  toggleTheme: Alternar tema
sections:
  codeComparison: Comparação de código
  whyModernWins: Por que a forma moderna ganha
  howItWorks: Como funciona
  relatedDocs: Documentação relacionada
  relatedPatterns: Padrões relacionados
```

**Key-level fallback rule:** if a key present in `en.yaml` is absent from a
locale file, the generator uses the English value and emits a build-time warning:

```
[WARN] pt-BR.yaml: missing key "footer.madeWith" — using English fallback
```

The page is always rendered completely; no key is ever silently blank. The warning
is purely informational and does **not** abort the build.

---

## Content Translation Files

Translated content files are **partial** — they contain **only** the
translatable fields. The generator overlays them onto the English base at build
time. This prevents translators from diverging structural data (code,
navigation, metadata) from the English source of truth.

### Field Translation Reference

Every field in a slug definition file falls into one of three categories:

| Category | Fields | Rule |
|---|---|---|
| **Translate** (include in translation file) | `title`, `summary`, `explanation`, `oldApproach`, `modernApproach`, `whyModernWins` (full array), `support.description` | These are the **only** fields present in a translation file |
| **English source of truth** (never in translation file) | `id`, `slug`, `category`, `difficulty`, `jdkVersion`, `oldLabel`, `modernLabel`, `oldCode`, `modernCode`, `prev`, `next`, `related`, `docs` | Always taken from the English content file; any values in the translation file are ignored |
| **Translated via UI strings** | `difficulty`, `support.state` | Enum values stay in English; display names resolved from `translations/strings/{locale}.yaml` at build time |

**Why enum fields use UI strings instead of content translation:**

Fields like `difficulty` (`beginner`, `intermediate`, `advanced`) and
`support.state` (`available`, `preview`, `experimental`) are enum values used
programmatically as CSS classes, filter keys, and data attributes. Their
**display names** are resolved at build time from the UI strings layer:

- `difficulty` → `difficulty.beginner`, `difficulty.intermediate`, `difficulty.advanced`
- `support.state` → `support.available`, `support.preview`, `support.experimental`
- `category` → display name from `categories.properties`

This separation ensures enum values remain stable across locales while display
text is centrally managed and consistently translated.

**Other non-translated fields:**

- `whyModernWins[*].icon` — emoji; typically kept as-is across locales
- `docs[*].title` — kept in English since the linked documentation is English

### Example Translated File

Translation files contain **only** translatable fields — no structural data:

```json
// translations/content/pt-BR/language/type-inference-with-var.json
{
  "title": "Inferência de tipo com var",
  "oldApproach": "Tipos explícitos",
  "modernApproach": "Palavra-chave var",
  "summary": "Use var para deixar o compilador inferir o tipo local.",
  "explanation": "...",
  "whyModernWins": [
    { "icon": "⚡", "title": "Menos ruído", "desc": "..." },
    { "icon": "👁",  "title": "Mais legível", "desc": "..." },
    { "icon": "🔒", "title": "Seguro", "desc": "..." }
  ],
  "support": {
    "description": "Amplamente disponível desde o JDK 10 (março de 2018)"
  }
}
```

---

## Generator — Resolution Order

For each pattern and locale the generator:

1. Loads the English baseline from `content/<cat>/<slug>.json` (or `.yaml`/`.yml`).
2. Checks whether `translations/content/<locale>/<cat>/<slug>.{json,yaml,yml}` exists.
   - **Yes** → deep-copies the English node, then overlays only the translatable
     fields (`title`, `summary`, `explanation`, `oldApproach`, `modernApproach`,
     `whyModernWins`, `support.description`) from the translation file.
   - **No** → use the English file and inject an "untranslated" banner
     (see next section).
3. Loads `translations/strings/<locale>.yaml` deep-merged over `en.yaml`.
   Any key present in `en.yaml` but absent from the locale file falls back to
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
name mirrors the dot-separated key path in `strings/{locale}.yaml`:

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

A globe icon button (🌐) is placed in the nav bar immediately next to the
dark/light theme toggle button. Clicking it opens a dropdown list of available
locales. The list is rendered at build time from `locales.properties`.

```html
<!-- inside .nav-right, adjacent to the existing theme-toggle button -->
<div class="locale-picker" id="localePicker">
  <button type="button" class="locale-toggle" aria-haspopup="listbox" aria-expanded="false"
          aria-label="Select language">🌐</button>
  <ul role="listbox" aria-label="Language">
    <li role="option" data-locale="en"    aria-selected="true">English</li>
    <li role="option" data-locale="pt-BR" aria-selected="false">Português (Brasil)</li>
  </ul>
</div>
```

When the user selects a locale, `app.js` rewrites the current URL path to the
equivalent page for that locale and persists the choice to `localStorage` so
subsequent page loads open in the correct language automatically.

The dropdown is hidden by default and toggled via the `.locale-toggle` button.
Clicking outside the dropdown or pressing `Escape` closes it. The currently
active locale's `<li>` receives `aria-selected="true"` and a visual highlight.

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
   Translate the following Java pattern from English to {locale}.
   Return a JSON file containing ONLY these translated fields:
   - title, summary, explanation, oldApproach, modernApproach
   - whyModernWins (full array with icon, title, desc)
   - support.description (inside a "support" object)
   Do NOT include: slug, id, category, difficulty, jdkVersion, oldLabel,
   modernLabel, oldCode, modernCode, docs, related, prev, next, support.state.
   Return valid JSON only.
   ```
   See the **Field Translation Reference** table above for the full rationale.
3. **Validate** — Verify the output contains only translatable fields.
4. **Commit** — Write the output to
   `translations/content/{locale}/<cat>/<slug>.json` and commit.
5. **Deploy** — The generator picks it up on next build; the "untranslated"
   banner disappears automatically.

### Keeping translations in sync

When an English file is **modified**, the same automation regenerates the
translated file or opens a PR flagging the diff for human review. Since
translation files only contain translatable text, structural changes to the
English file (code, navigation, metadata) take effect immediately without
needing to update the translation.

---

## Practical Guidelines for Translation Files

### YAML quoting

Translation text frequently contains colons (`:`) — especially in Romance
languages with explanatory phrases like "Nota: el resultado es inmutable" or
"Ejemplo: seed es el inicio". **Unquoted colons break YAML parsing.**

Always ensure values containing colons are properly quoted:

```yaml
# ✗ BROKEN — YAML interprets "Nota:" as a mapping key
explanation: Stream.toList() devuelve una lista. Nota: el resultado es inmutable.

# ✓ CORRECT — value is quoted
explanation: "Stream.toList() devuelve una lista. Nota: el resultado es inmutable."
```

When generating translation files programmatically, use a YAML library's
`dump()` function (which handles quoting automatically) rather than writing
raw YAML strings by hand.

### Jackson YAML parser compatibility

Jackson's YAML parser (`jackson-dataformat-yaml`) is stricter than Python's
`PyYAML`. Certain serialization styles that Python accepts can crash Jackson:

- **`---` document markers** combined with backslash line continuations (`\`)
  can trigger `ArrayIndexOutOfBoundsException` in Jackson's `UTF8Reader`.
- Always test translation files with `jbang html-generators/generate.java`
  before committing — Python validation alone is not sufficient.

If a file parses in Python but fails in Jackson, re-serialize it with
`yaml.dump(..., default_flow_style=False)` to produce a compatible style.

### Validation before commit

Always validate translation files before committing:

```bash
# Build all locales — will fail fast on any broken file
jbang html-generators/generate.java

# Or build a single locale for faster iteration
jbang html-generators/generate.java --locale es
```

### Format support

Both content translations and UI strings files can be in JSON or YAML format.
The generator discovers files by trying `.json`, `.yaml`, and `.yml` extensions
in order via `findWithExtensions()`. Within a single locale, formats can be
mixed freely (e.g., strings in YAML, some content in JSON).

---

## Migration Path

| Phase | Work |
|---|---|
| 1 | Extract every hard-coded string from templates into `translations/strings/en.yaml`; replace literals with `{{…}}` tokens; verify English output is unchanged |
| 2 | Add `locales.properties`; extend generator to load strings, support `--locale`, and fall back gracefully |
| 3 | Add language selector to nav; implement `app.js` locale detection and path rewrite |
| 4 | Translate `strings/pt-BR.yaml` and 2–3 content files as a proof-of-concept; verify fallback banner |
| 5 | Update GitHub Actions; add `hreflang` alternate links |
| 6 | Wire up AI translation automation; add `translations/` schema documentation |
