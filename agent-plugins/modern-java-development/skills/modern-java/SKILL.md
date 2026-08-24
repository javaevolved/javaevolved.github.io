---
name: modern-java
description: Detects a project's effective Java version and provides release-appropriate guidance for writing, reviewing, refactoring, upgrading, and modernizing Java code. Use for Java implementation, architecture, code review, build configuration, migration, performance, concurrency, testing, or API design tasks.
license: MIT
compatibility: Requires Python 3 to run the bundled detector; an agent may inspect the same project files directly when Python is unavailable.
metadata:
  author: java.evolved
  version: "1.0.0"
---

# Modern Java

Determine the project's effective compilation target before proposing code.
An installed JDK is only fallback evidence; it does not prove which language
features or APIs the build accepts.

## Workflow

1. From the project or module root, run:

   ```bash
   python3 <skill-directory>/scripts/detect_java_version.py .
   ```

   If the user explicitly supplied Java version X, pass
   `--java-version X`. Explicit user intent takes precedence over project files.

2. Read the JSON result:
   - Use `selected.version` as the target.
   - Treat `maven-release`, `gradle-release`, and explicit overrides as stronger
     evidence than toolchains, source compatibility, version-manager files, CI,
     environment variables, or the installed runtime.
   - If `ambiguous` is true, inspect the listed files and determine which module
     or build profile is in scope. Do not silently choose the newest version.
   - If no version is selected, inspect build and CI configuration. If still
     unknown, state the assumption before writing version-sensitive code.

3. Read [core practices](references/core-practices.md), then read the target
   release and all earlier applicable groups in
   [release practices](references/release-practices.md). Recommendations are
   cumulative: Java 21 code may use final features from Java 21 and below.

4. Inspect whether preview is explicitly enabled (`--enable-preview` in both
   compile and runtime/test configuration). Do not recommend preview features
   by default. Never use a feature finalized after the detected target.

5. Tailor the work:
   - **New code:** prefer the clearest final API available at the target.
   - **Review:** flag needless legacy patterns only when the replacement is
     available at the target and fits the behavior.
   - **Modernization:** separate behavior-preserving refactors from Java target
     upgrades. Do not raise the target unless requested.
   - **Libraries:** respect the published minimum Java version, not the
     maintainer's local JDK.
   - **Multi-release or multi-module builds:** evaluate each affected source set
     or module against its own target.

6. Validate with the project's existing build using its configured toolchain.
   Compile and run tests with the same `--release` and preview settings used by
   production. Do not treat a successful compile on a newer local JDK as proof
   of compatibility.

## Recommendation format

When version matters, briefly name the basis:

> Detected Java 17 from `maven.compiler.release`; recommendations are limited to
> final Java 17 APIs and language features.

For upgrades, distinguish:

- **Usable now:** supported by the current target.
- **Available after upgrade:** requires a specific newer target.
- **Preview:** experimental for a specific release and opt-in only.

Prefer small, behavior-preserving changes. Do not mechanically replace every
older construct: readability, API contracts, allocation behavior, framework
constraints, and team conventions still apply.
