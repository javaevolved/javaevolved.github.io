# HTML Generators

This folder contains the build scripts that generate all HTML detail pages and `site/data/snippets.json` from the JSON source files in `content/`.

## Files

| File | Description |
|------|-------------|
| `generate.java` | JBang script (Java 25) — primary generator |
| `generate.py` | Python equivalent — produces identical output |
| `generate.jar` | Pre-built fat JAR (no JBang/JDK setup needed) |

## Running

### Option 1: Fat JAR (fastest, no setup)

```bash
java -jar html-generators/generate.jar
```

Requires only a Java 25+ runtime — no JBang installation needed.

### Option 2: JBang (for development)

```bash
jbang html-generators/generate.java
```

Requires [JBang](https://jbang.dev) and Java 25+.

### Option 3: Python

```bash
python3 html-generators/generate.py
```

Requires Python 3.8+.

## Rebuilding the fat JAR

After modifying `generate.java`, rebuild the fat JAR:

```bash
jbang export fatjar --output html-generators/generate.jar html-generators/generate.java
```

This produces a self-contained ~2.2 MB JAR with all dependencies (Jackson) bundled. The `build-generator.yml` GitHub Action does this automatically when `generate.java` changes.
