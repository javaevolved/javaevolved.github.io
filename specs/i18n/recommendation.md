# i18n Architecture: AI-Driven Translation Workflow

The i18n architecture is based on **externalized UI strings and full-replacement content files** (see `plan-b-externalized-strings-full-translations.md`).

---

## Why this approach works well with AI-generated translations

When a new slug is added and AI generates the translations automatically, the
pipeline becomes:

```
New English slug  →  AI prompt  →  Translated JSON file  →  Commit to repo
```

Key properties that make this AI-friendly:

- AI receives the full English JSON and outputs a complete translated JSON —
  no special field-filtering rules in the prompt.
- `oldCode` and `modernCode` are simply copied verbatim from the English file
  at build time, regardless of what the AI wrote — the generator always
  overwrites them. Zero prompt-engineering required to handle this case.
- The output is **self-contained and trivially validatable**: run the same JSON
  schema checks as for English files.
- The fallback mechanism is explicit: if the AI-generated file does not yet
  exist, the generator falls back to English and can display an "untranslated"
  banner — a clear signal rather than a silent gap.
- `translations/strings/{locale}.json` (UI strings) is a simple key-value
  file; AI can translate it in one shot with minimal instructions.

---

## Recommended AI automation workflow

1. **Trigger**: GitHub Actions detects a new `content/<cat>/<slug>.json` commit
   (or a workflow dispatch).
2. **AI step**: For each supported locale, call the translation AI with a
   structured prompt:
   ```
   Translate the following Java pattern JSON from English to {locale}.
   - Keep slug, id, category, difficulty, jdkVersion, oldLabel, modernLabel,
     oldCode, modernCode, docs, related, prev, next, support.state unchanged.
   - Translate: title, summary, explanation, oldApproach, modernApproach,
     whyModernWins[*].title, whyModernWins[*].desc, support.description.
   - Return valid JSON only.
   ```
3. **Validate**: Run the same JSON schema validation used for English files.
4. **Write**: Commit the translated file to
   `translations/content/{locale}/<cat>/<slug>.json`.
5. **Build**: The generator picks it up on the next deployment and removes the
   "untranslated" banner automatically.

> **Note**: Even though the full JSON asks for `oldCode` and `modernCode`, the
> build tooling always overwrites those fields with the English values. So AI
> can safely copy them verbatim — no risk of translated or hallucinated code
> leaking into the site.

---

## Keeping translations in sync

When an English `content/<cat>/<slug>.json` file is **modified**, the same
automation can regenerate the translated file, or flag the diff for human
review. A simple CI check can compare `jdkVersion`, `id`, and `slug` in the
English file with the translated file to catch stale translations.
