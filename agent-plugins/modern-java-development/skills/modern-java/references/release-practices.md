# Practices by final Java release

Use the group matching the detected target plus all earlier groups. The entries
name capabilities that became final in that release unless explicitly labeled
preview. Preview history is otherwise excluded. API availability still depends
on the exact runtime and module graph.

## Java 7 and older maintenance targets

- Use generics instead of raw collections, `Deque` instead of legacy `Stack`,
  and unsynchronized collections unless synchronization is part of the contract.
  <!-- covers: raw-collections-to-generics stack-to-deque legacy-synchronized-collections -->
- Use `ProcessBuilder` instead of `Runtime.exec`, construct URLs through `URI`,
  and specify charsets explicitly at text/byte boundaries.
  <!-- covers: runtime-exec-to-process-builder url-constructors-to-uri explicit-charset-file-io -->
- Use multi-catch when handlers have identical behavior and neither alternative
  needs a more specific type. Preserve separate catches when recovery differs.
  <!-- covers: multi-catch -->
- This guide does not recommend new development on pre-Java 8 targets. Preserve
  compatibility, apply general engineering practices, and plan a supported-JDK
  migration separately from behavior-preserving refactoring.

## Java 8 baseline

- Use lambdas and method references when they clarify behavior, and streams for
  side-effect-free transformations and reductions. Use
  `stream.toArray(Type[]::new)` when a typed array is the required API boundary.
  <!-- covers: anonymous-classes-to-lambdas stream-toarray-typed -->
- Prefer collection bulk operations, `Map.compute`/`merge`, comparator factories,
  and the standard Base64 codecs over hand-written equivalents when their
  contracts match the required behavior.
  <!-- covers: collection-bulk-operations map-compute-and-merge comparator-factories standard-base64 -->
- Use `java.time`, `DateTimeFormatter`, `Duration`, and `Period` instead of
  mutable `Date`, `Calendar`, `SimpleDateFormat`, or unitless millisecond math.
  <!-- covers: java-time-basics date-formatting duration-and-period -->
- Use `CompletableFuture` for genuinely asynchronous composition, with an
  explicit executor and failure strategy; do not block each stage with `get`.
  <!-- covers: completablefuture-chaining -->
- Use default methods to evolve interfaces compatibly and static interface
  methods for operations conceptually owned by that API. Avoid turning an
  interface into an unrelated utility namespace.
  <!-- covers: default-interface-methods static-methods-in-interfaces -->

## Java 9

- Prefer `List.of`, `Set.of`, `Map.of`, and `Map.entry` for small immutable
  collections. They reject nulls, and duplicate set elements/map keys fail.
  <!-- covers: immutable-list-creation immutable-set-creation immutable-map-creation map-entry-factory -->
- Use `takeWhile`/`dropWhile` only with understood encounter-order semantics;
  use `Stream.ofNullable`, three-argument `iterate`, `Collectors.flatMapping`,
  `Optional.or`, and `ifPresentOrElse` when they directly express intent.
  <!-- covers: stream-takewhile-dropwhile stream-of-nullable stream-iterate-predicate collectors-flatmapping optional-or optional-ifpresentorelse optional-chaining -->
- Use `Objects.requireNonNullElse` for a non-null eager default; use the supplier
  variant when fallback construction is expensive.
  <!-- covers: require-nonnull-else -->
- Use private interface methods to share implementation among defaults, and the
  diamond operator with anonymous classes when inference remains obvious.
  <!-- covers: private-interface-methods diamond-operator -->
- Use `InputStream.transferTo` for whole-stream copying and effectively-final
  resources in try-with-resources when ownership and close timing are clear.
  <!-- covers: inputstream-transferto try-with-resources-effectively-final -->
- Use `ProcessBuilder` to start processes and `ProcessHandle` to inspect or
  manage them. Drain output, bound waits, and handle process-tree termination.
  <!-- covers: process-api runtime-exec-to-process-builder -->
- Use `StackWalker` for controlled stack inspection, `Cleaner` or explicit
  resource ownership instead of finalization, and
  `getDeclaredConstructor().newInstance()` instead of `Class.newInstance()`.
  <!-- covers: stack-walker finalizers-to-resource-cleanup class-newinstance-to-constructor -->
