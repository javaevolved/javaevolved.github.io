# java.evolved

**Java has evolved. Your code can too.**

A collection of 85 side-by-side code comparisons showing old Java patterns next to their clean, modern replacements — from Java 8 all the way to Java 25.

🔗 **[javaevolved.github.io](https://javaevolved.github.io)**

---

## What is this?

Every snippet shows two panels:

- **✕ Old** — the traditional way (Java 7/8 era)
- **✓ Modern** — the clean, idiomatic replacement (Java 9–25)

Each comparison includes an explanation of *why* the modern approach is better, which JDK version introduced it, and links to related patterns.

## Categories

| Category | Examples |
|---|---|
| **Language** | Records, sealed classes, pattern matching, switch expressions, var, unnamed variables |
| **Collections** | Immutable factories, sequenced collections, unmodifiable collectors |
| **Strings** | Text blocks, `isBlank()`, `strip()`, `repeat()`, `formatted()`, `indent()` |
| **Streams** | `toList()`, `mapMulti()`, `takeWhile()`/`dropWhile()`, gatherers |
| **Concurrency** | Virtual threads, structured concurrency, scoped values, `ExecutorService` as `AutoCloseable` |
| **I/O** | `Files.readString()`, `writeString()`, `Path.of()`, `transferTo()`, HTTP Client |
| **Errors** | `requireNonNullElse()`, record-based errors, deserialization filters |
| **Date/Time** | `java.time` basics, `Duration`/`Period`, `DateTimeFormatter`, instant precision |
| **Security** | TLS defaults, `SecureRandom`, PEM encoding, key derivation functions |
| **Tooling** | JShell, single-file execution, JFR profiling, compact source files, AOT |

## Architecture

This site uses a **JSON-first** build pipeline:

- **Source of truth**: Individual `category/slug.json` files (85 across 10 category folders)
- **Template**: `slug-template.html` — shared HTML template with `{{placeholder}}` tokens
- **Generator**: `Generate.java` — a [JBang](https://jbang.dev) script that produces all HTML detail pages and `data/snippets.json`
- **Deploy**: GitHub Actions runs the generator and deploys to GitHub Pages

Generated files (`category/*.html` and `data/snippets.json`) are in `.gitignore` — never edit them directly.

## Build & run locally

### Prerequisites

- **Java 25+** (e.g. [Temurin](https://adoptium.net/))
- **[JBang](https://jbang.dev)** (`brew install jbang` / `sdk install jbang` / [other options](https://www.jbang.dev/download/))

### Generate and serve

```bash
# Generate all HTML pages and data/snippets.json
jbang Generate.java

# Serve locally
python3 -m http.server 8090
# Open http://localhost:8090
```

The generator is a compact source file (Java 25) that uses Jackson for JSON parsing. On first run, JBang resolves the Jackson dependency (~2s); subsequent runs use a cached jar (~0.8s).

A Python equivalent (`generate.py`) is also available and produces identical output:

```bash
python3 generate.py
```

## Contributing

Contributions are welcome! Content is managed as JSON files — never edit generated HTML.

1. Fork the repo
2. Create or edit a JSON file in the appropriate category folder (e.g. `language/my-feature.json`)
3. Follow the [snippet JSON schema](.github/copilot-instructions.md) for all required fields
4. Run `jbang Generate.java` to verify your changes build correctly
5. Update `index.html` with a new preview card if adding a new snippet
6. Open a pull request

Please ensure JDK version labels only reference the version where a feature became **final** (non-preview).

## Tech stack

- Plain HTML, CSS, and JavaScript — no frontend frameworks
- [JBang](https://jbang.dev) + [Jackson](https://github.com/FasterXML/jackson) for build-time generation
- Hosted on GitHub Pages via GitHub Actions

## Author

**Bruno Borges**

- GitHub: [@brunoborges](https://github.com/brunoborges)
- X/Twitter: [@brunoborges](https://x.com/brunoborges)
- LinkedIn: [brunocborges](https://www.linkedin.com/in/brunocborges)

## License

This project is licensed under the [MIT License](LICENSE).
