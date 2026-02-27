# HTML Generators

This folder contains the build scripts that generate all HTML detail pages and `site/data/snippets.json` from the JSON source files in `content/`.

## Files

| File            | Description                                   |
|-----------------|-----------------------------------------------|
| `generate.java` | JBang script (Java 25) — primary generator    |
| `generate.py`   | Python equivalent — produces identical output |
| `generate.jar`  | Pre-built fat JAR (no JBang/JDK setup needed) |
| `build-cds.sh`  | Script to build a platform-specific AOT cache |
| `proof.java`    | JBang script (Java 25) — runs all `proof/**/*.java` scripts via JBang |

## Benchmark

See [benchmark/README.md](benchmark/README.md) for performance comparisons across all four execution methods (AOT, Fat JAR, JBang, Python).

## Running

### Option 1: Fat JAR (fastest, no setup)

```bash
java -jar html-generators/generate.jar
```

Requires only a Java 25+ runtime — no JBang installation needed.

### Option 2: Fat JAR with AOT cache (fastest possible)

```bash
# One-time: build the AOT cache (~21 MB, platform-specific)
./html-generators/build-cds.sh

# Subsequent runs use the cache
java -XX:AOTCache=html-generators/generate.aot -jar html-generators/generate.jar
```

The AOT cache (Java 25, JEP 514/515) pre-loads classes from a training run, reducing startup time by ~30%. The cache is platform-specific and is not committed to git — regenerate it after changing the JAR or JDK version.

### Option 3: JBang (for development)

```bash
jbang html-generators/generate.java
```

Requires [JBang](https://jbang.dev) and Java 25+.

### Option 4: Python

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

## CI/CD Workflows

Three GitHub Actions workflows automate the build, deploy, and proof pipeline:

1. **`build-generator.yml`** — Triggered when `generate.java` changes on `main`. Uses JBang to rebuild the fat JAR and commits the updated `generate.jar` back to the repository.

2. **`deploy.yml`** — Triggered when content, templates, the JAR, or site assets change on `main`. Runs `java -jar html-generators/generate.jar` to regenerate all HTML pages, `snippets.json`, and `index.html`, then deploys the `site/` folder to GitHub Pages.

3. **`proof.yml`** — Triggered when `proof/` scripts or content changes on `main` or in pull requests. Runs `jbang html-generators/proof.java` to verify all proof scripts compile and execute correctly.

This means the deploy workflow always uses the pre-built fat JAR (no JBang required at deploy time), and the JAR stays in sync with the source automatically.
