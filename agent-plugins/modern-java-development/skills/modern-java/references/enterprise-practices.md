# Enterprise Java and framework practices

Apply this guide only when the corresponding framework is present. The Java
compilation target, Jakarta EE platform level, MicroProfile version, Spring
generation, provider capabilities, and database support are independent
compatibility axes. Detect each from build dependencies and deployment
configuration before recommending a migration.

## Migration boundaries

- Preserve transaction boundaries, security constraints, retries, delivery
  guarantees, persistence behavior, observability, and container lifecycle.
  Replacing annotations is not proof of equivalent behavior.
- Treat `javax.*` to `jakarta.*` as an ecosystem migration. Verify the runtime,
  dependencies, generated sources, descriptors, tests, and integrations move
  together; do not mix the namespaces in one application.
- Prefer constructor injection for required dependencies when the framework
  supports it. Use field or resource injection only where the container contract
  requires it.
- Confirm native-image/AOT, reflection, proxying, serialization, and build-time
  indexing constraints before changing frameworks or component models.

## Jakarta component model

- Prefer CDI scopes and `@Inject` for ordinary application services over EJB
  session beans, and use `@Transactional` when its interceptor semantics match.
  Keep EJB where remote interfaces, passivation, asynchronous methods, timers,
  pooling, or other EJB services are required.
  <!-- covers: ejb-vs-cdi singleton-ejb-vs-cdi-application-scoped -->
- Replace JNDI service-locator code with typed injection for managed resources,
  but retain explicit lookup at genuine dynamic naming boundaries.
  <!-- covers: jndi-lookup-vs-cdi-injection -->
- Prefer CDI `@Named` beans to legacy JSF managed beans. Verify scopes, proxy
  requirements, serialization, and view lifetime during migration.
  <!-- covers: jsf-managed-bean-vs-cdi-named -->
- Prefer declarative `@Transactional` boundaries at service methods over manual
  transaction choreography. Keep explicit transactions when one method truly
  needs multiple independently controlled units of work, and account for
  self-invocation and rollback rules.
  <!-- covers: manual-transaction-vs-declarative -->
- Use Jakarta Concurrency managed executors rather than application-created
  threads in a Jakarta runtime. Do not replace persistent, calendar-based EJB
  timers with an in-memory fixed-rate schedule unless restart, clustering,
  misfire, timezone, and exactly-once requirements permit it.
  <!-- covers: ejb-timer-vs-jakarta-scheduler -->

## HTTP APIs and messaging

- Prefer Jakarta REST resources for resource-oriented HTTP APIs over low-level
  servlet dispatch code. Keep filters, streaming, protocol upgrades, and other
  servlet-level behavior where those APIs are the right abstraction.
  <!-- covers: servlet-vs-jaxrs -->
- Prefer REST/JSON over SOAP only when contracts, WS-Security, reliable
  messaging, transactions, schema-first tooling, and client compatibility do
  not require the WS-* stack. Modernize the contract, not merely the transport.
  <!-- covers: soap-vs-jakarta-rest -->
- Consider MicroProfile Reactive Messaging for channel-based event processing,
  typed payloads, and backpressure. Before replacing an MDB, match JMS
  acknowledgement, ordering, redelivery, dead-letter, transaction, selector,
  and concurrency semantics and verify the chosen connector.
  <!-- covers: mdb-vs-reactive-messaging -->

## Persistence choices

- Use JPA for aggregate-oriented persistence and managed entity lifecycles; use
  JDBC when exact SQL, low overhead, bulk operations, or database-specific
  control is more important. Account for lazy loading, fetching, locking,
  batching, caching, and transaction scope rather than assuming ORM is simpler.
  <!-- covers: jdbc-vs-jpa -->
- Use the JPA Criteria API for genuinely dynamic entity queries. It is not fully
  type-safe when attributes are referenced by strings; use the static metamodel
  or another typed query approach when compile-time attribute checking matters.
  <!-- covers: jdbc-resultset-vs-jpa-criteria -->
- Consider jOOQ when SQL is the primary abstraction and generated schema types,
  CTEs, window functions, vendor features, and composable queries matter.
  Verify edition/database support, code generation, dialect behavior, and
  transaction integration. Continue binding all untrusted values.
  <!-- covers: jdbc-vs-jooq -->
- Prefer Jakarta Data repositories for conventional CRUD and derived queries
  when the Jakarta EE 11 runtime and provider implement the required behavior.
  Retain `EntityManager`, criteria, or SQL for complex persistence operations,
  and verify query derivation, pagination, locking, and transaction semantics.
  <!-- covers: jpa-vs-jakarta-data -->

## Spring

- Prefer annotation-based configuration, constructor injection, and focused
  explicit `@Configuration` over large XML bean graphs in modern Spring.
  Preserve XML where externalized wiring or legacy integration makes it useful;
  do not rely on broad component scanning that obscures ownership.
  <!-- covers: spring-xml-config-vs-annotations -->
- On Spring Framework 7, use native API version conditions when they match the
  public versioning strategy. Keep version negotiation centralized, document
  deprecation and compatibility policy, and avoid merging unrelated versions
  into a controller merely to reduce class count.
  <!-- covers: spring-api-versioning -->
- On Spring Framework 7, migrate Spring nullness annotations to JSpecify
  deliberately. Establish `@NullMarked` boundaries, annotate type uses
  accurately, configure a checker, and treat diagnostics as migration work
  rather than suppressing them.
  <!-- covers: spring-null-safety-jspecify -->