- Preserve nanosecond `Instant` precision through storage and serialization;
  do not silently truncate to epoch milliseconds.
  <!-- covers: instant-precision -->
- Avoid native Java serialization for untrusted data. Where legacy
  deserialization remains, install narrow `ObjectInputFilter` limits.
  <!-- covers: deserialization-filters -->
- Use `SecureRandom.getInstanceStrong()` only when its potentially blocking
  provider behavior is acceptable; ordinary `SecureRandom` is appropriate for
  most security-sensitive generation.
  <!-- covers: strong-random -->
- Use `String.chars()` for UTF-16 code units and `codePoints()` for Unicode code
  points; neither directly iterates grapheme clusters.
  <!-- covers: string-chars-stream -->
- Use JShell for exploration and JFR for low-overhead production diagnostics.
  Record representative workloads and protect sensitive event data.
  <!-- covers: jshell-prototyping jfr-profiling -->
- Use modules only with an explicit encapsulation or distribution goal; do not
  modularize mechanically.

## Java 10

- Use local `var` when the initializer makes the type obvious and the name
  preserves intent. Avoid it when it hides an important numeric, generic, or
  domain type.
  <!-- covers: type-inference-with-var -->
- Use `List.copyOf`, `Set.copyOf`, and `Map.copyOf` for unmodifiable snapshots,
  understanding that an already unmodifiable input may be reused.
  <!-- covers: copying-collections-immutably -->
- Use no-argument `Optional.orElseThrow()` for required values rather than
  `get()`, unless a domain-specific exception provides better context.
  <!-- covers: optional-orelsethrow -->

## Java 11

- Prefer `java.net.http.HttpClient` for JDK-native HTTP. Reuse clients, configure
  connect/request timeouts, handle interruption, and validate status and body
  limits.
  <!-- covers: http-client http-websocket-client -->
- Use `String.isBlank`, `strip`, `lines`, and `repeat` instead of hand-written
  equivalents. `strip` is Unicode-aware; `lines` recognizes multiple line
  terminators and does not retain them.
  <!-- covers: string-isblank string-strip string-lines string-repeat -->
- Use `Files.readString`, `writeString`, and `Path.of` for appropriately sized
  content with explicit charset/options where defaults are not the contract.
  Do not load unbounded files wholly into memory.
  <!-- covers: reading-files writing-files path-of -->
- Use `Predicate.not` where it reads better than lambda negation.
  <!-- covers: predicate-not -->
- Run small single-file source programs directly when a build is needless;
  retain a normal build for dependencies, repeatable testing, and packaging.
  <!-- covers: single-file-execution -->
- Rely on TLS 1.3 defaults where peers permit, while retaining hostname
  verification, certificate validation, and explicit protocol policy when
  interoperability requires it.
  <!-- covers: tls-default -->

## Java 12-13

- Use `Files.mismatch` for a mismatch position without hand-written byte loops.
  <!-- covers: files-mismatch -->
- Use `Collectors.teeing` for two independent reductions over one stream when it
  is clearer than an explicit accumulator.
  <!-- covers: collectors-teeing -->
- Use `String.indent` and `transform` for explicit text pipelines, accounting
  for line terminator normalization by `indent`.
  <!-- covers: string-indent-transform -->
- Do not use switch expressions on these targets unless preview is intentionally
  enabled; they become final in Java 14.

## Java 14

- Use switch expressions with `->` and `yield` for value-producing decisions.
  Keep side-effect-heavy control flow as statements.
  <!-- covers: switch-expressions -->
- Use helpful NullPointerException diagnostics for diagnosis, but still validate
  public inputs with domain-specific messages and do not parse VM messages.
  <!-- covers: helpful-npe -->

## Java 15

- Use text blocks for multiline SQL, JSON, HTML, and fixtures while reviewing
  incidental indentation, trailing whitespace, and escaping.
  <!-- covers: text-blocks-for-multiline-strings -->
- Use `String.formatted` when receiver-oriented formatting is clearer than
  `String.format`; specify locale explicitly for locale-sensitive output.
  <!-- covers: string-formatted -->

## Java 16

