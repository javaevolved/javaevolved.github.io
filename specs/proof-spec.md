# Proof Files Specification

## Overview

The `proof/` directory contains one `.java` file per pattern slug. Each file
wraps the pattern's **modern code** in a compilable program, proving that every
code snippet shown on the site actually compiles with the advertised JDK version.

Only the **modern code** is proven — the old code is what we're moving away from.

---

## Directory Layout

```
proof/
  language/
    TypeInferenceWithVar.java
    RecordsForDataClasses.java
    SealedClasses.java
    ...
  collections/
    ImmutableListCreation.java
    ...
  strings/
  streams/
  concurrency/
  io/
  errors/
  datetime/
  security/
  tooling/
  enterprise/
```

The folder structure mirrors `content/` — one subfolder per category.

---

## File Conventions

### Naming

- **Folder**: matches the category name (e.g., `language/`, `collections/`)
- **File**: PascalCase version of the slug (e.g., `type-inference-with-var` → `TypeInferenceWithVar.java`)

### Structure

Each proof file follows this structure:

```java
import java.util.*;  // whatever imports the snippet needs

/// Proof: {slug}
/// Source: content/{category}/{slug}.yaml
void main() {
    // modern code from the pattern, adapted to compile
}
```

Key rules:

1. **Use implicit class and `void main()`** — Java 25 supports running single-file
   programs without an explicit class declaration
2. **Add necessary imports** — the snippet JSON/YAML doesn't include imports;
   add whatever is needed for compilation
3. **Add minimal scaffolding** — if the snippet references variables or types not
   defined in the code, add stub declarations so it compiles
4. **Keep it minimal** — the goal is compilation proof, not a full test suite
5. **Include the `/// Proof:` and `/// Source:` comments** — links the proof file
   back to the content source

### What to avoid

- Don't add runtime assertions or test logic
- Don't restructure the modern code — keep it as close to the snippet as possible
- Don't add the old code

---

## Running Proof Files

### Compile a single file

```bash
java --enable-preview proof/language/TypeInferenceWithVar.java
```

### Compile all proof files

```bash
# Compile every proof file and report failures
find proof -name '*.java' -exec sh -c '
  echo "Compiling: $1"
  java --enable-preview "$1" 2>&1 || echo "FAILED: $1"
' _ {} \;
```

### Prerequisites

- **Java 25+** — proof files use implicit classes and `void main()` which
  require `--enable-preview`

---

## Adding a Proof File

When adding a new pattern:

1. Create the content file under `content/category/slug.yaml`
2. Create a proof file under `proof/category/SlugName.java`
3. Copy the `modernCode` from the pattern into the `void main()` body
4. Add imports and minimal scaffolding to make it compile
5. Run `java --enable-preview proof/category/SlugName.java` to verify
