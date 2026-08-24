# Practices by final Java release

Use the group matching the detected target plus all earlier groups. These are
capabilities that became final in that release; preview history is intentionally
excluded. Empty releases are grouped with adjacent releases.

## Java 8 baseline

- Use lambdas and method references when they clarify behavior.
- Use streams for side-effect-free collection transformations and reductions.
- Use `java.time` instead of `Date`, `Calendar`, and `SimpleDateFormat`.
- Use `CompletableFuture` for genuinely asynchronous composition, not as a
  wrapper around blocking code without an executor strategy.
- Use default and static interface methods deliberately to evolve APIs.

## Java 9

- Prefer `List.of`, `Set.of`, `Map.of`, and `Map.entry` for small immutable
  collections; reject nulls intentionally.
- Use `takeWhile`, `dropWhile`, `Stream.ofNullable`, `Optional.or`, and
  `ifPresentOrElse` where they directly express intent.
- Use private interface methods to share implementation among defaults.
- Use `InputStream.transferTo`, effectively-final resources in
  try-with-resources, `ProcessHandle`, and the JDK HTTP/2 client incubator only
  with awareness that the standardized HTTP Client arrives in Java 11.
- Use modules only with an explicit encapsulation or distribution goal; do not
  modularize mechanically.

## Java 10

- Use local `var` when the initializer makes the type obvious and the name
  preserves intent. Avoid it when it hides an important numeric, generic, or
  domain type.
- Use `List.copyOf`, `Set.copyOf`, and `Map.copyOf` for immutable snapshots.
- Use no-argument `Optional.orElseThrow()` for required values.

## Java 11

- Prefer the standardized `java.net.http.HttpClient` for JDK-native HTTP,
  configuring timeouts and handling interruption.
- Use `String.isBlank`, `strip`, `lines`, and `repeat` instead of hand-written
  equivalents.
- Use `Files.readString`, `writeString`, and `Path.of` for appropriately sized
  content.
- Use `Predicate.not` and `Optional.isEmpty` where they improve readability.
- Run small single-file source programs directly when a full build is needless.
- Rely on TLS 1.3 defaults where interoperability permits.

## Java 12-13

- Use `Files.mismatch` to compare file content and `Collectors.teeing` for two
  independent reductions over one stream.
- Use `String.indent` and `transform` for explicit text pipelines.
- Do not use switch expressions yet unless preview is intentionally enabled;
  they become final in Java 14.

## Java 14

- Use switch expressions with `->` and `yield` for value-producing,
  exhaustively handled decisions.
- Use helpful NullPointerException diagnostics during development; still
  validate public inputs with domain-specific messages.

## Java 15

- Use text blocks for multiline SQL, JSON, HTML, and test fixtures while
  reviewing incidental indentation and escaping.
- Use `String.formatted` when receiver-oriented formatting reads more clearly
  than `String.format`.

## Java 16

- Use records for transparent, shallowly immutable data carriers. Defensively
  copy mutable components in a compact canonical constructor.
- Use pattern matching for `instanceof` to remove redundant casts.
- Use `Stream.toList()` when an unmodifiable result is intended; do not assume
  a particular implementation or null-rejection behavior.
- Use `mapMulti` when one input emits zero or more outputs without temporary
  streams.
- Use `toUnmodifiableList`, `Set`, or `Map` when collector semantics are needed.

## Java 17

- Use sealed classes and interfaces for intentionally closed hierarchies,
  especially when exhaustive handling is valuable.
- Use `HexFormat` instead of custom hexadecimal conversion.
- Use the `RandomGenerator` hierarchy when algorithm selection or modern
  pseudo-random generators matter; continue using `SecureRandom` for security.
- Treat strong encapsulation of JDK internals as a migration requirement, not
  something to bypass permanently with `--add-opens`.
- Java 17 is the minimum for JUnit 6; use JSpecify-aware nullness tooling when
  adopting it.

## Java 18-20

- Use the simple web server (`jwebserver`) for local static development, not as
  a production application server.
- Use `Thread.sleep(Duration)` and try-with-resources for `ExecutorService`
  starting in Java 19.
- Do not recommend record patterns, pattern-switch, or virtual threads as final
  features before Java 21.

## Java 21

- Use virtual threads for high-throughput workloads dominated by blocking I/O.
  Do not pool virtual threads, and audit pinning, `ThreadLocal` usage, native
  calls, rate limits, and downstream capacity.
- Use record patterns and final pattern matching for switch for concise,
  exhaustive data-oriented logic.
- Use sequenced collection APIs (`getFirst`, `getLast`, `reversed`) instead of
  index arithmetic or copied reversals.
- Use `Math.clamp` for bounded numeric values.
- Structured concurrency and scoped values are preview APIs in Java 21; do not
  present them as final for this target.

## Java 22

- Use unnamed variables and patterns (`_`) when a binding is intentionally
  unused.
- Use the Foreign Function & Memory API for supported native interop instead of
  introducing new JNI where its constraints fit. Make arena lifetime and thread
  confinement explicit.
- Use the multi-file source launcher for small source-only programs.
- Use `FileChannel.map` with arenas for explicit mapped-memory lifetime.
- Do not use stream gatherers or module import declarations as final features.

## Java 23

- Use Markdown documentation comments when they improve maintainability of
  prose-heavy Javadoc.
- Continue treating module import declarations, compact source files, and
  primitive patterns as preview features.

## Java 24

- Use stream gatherers for reusable stateful intermediate stream operations
  when standard operations cannot express the transformation clearly.
- Consider class-file API and related platform capabilities only when the task
  actually manipulates bytecode; prefer standard APIs over internal ASM copies.
- Structured concurrency, scoped values, and compact source files remain
  preview in Java 24.

## Java 25

- Use scoped values for bounded, immutable context propagation, especially with
  virtual threads; do not use them as mutable globals.
- Use compact source files and instance `main` methods for scripts, teaching,
  and small programs; retain conventional classes where frameworks or public
  APIs expect them.
- Use module import declarations sparingly where broad imports improve small
  source files without obscuring ownership.
- Use flexible constructor bodies while preserving the rule that an object
  cannot be observed before superclass construction.
- Use the PEM encoding API and key derivation function API instead of hand-made
  parsing or cryptographic constructions.
- Evaluate compact object headers and AOT ergonomics with measurements and
  deployment-specific startup, footprint, and compatibility goals.
- Structured concurrency and primitive types in patterns are preview in Java
  25 and require explicit preview configuration. Do not recommend them by
  default.

## Later or unknown targets

Do not assume features beyond Java 25 from this guide. Verify final status
against the OpenJDK JEP index and the target release documentation. Clearly
label any preview or incubating API and confirm the exact release syntax.