- Use records for transparent, shallowly immutable data carriers and compact
  constructors for validation/normalization. Defensively copy mutable
  components and consider serialization/framework contracts.
  <!-- covers: records-for-data-classes compact-canonical-constructor record-based-errors -->
- Use pattern matching for `instanceof` to remove redundant casts while keeping
  variable scope narrow.
  <!-- covers: pattern-matching-instanceof -->
- Use `Stream.toList()` for an unmodifiable result; do not assume a concrete
  implementation, null policy equivalent to `List.of`, or mutability equivalent
  to `Collectors.toList`.
  <!-- covers: stream-tolist unmodifiable-collectors -->
- Use `mapMulti` when one input emits zero or more outputs without temporary
  streams and the callback remains readable.
  <!-- covers: stream-mapmulti -->
- Inner classes may declare static members. Use them only when the member belongs
  to that nested type; do not use the relaxation to hide unrelated globals.
  <!-- covers: static-members-in-inner-classes -->

## Java 17

- Use sealed classes/interfaces for intentionally closed hierarchies, especially
  when exhaustive handling is valuable. Account for modules/packages, proxies,
  persistence frameworks, and downstream extension points.
  <!-- covers: sealed-classes -->
- Use `HexFormat` instead of custom hexadecimal conversion and specify delimiter,
  prefix, suffix, and case as part of the format contract.
  <!-- covers: hex-format -->
- Use the `RandomGenerator` hierarchy when algorithm selection or modern
  pseudo-random generators matter; continue using `SecureRandom` for security.
  <!-- covers: random-generator -->
- Java 17 is the minimum for JUnit 6. Adopt it only after checking engine,
  extension, IDE, and build-tool compatibility; configure JSpecify-aware
  nullness analysis rather than treating annotations as runtime validation.
  <!-- covers: junit6-with-jspecify -->
- Treat strong encapsulation of JDK internals as a migration requirement, not
  something to bypass permanently with `--add-opens`.
- Plan migrations away from the deprecated Security Manager around explicit
  process, container, module, and application security boundaries.
  <!-- covers: security-manager-migration -->

## Java 18-20

- Use `jwebserver` for local static development and diagnostics, not as a
  production application server.
  <!-- covers: built-in-http-server -->
- Use `Thread.sleep(Duration)` to make units explicit while preserving
  interruption and avoiding sleeps as coordination.
  <!-- covers: thread-sleep-duration -->
- Use try-with-resources for locally owned `ExecutorService` lifetimes. Define
  cancellation, graceful shutdown, timeout, and forced-shutdown behavior.
  <!-- covers: executor-try-with-resources -->
- Use `Locale.of` instead of deprecated locale constructors, while preserving
  language, region, variant, and BCP 47 semantics.
  <!-- covers: locale-of -->
- Record patterns, pattern switch, and virtual threads are not final before Java
  21.

## Java 21

- Use virtual threads for high-throughput workloads dominated by blocking I/O.
  Do not pool them; audit pinning, `ThreadLocal` usage, native calls, rate limits,
  observability, and downstream capacity. Prefer one task per virtual thread and
  bound the scarce external resource rather than the thread count.
  <!-- covers: virtual-threads virtual-thread-executor concurrent-http-virtual -->
- Use record patterns and pattern matching for switch for concise data-oriented
  logic. Use guards for case-specific predicates, `case null` only when null is
  part of the input contract, and sealed exhaustiveness instead of a masking
  `default` when all permitted cases should be handled.
  <!-- covers: record-patterns pattern-matching-switch guarded-patterns null-in-switch exhaustive-switch -->
- Use sequenced collection operations such as `getFirst`, `getLast`, and
  `reversed` instead of index arithmetic or copied reversals. Remember that
  reversed views are backed by the original collection.
  <!-- covers: sequenced-collections reverse-list-iteration -->
- Use `Math.clamp` for bounded numeric values, preserving its argument-order,
  NaN, signed-zero, and invalid-range semantics.
  <!-- covers: math-clamp -->
- Structured concurrency and scoped values are preview APIs in Java 21; do not
  present them as final for this target.

## Java 22

- Use unnamed variables and patterns (`_`) when a binding is intentionally
  unused, not when a meaningful name would document the code.
  <!-- covers: unnamed-variables -->
