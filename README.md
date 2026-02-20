# java.evolved

**Java has evolved. Your code can too.**

A collection of side-by-side code comparisons showing old Java patterns next to their clean, modern replacements — from Java 8 all the way to Java 25.

🔗 **[javaevolved.github.io](https://javaevolved.github.io)**

[![GitHub Pages](https://img.shields.io/badge/GitHub%20Pages-live-brightgreen)](https://javaevolved.github.io)
[![Snippets](https://img.shields.io/badge/snippets-107-blue)](#categories)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-orange)](#contributing)

> **Note:** Update the snippet count badge above when adding new patterns.

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
| **Enterprise** | EJB → CDI, JDBC → JPA/Jakarta Data, JNDI → injection, MDB → reactive messaging, REST |

## Architecture

This site uses a **JSON-first** build pipeline:

- **Source of truth**: Individual `content/category/slug.json` files (107 across 11 category folders)
- **Templates**: `templates/` — shared HTML templates with `{{placeholder}}` tokens
- **Generator**: `html-generators/generate.jar` — pre-built fat JAR that produces all HTML detail pages and `data/snippets.json`
- **Deploy**: GitHub Actions runs the generator and deploys to GitHub Pages

Generated files (`site/category/*.html` and `site/data/snippets.json`) are in `.gitignore` — never edit them directly.

## Build & run locally

### Prerequisites

- **Java 25+** (e.g. [Temurin](https://adoptium.net/))

### Generate and serve

```bash
# Generate all HTML pages and data/snippets.json into site/
jbang html-generators/generate.java

# Serve locally
jwebserver -b 0.0.0.0 -d site -p 8090
# Open http://localhost:8090
```

The fat JAR is a self-contained ~2.2 MB file with all dependencies bundled. No JBang installation needed.

For development on the generator itself, you can use JBang or Python — see [html-generators/README.md](html-generators/README.md) for details.

## Contributing

Contributions are welcome! Content is managed as JSON files — never edit generated HTML.

1. Fork the repo
2. Create a new JSON file in the appropriate `content/<category>/` folder (e.g. `content/language/my-feature.json`)
3. Copy [`content/template.json`](content/template.json) to the new file as a starting point for all required fields (see the [snippet JSON schema](.github/copilot-instructions.md) for details)
4. Update the `prev`/`next` fields in adjacent pattern JSON files to maintain navigation
5. Run `jbang html-generators/generate.java` to verify your changes build correctly
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
