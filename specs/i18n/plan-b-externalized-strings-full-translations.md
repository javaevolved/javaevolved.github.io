# Plan B — Externalized UI Strings + Full Translation Files

## Overview

Separate concerns into two distinct layers:

1. **UI strings layer** — every piece of hard-coded copy in the templates
   (labels, button text, nav, footer, etc.) is moved into a per-locale
   `strings/{locale}.json` file and injected at build time.

2. **Content translation layer** — translated pattern JSON files are
   complete, stand-alone replacements (not overlays) stored next to the
   English originals. The generator falls back to the English file for any
   pattern that has not yet been translated.

This approach treats English as just another locale and unifies the build
pipeline so all locales are first-class citizens.

---

## Directory Layout

```
content/                              # Unchanged English content
  language/
    type-inference-with-var.json
  collections/
    ...

translations/                         # New top-level directory
  strings/
    en.json                           # English UI strings (extracted from templates)
    pt-BR.json
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

templates/                            # Templates gain {{…}} tokens for every
  slug-template.html                  # hard-coded UI string (no literal English left)
  index.html
  ...

html-generators/
  locales.properties                  # Ordered list of supported locales + display names
  generate.java / generate.py         # Rewritten to iterate all locales

site/                                 # Generated output
  index.html                          # English home (locale = en, path = /)
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

## `strings/{locale}.json` Schema

Every user-visible string in the templates is assigned a dot-separated key:

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
// translations/strings/pt-BR.json
// Strings files support partial translation: missing keys fall back to en.json at build time.
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

Missing keys in a locale's strings file fall back to the `en.json` value —
strings files are partial by design and only require the keys that need
translation.

> **Note**: This partial-fallback behaviour applies to `strings/{locale}.json`
> **only**. Translated *content* files (see next section) are always complete
> replacements, not overlays.

---

## Full-Replacement Content Files

Unlike Plan A's overlay approach, translated content files are **complete**
copies of the pattern JSON with every field in the target language. This
avoids partial-merge edge cases and lets translators work with a self-contained
file.

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
  "oldCode": "String nome = \"Alice\";\nString saudacao = \"Olá, \" + nome + \"!\";",
  "modernCode": "var nome = \"Alice\";\nvar saudacao = \"Olá, %s!\".formatted(nome);",
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

`oldCode` and `modernCode` are always copied from the English file at build
time; translators must not provide them (or they are silently ignored). This
keeps code snippets language-neutral.

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

## Generator Changes

### Resolution Order

For each pattern the generator:

1. Loads the English baseline from `content/<cat>/<slug>.json`.
2. Checks if `translations/content/<locale>/<cat>/<slug>.json` exists.
   - If **yes**: use the translated file but override `oldCode`/`modernCode`
     with the English values.
   - If **no**: use the English file and optionally mark the page with a
     banner ("This pattern has not yet been translated").
3. Loads `translations/strings/<locale>.json`, deep-merged over
   `translations/strings/en.json`.
4. Renders the template, substituting both content tokens (`{{title}}`, …)
   and UI-string tokens (`{{nav.allPatterns}}`, …).
5. Writes the output to `site/<locale>/<cat>/<slug>.html`
   (or `site/<cat>/<slug>.html` for `en`).

### Untranslated Pattern Banner (optional)

When falling back to English content for a non-English locale, the generator
can inject a `<div class="untranslated-notice">` banner:

```html
<div class="untranslated-notice" lang="en">
  This page has not yet been translated into Português (Brasil).
  <a href="/language/type-inference-with-var.html">View in English</a>
</div>
```

The banner is suppressed when the locale is `en` or a translation file exists.

---

## Template Changes

Every hard-coded English string in the templates is replaced with a token.
The token naming convention mirrors the key path in `strings/{locale}.json`:

| Before | After |
|---|---|
| `Code Comparison` | `{{sections.codeComparison}}` |
| `Why the modern way wins` | `{{sections.whyModernWins}}` |
| `How it works` | `{{sections.howItWorks}}` |
| `← All patterns` | `{{nav.allPatterns}}` |
| `Copy` | `{{copy.copy}}` |
| `Copied!` | `{{copy.copied}}` |
| `Search patterns…` | `{{search.placeholder}}` |

The `<html>` tag becomes `<html lang="{{locale}}">`.

---

## HTML `<head>` Changes

`hreflang` alternate links are generated for every supported locale:

```html
<link rel="alternate" hreflang="en"       href="https://javaevolved.github.io/language/type-inference-with-var.html">
<link rel="alternate" hreflang="pt-BR"    href="https://javaevolved.github.io/pt-BR/language/type-inference-with-var.html">
<link rel="alternate" hreflang="x-default" href="https://javaevolved.github.io/language/type-inference-with-var.html">
```

---

## Navigation — Language Selector

A `<select>` dropdown in the nav bar lets users switch locales.
The list of locales is rendered at build time from `locales.properties`:

```html
<select id="localePicker" aria-label="Select language">
  <option value="en"    >English</option>
  <option value="pt-BR" selected>Português (Brasil)</option>
</select>
```

`app.js` handles path-rewriting when the user picks a different locale.

---

## `app.js` Changes

The search index path and the locale picker rewrite must both be locale-aware:

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

Localised strings needed by JavaScript (search placeholder, "no results"
message, "Copied!") are embedded as a `<script>` block by the generator:

```html
<script>
  window.i18n = {
    searchPlaceholder: "Buscar padrões...",
    noResults:         "Nenhum resultado encontrado.",
    copied:            "Copiado!"
  };
</script>
```

`app.js` reads from `window.i18n` instead of hard-coded strings.

---

## GitHub Actions Changes

The deploy workflow builds the English site first, then iterates every entry
in `locales.properties` (skipping `en`) to build locale subtrees:

```yaml
- name: Build site
  run: |
    python3 html-generators/generate.py            # English
    python3 html-generators/generate.py --locale pt-BR
    python3 html-generators/generate.py --locale ja
```

Or, with a build-all mode added to the generator:

```yaml
- name: Build site
  run: python3 html-generators/generate.py --all-locales
```

---

## Migration Path

| Phase | Work |
|---|---|
| 1 | Create `translations/strings/en.json` by extracting every hard-coded string from templates; replace literals with `{{…}}` tokens; verify English output is byte-identical |
| 2 | Add `locales.properties`; extend generator to load strings, support `--locale`, fall back gracefully |
| 3 | Add language selector to nav + `app.js` locale detection and path rewrite |
| 4 | Translate `strings/pt-BR.json` and 2–3 content files as a proof-of-concept; verify fallback banner |
| 5 | Update GitHub Actions; add `hreflang` alternate links |
| 6 | Open translation contribution guide; document `translations/` schema |

---

## Trade-offs

| Pros | Cons |
|---|---|
| English is a first-class locale — no special-cased code paths | Larger initial refactor (all template strings must be extracted) |
| Complete translation files are easy for translators to understand | Full content files are larger and harder to keep in sync with English originals |
| Untranslated-page fallback is explicit and user-visible | `translations/` directory adds a new top-level location to learn |
| UI strings fall back to English automatically — no silent gaps | `app.js` must be updated to consume `window.i18n` instead of literals |
| Scales cleanly to many locales | Build time grows linearly with locale count × pattern count |