- Use the Foreign Function & Memory API for supported native interop instead of
  introducing new JNI where it fits. Make arena lifetime, thread confinement,
  ABI/platform assumptions, restricted methods, and native error handling
  explicit.
  <!-- covers: call-c-from-java -->
- Use `FileChannel.map` with arenas for explicit mapped-memory lifetime. Account
  for file mutation, address-space pressure, platform unmapping behavior, and
  access after arena closure.
  <!-- covers: file-memory-mapping -->
- Use the multi-file source launcher for small source-only programs whose
  dependencies fit its source-discovery model; use a normal build for production
  packaging and complex dependency management.
  <!-- covers: multi-file-source -->
- Stream gatherers and module import declarations are not final in Java 22.

## Java 23

- Use Markdown documentation comments when they improve prose-heavy API docs.
  Check generated output, links, code fences, and doclint rather than assuming
  Markdown renders as intended.
  <!-- covers: markdown-javadoc-comments -->
- Module import declarations, compact source files, and primitive patterns are
  preview features on this target.

## Java 24

- Use the Class-File API for class-file parsing, generation, and transformation
  when its typed model fits; preserve unknown attributes and verify emitted
  bytecode when interoperability matters.
  <!-- covers: class-file-api -->
- Use stream gatherers for reusable stateful intermediate operations when
  standard operations cannot express the transformation clearly. Respect
  integrator state, short-circuiting, parallel-combiner, and finisher semantics.
  <!-- covers: stream-gatherers -->
- Structured concurrency, scoped values, compact source files, stable values,
  and primitive patterns are not final defaults on this target.

## Java 25

- Use scoped values for bounded, immutable context propagation, especially with
  virtual threads. Bind values over the smallest dynamic scope and do not use
  them as mutable globals.
  <!-- covers: scoped-values -->
- Use compact source files and instance `main` methods for scripts, teaching,
  and small programs, with `java.lang.IO` for concise console interaction.
  Retain conventional classes and explicit streams/readers where frameworks,
  launchers, public APIs, redirection, charset control, or production error
  handling require them.
  <!-- covers: compact-source-files io-class-console-io -->
- Use module import declarations sparingly where broad imports improve small
  source files without obscuring symbol ownership or creating ambiguity.
  <!-- covers: module-import-declarations -->
- Use flexible constructor bodies for argument validation and preparation before
  constructor invocation, while preserving the prohibition on observing the
  under-construction instance.
  <!-- covers: flexible-constructor-bodies -->
- Use the final KDF API rather than hand-rolled key derivation. Keep algorithm,
  salt, work factor, output length, key lifecycle, and protocol interoperability
  explicit.
  <!-- covers: key-derivation-functions -->
- Evaluate compact object headers and AOT cache/training ergonomics with
  measurements on the deployed workload. Verify GC, serviceability, training
  representativeness, cache reproducibility, class path, and deployment-image
  compatibility.
  <!-- covers: compact-object-headers aot-class-preloading -->
- **Preview:** Stable Values can replace correct-but-complex lazy initialization
  when preview is deliberately enabled. Verify initialization, exception,
  recursion, and contention semantics; do not mechanically replace every holder
  or memoization pattern.
  <!-- covers: stable-values lock-free-lazy-init -->
- **Preview:** Structured concurrency can make related subtasks a single
  cancellation and failure-propagation unit. Define deadlines, failure policy,
  result ownership, and interruption behavior.
  <!-- covers: structured-concurrency -->
- **Preview:** Primitive types in patterns can combine matching with exact/range
  checks. Review numeric conversion, overflow, exhaustiveness, and release-specific
  syntax.
  <!-- covers: primitive-types-in-patterns -->
- **Preview:** Use the PEM API only with preview enabled. Validate object type,
  encryption, algorithm/provider support, malformed input, secret handling, and
  interoperability; do not conflate it with the final KDF API.
  <!-- covers: pem-encoding -->

## Later or unknown targets

Do not assume features beyond Java 25 from this guide. Verify final status
against the OpenJDK JEP index and target-release API documentation. Clearly
label preview or incubating APIs and confirm the exact release syntax.
