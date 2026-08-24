# Core modern Java practices

Apply these practices at every supported Java version, using only APIs and
syntax available at the detected compilation target.

## Build and compatibility

- Treat `--release`, Maven `maven.compiler.release`, or Gradle
  `options.release` as the compatibility contract.
- Pin a reproducible toolchain in the build and CI. Do not rely solely on
  `JAVA_HOME` or a developer's installed JDK.
- For libraries, test the minimum supported Java release. Avoid accidentally
  linking to APIs from the JDK used to run the build.
- Keep preview features out of production defaults. If adopted deliberately,
  enable preview consistently for compilation, tests, execution, packaging,
  IDEs, and CI, and plan migration at the next release.
- Prefer supported LTS releases for long-lived production systems unless the
  organization intentionally follows the six-month release cadence.

## Language and API design

- Model invalid states out of the type system where practical. Use small value
  types, enums, and sealed hierarchies when the target supports them.
- Prefer immutable state and explicit ownership. Make defensive copies at
  mutable boundaries; do not expose modifiable internal collections.
- Use `Optional` primarily as a return type for an absent single value, not for
  fields, parameters, collections, or serialization contracts by default.
- Use exceptions for exceptional failure, with actionable messages and
  preserved causes. Do not catch broad exceptions or silently continue.
- Prefer standard JDK APIs over dependencies when they are equally clear and
  capable, but do not rewrite mature library functionality merely to remove a
  dependency.
- Keep public APIs unsurprising. Modern syntax is not a reason to break binary,
  source, serialization, or framework compatibility.

## Collections, streams, and nullness

- Return empty collections instead of `null`. State mutability and encounter
  order in API contracts.
- Use streams for declarative transformations, not for control flow with hidden
  side effects. Prefer a loop when it is clearer or permits early exit.
- Do not parallelize streams without measurement and a suitable workload.
- Establish an explicit nullness policy. Prefer JSpecify annotations when the
  surrounding ecosystem and tooling support them.

## Concurrency

- Prefer structured ownership of tasks and resources. Always define
  cancellation, timeout, interruption, and failure propagation behavior.
- Preserve interruption (`Thread.currentThread().interrupt()`) when an
  `InterruptedException` cannot be propagated.
- Use concurrent collections and high-level synchronization utilities before
  hand-written locking. Document invariants protected by locks.
- Measure before selecting executors, pool sizes, lock-free structures, or
  virtual-thread migration. CPU-bound and I/O-bound workloads need different
  strategies.

## Security and reliability

- Validate untrusted input at boundaries. Avoid native Java serialization for
  untrusted data; apply deserialization filters where legacy use remains.
- Use modern TLS defaults, authenticated encryption, `SecureRandom`, and
  purpose-built password/key derivation APIs. Never invent cryptography.
- Use try-with-resources for every `AutoCloseable` whose lifetime is locally
  owned.
- Add timeouts to network calls and bounded behavior to queues, retries,
  payloads, and caches.

## Performance and observability

- Measure with JFR, JDK tools, and a representative workload before optimizing.
- Use JMH for microbenchmarks; include warmup, forks, and consumed results.
- Prefer simple allocation-friendly code, but do not trade correctness or
  clarity for speculative micro-optimizations.
- Include enough context in logs to diagnose failures without exposing secrets
  or personal data.

## Testing

- Test externally visible behavior and edge cases, not implementation details.
- Use the project's existing test framework and build. Keep tests deterministic;
  control clocks, randomness, concurrency, locale, timezone, and filesystem
  dependencies where relevant.
- Add focused regression tests for bug fixes and compatibility tests for public
  APIs or serialized formats.
