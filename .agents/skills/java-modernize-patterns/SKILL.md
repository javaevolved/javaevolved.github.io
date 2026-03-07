# Skill: Java Modernization Patterns

Refactor Java code by replacing legacy idioms with modern equivalents from the
[java.evolved](https://javaevolved.github.io) pattern library.

## Usage

1. **Identify the target JDK version** of the project (`java.version` in `pom.xml`,
   `build.gradle`, `.java-version`, or CI configuration).
2. **Look up the pattern** in `references/pattern-map.yaml` using the legacy idiom.
3. **Gate on `min_jdk`** — only apply a pattern when `project_jdk >= min_jdk`.
4. **Check `safety`** before applying:
   - `safe` — apply automatically; the transformation is purely mechanical with no
     behavioural change.
   - `review` — apply the transformation, then flag the diff for human review; the
     pattern may change semantics, mutability, or require broader API updates.
5. Apply the transformation **one pattern at a time** in a single commit so the
   change is easy to review and revert.

## Safety levels

| Level | Meaning |
|-------|---------|
| `safe` | Mechanical substitution; no observable behaviour change. Apply freely. |
| `review` | Likely beneficial but may change semantics (e.g. mutability, thread model, API surface). Always leave a comment or PR note explaining the change. |

## Refactoring rules

### Language

- Replace `ExplicitType var = expr` local variables with `var` where the type is
  obvious from the RHS (`min_jdk: 10`, `safe`).
- Replace triple-quoted string concatenation with text blocks (`min_jdk: 15`, `safe`).
- Replace `if (x instanceof Foo) { Foo f = (Foo) x; }` with
  `if (x instanceof Foo f)` pattern variables (`min_jdk: 16`, `safe`).
- Replace verbose POJOs (constructor + getters + equals/hashCode/toString) with
  `record` when the class is a pure data carrier (`min_jdk: 16`, **`review`** —
  records are immutable; existing mutation callers must be updated).
- Replace `switch` statements that return a value with switch expressions using
  arrow labels (`min_jdk: 14`, `safe`).
- Replace `if-else instanceof` chains over a class hierarchy with `switch` type
  patterns (`min_jdk: 21`, `safe`).
- Replace open class hierarchies intended to be closed with `sealed … permits`
  (`min_jdk: 17`, **`review`** — breaks external subclassing).

### Collections

- Replace `Arrays.asList(…)` / `Collections.unmodifiableList(new ArrayList<>(…))`
  with `List.of(…)` for immutable lists (`min_jdk: 9`, **`review`** — `List.of()`
  rejects `null` elements; verify no nulls are present).
- Replace `new ArrayList<>(existingList)` + `Collections.unmodifiableList` with
  `List.copyOf(existingList)` (`min_jdk: 10`, **`review`** — rejects nulls).
- Replace `collect(Collectors.toList())` with `.toList()` (`min_jdk: 16`,
  **`review`** — `.toList()` returns an unmodifiable list; callers that mutate the
  result will break).

### Strings

- Replace `str.trim()` with `str.strip()` for Unicode-aware whitespace handling
  (`min_jdk: 11`, `safe`).
- Replace `str == null || str.trim().isEmpty()` with `str == null || str.isBlank()`
  (`min_jdk: 11`, `safe`).
- Replace `str.split("\\n")` with `str.lines()` (`min_jdk: 11`, `safe`).
- Replace `String.format(template, …)` with `template.formatted(…)` (`min_jdk: 15`,
  `safe`).
- Replace `StringBuilder` loops that just repeat a string with `str.repeat(n)`
  (`min_jdk: 11`, `safe`).

### Streams / Optional

- Replace `Predicate<T> not = t -> !pred.test(t)` with `Predicate.not(pred)`
  (`min_jdk: 11`, `safe`).
- Replace `if (opt.isPresent()) { … } else { … }` with
  `opt.ifPresentOrElse(…, …)` (`min_jdk: 9`, `safe`).
- Replace `opt.orElseThrow(() -> new NoSuchElementException(…))` with
  `opt.orElseThrow()` (`min_jdk: 10`, `safe`).
- Replace `value != null ? Stream.of(value) : Stream.empty()` with
  `Stream.ofNullable(value)` (`min_jdk: 9`, `safe`).

### Errors / Null handling

- Replace `try { … } catch (A e) { … } catch (B e) { … }` (identical bodies) with
  multi-catch `catch (A | B e)` (`min_jdk: 7`, `safe`).
- Replace `x != null ? x : defaultValue` with
  `Objects.requireNonNullElse(x, defaultValue)` (`min_jdk: 9`, `safe`).
- Replace nested null-check chains with an `Optional` pipeline (`min_jdk: 9`,
  **`review`** — Optional is not serialisable; avoid as a field type).

### I/O

- Replace `Paths.get(…)` with `Path.of(…)` (`min_jdk: 11`, `safe`).
- Replace `Files.readAllLines` + join or `BufferedReader` idioms for whole-file reads
  with `Files.readString(path)` (`min_jdk: 11`, `safe`).
- Replace `FileWriter` + `BufferedWriter` idioms for whole-file writes with
  `Files.writeString(path, content)` (`min_jdk: 11`, `safe`).
- Replace `HttpURLConnection` with the built-in `HttpClient` (`min_jdk: 11`,
  **`review`** — async API requires caller restructuring).
- Replace `InputStream.read()` copy loops with `in.transferTo(out)` (`min_jdk: 9`,
  `safe`).

### Concurrency

- Replace `executor.shutdown(); executor.awaitTermination(…)` boilerplate with
  try-with-resources on `ExecutorService` (`min_jdk: 19`, `safe`).
- Replace `Thread.sleep(millis)` with `Thread.sleep(Duration.of…)` for
  self-documenting waits (`min_jdk: 19`, `safe`).
- Replace platform thread-per-task patterns with virtual threads
  (`min_jdk: 21`, **`review`** — verify that `ThreadLocal` usage is thread-safe
  under virtual threads, and that no native code or pinning-sensitive locks are
  involved).
- Replace `ThreadLocal` used for cross-call context propagation with `ScopedValue`
  (`min_jdk: 25`, **`review`** — `ScopedValue` is immutable per scope; callers that
  mutate `ThreadLocal` values must be redesigned).

### Datetime

- Replace `new Date()`, `Calendar`, `SimpleDateFormat` with `java.time.*` types
  (`min_jdk: 8`, **`review`** — verify timezone handling and serialisation format
  compatibility).
- Replace `new SimpleDateFormat(pattern)` with
  `DateTimeFormatter.ofPattern(pattern)` (`min_jdk: 8`, `safe`).
- Replace manual `Math.max(min, Math.min(value, max))` clamps with
  `Math.clamp(value, min, max)` (`min_jdk: 21`, `safe`).

### Security

- Replace `new Random()` with `RandomGenerator.of("Xoshiro256PlusPlus")` or
  another named algorithm (`min_jdk: 17`, `safe`).
- Replace `new SecureRandom()` with `SecureRandom.getInstanceStrong()` where a
  strong generator is required (`min_jdk: 9`, `safe`).

## Anti-patterns to avoid

- **Do not** apply `List.of()` / `Map.of()` / `Set.of()` if the original code
  contains or may contain `null` elements.
- **Do not** replace `Collectors.toList()` with `.toList()` if the returned list
  is later mutated (e.g. `list.add(…)`, `list.remove(…)`).
- **Do not** convert a class to a `record` if it has mutable fields, non-trivial
  inheritance, or is used as a JPA entity.
- **Do not** replace `ThreadLocal` with `ScopedValue` without tracing all
  mutation sites.
- **Do not** apply patterns above their `min_jdk` — always verify the project JDK
  version first.

## Reference

Full pattern catalogue with code examples:
`references/pattern-map.yaml` (machine-readable) and
<https://javaevolved.github.io> (human-readable).
