# Contributing

Contributions are welcome! Content is managed as YAML files — never edit generated HTML.

## Adding a new pattern

1. Fork the repo
2. Create a new YAML file in the appropriate `content/<category>/` folder (e.g. `content/language/my-feature.yaml`)
3. Copy [`content/template.json`](content/template.json) as a starting point for all required fields (see the [snippet schema](.github/copilot-instructions.md) for details)
4. Update the `prev`/`next` fields in adjacent pattern files to maintain navigation
5. Run `jbang html-generators/generate.java` to verify your changes build correctly
6. Optionally add a `proofCode` field — a self-contained JShell snippet that proves the modern approach works. Run `jbang html-generators/proof.java` to validate it.
7. Open a pull request

Please ensure JDK version labels only reference the version where a feature became **final** (non-preview).

## Translating the site

The site supports multiple languages. See [`specs/i18n/i18n-spec.md`](specs/i18n/i18n-spec.md) for the full specification.

### Adding a new locale

1. Add the locale to `html-generators/locales.properties` (e.g. `ja=日本語`)
2. Create `translations/strings/<locale>.yaml` with all UI strings translated (copy `translations/strings/en.yaml` as a starting point)
3. Create content translation files under `translations/content/<locale>/<category>/<slug>.yaml`
4. Run `jbang html-generators/generate.java` and verify the build succeeds
5. Open a pull request

### Translating content files

Translation files contain **only** translatable fields — the generator merges them onto the English base at build time. This prevents translated files from diverging structurally from the English source of truth.

A translation file should contain exactly these fields:

```yaml
title: "Inferencia de tipos con var"
oldApproach: "Tipos explícitos"
modernApproach: "Palabra clave var"
summary: "Usa var para inferencia de tipos..."
explanation: "Desde Java 10, el compilador infiere..."
whyModernWins:
  - icon: "⚡"
    title: "Menos código repetitivo"
    desc: "No es necesario repetir tipos genéricos..."
  - icon: "👁"
    title: "Mejor legibilidad"
    desc: "..."
  - icon: "🔒"
    title: "Igualmente seguro"
    desc: "..."
support:
  description: "Ampliamente disponible desde JDK 10 (marzo 2018)"
```

Do **not** include `id`, `slug`, `category`, `difficulty`, `jdkVersion`, `oldCode`, `modernCode`, `prev`, `next`, `related`, or `docs` — these are always taken from the English source.

**Important:** If your text contains colons (`:`), ensure the value is properly quoted in YAML to avoid parse errors. Always validate with `jbang html-generators/generate.java` before submitting.
