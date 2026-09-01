# hmpps-approved-premises-api — Development Guide

**What this repo is:** the shared Kotlin/Spring Boot backend service for the Community Accommodation (CAS) products

- CAS1 - Approved Premises
- CAS2 - Short-Term Accommodation
- CAS2 HDC - Home Detention Curfew
- CAS3 - Temporary Accommodation

It exposes REST APIs consumed by the CAS user interfaces, backed by
PostgreSQL and emitting domain events to SQS. It is a large codebase built with Gradle on JDK 25.

This guide documents the conventions and workflows actually used in this
codebase: project orientation and common commands, the Controller, Service, Data
(JPA entity / repository) and Transformer layers, request/response models,
database migrations, domain events, feature flags, testing, and the lint/build
gates. Follow these patterns when adding or changing code so new work stays
consistent with what already exists.

The typical request flow is:

`Controller` -> `Service` (business logic + `CasResult`) -> `Repository` (JPA)
-> entities returned to the `Service` -> `Transformer` (entity -> API model) ->
`Controller` returns a `ResponseEntity`.

Service packages are scoped by product: **CAS1** (Approved Premises, `cas1`),
**CAS2** (`cas2`, `cas2hdc`), **CAS3** (Temporary Accommodation,
`cas3`). Shared/legacy code lives in the root `approvedpremisesapi` package.
Prefer the service-scoped types when working in a CAS-specific area.

---

## Quick orientation (read first)

* **Stack:** Kotlin (JDK 25 — see `.sdkmanrc`), Spring Boot, Spring Data JPA,
  PostgreSQL + Flyway, Gradle (`./gradlew`).
* **Package layout (ADR 0029):** `casx` (`cas1`, `cas2`, `cas2hdc`,
  `cas3`) each contain `controller`, `dto`, `service`, `jobs/{migration,seed}`,
  `transformer`, and an entities+repositories package. **Note the entity package
  differs by service:** CAS1 uses `cas1/entity/**`, whereas CAS2/CAS2hdc/CAS3 use
  `<service>/jpa/entity/**` — match the convention already used in the module you
  are editing. Shared/legacy code lives in the root `approvedpremisesapi`
  package (`config`, `common`, `client`, `cmd`, plus legacy `api`, `jpa`,
  `service`, `transformer`). New service-specific code belongs under `casx/**`;
  add sub-packages by domain (reports, applications, assessment, ...).
* **Request flow:** `Controller` -> `Service` (returns `CasResult<T>`) ->
  `Repository` (JPA) -> entity -> `Transformer` (entity -> DTO/API model) ->
  `Controller` returns `ResponseEntity`.
* **Key files:** `common/results/CasResult.kt`, `util/EntityUtils.kt` (result
  unwrapping), `config/OAuth2ResourceServerSecurityConfiguration.kt` (endpoint
  security), `integration/IntegrationTestBase.kt` (integration test base).
* **Reference docs:** architecture decisions in `doc/architecture/decisions/**`
  and task guides in `doc/how-to/**` (e.g. `add_new_endpoint.md`,
  `define_an_api.md`, `best-practice-jpa-entities.md`,
  `add-a-feature-flag.md`, `modifying_domain_event_schemas.md`). Specialised
  sub-agents live in `.github/agents/**`.

### Common commands

```bash
./gradlew ktlintFormat          # auto-fix formatting
./gradlew ktlintCheck detekt    # lint + static analysis (must pass, no new baseline)
./gradlew compileKotlin compileTestKotlin -q   # verify structural/cross-file changes

script/test_database            # start test dependencies (Postgres etc.) — run first
./gradlew test --tests '*Cas1PremisesTest'     # run a focused test locally
./script/test                   # full suite (slow; prefer CI for the whole run)
```

---

## Controller (API) Conventions

### General annotation rules

* `@Operation` annotations should not define an `operationId`. These are provided
  on-the-fly in `OpenApiConfiguration`.
* `@PathVariable` and `@RequestParam` annotations should not include redundant
  values (e.g. where the name of the variable being annotated matches the value
  defined in the annotation).
* `@RequestParam` and `@Parameter` annotations should not include redundant
  `required` entries because this can be inferred from the Kotlin definition (e.g.
  if the type is suffixed with `?`).
* `@Schema` annotations should not include empty descriptions. If a description is
  empty it is redundant and should be removed.
* In the controller function argument list put each argument on a new line.

### Controller declaration

* Prefer the hand-written controller style using the per-service meta-annotation
  — `@Cas1Controller`, `@Cas3Controller`, etc. These are `@Target(CLASS)`
  meta-annotations that combine `@RestController` and
  `@RequestMapping("${api.base-path:}/<service>", produces = [JSON, PROBLEM_JSON])`.
  See `cas1/controller/Cas1Controller.kt`.
* The older style implements a generated OpenAPI `*Delegate` interface (e.g.
  `Cas1ReferenceDataController : ReferenceDataCas1Delegate`). Both coexist; new
  hand-written controllers should use the meta-annotation style.
* Use constructor injection (`private val`) for services and transformers.
* Add a class-level `@Tag(name = "CAS1 Premises")` to group endpoints in Swagger.

### Endpoint methods

* Map with `@GetMapping` / `@PostMapping` / `@PutMapping` / `@PatchMapping` /
  `@DeleteMapping`. Add a concise `@Operation(summary = "...")`; use a `description`
  and `responses = [ApiResponse(...)]` only when it adds real value.
* Keep controllers thin — no business logic. A controller method should:
  authorise, extract the current user if needed, call the service, unwrap the
  result, map via a transformer, and return a `ResponseEntity`.
* Return `ResponseEntity`:
  * `ResponseEntity.ok(body)` for 200.
  * `ResponseEntity.created(uri)` or `ResponseEntity.status(HttpStatus.CREATED).body(...)`
    for 201.
  * `ResponseEntity.noContent().build()` for 204.

### Authorisation

* Endpoint security is enforced in code and covered by `AuthTest` — **not** with
  `@PreAuthorize`/`ResourceSecurityTest` on internal endpoints.
* Enforce permissions with the user-access service, e.g.
  `userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)`
  (CAS1 uses `Cas1UserAccessService`; CAS3 uses `UserAccessService`).
* External controllers (`controller/external/**`) are the exception and do use
  `@PreAuthorize("hasRole(...)")`.
* Every new endpoint must require an appropriate role/permission and have auth
  tests.

### Current user and result handling

* Get the request user with `userService.getUserForRequest()`.
* Services return `CasResult<T>` (see below). Unwrap it in the controller with
  `extractEntityFromCasResult(result)` (from `util/EntityUtils.kt`), which returns
  the success entity or throws the appropriate problem. Use
  `ensureEntityFromCasResultIsSuccess(result)` when you only need to assert success
  without an entity.

### Request/response models (code-first — no OpenAPI generation)

* This project is **code-first**: the OpenAPI/Swagger UI is produced at runtime by
  `springdoc` from the annotated controllers and models. The old generated-from-YAML
  workflow has been removed — there is **no** `src/main/resources/static/*.yml`
  spec and no `openApiGenerate` Gradle task. Ignore any older `doc/how-to`/ADR
  references that describe editing a spec and regenerating.
* Legacy shared API models are hand-written `data class`es under `api/model` (many
  still carry generator-style `@get:JsonProperty(..., required = true)` /
  `@Schema(example = ...)` annotations — keep that style when editing them). A few
  legacy `*Delegate`/`*Api` interfaces under `api/**` also remain and are
  implemented by controllers; new controllers use the meta-annotation style instead.
* For **new** models, follow `doc/how-to/define_an_api.md`:
  * Put them in the relevant `casx/dto` package and suffix with `Dto`
    (`...RequestDto` / `...ResponseDto` for request/response roots).
  * Never return native container types (`List<*>`) directly — wrap them in a
    named response type so the contract can evolve.
  * JSON-serialized enums use the `@get:JsonValue val value: String` +
    `@JsonCreator forValue(...)` companion pattern (see `define_an_api.md`);
    prefer `entries` over the deprecated `values()`. ADR 0025 *aspires* to
    UPPERCASE entry names, but the vast majority of existing enums (incl. recent
    `casx/dto` ones) still use camelCase entries with
    `@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")` — match the
    surrounding code rather than assuming UPPERCASE.

---

## Service Conventions

### Class declaration

* Annotate with `@Service`. Use constructor injection with `private val`
  dependencies. Use `@Lazy` only to break genuine circular dependencies.
* Inject configuration with `@Value("\${...}")` constructor parameters rather than
  scattering `@Value` on fields.
* Inject `java.time.Clock` and derive time from it (`LocalDate.now(clock)`,
  `OffsetDateTime.now(clock)`) — never call `now()` without the clock. This keeps
  services testable.
* Generate identifiers with `UUID.randomUUID()`.

### Transactions

* Use `@Transactional`. Prefer the Jakarta import
  (`jakarta.transaction.Transactional`) for new code; some existing CAS1 services
  use the Spring variant. Apply at class level for broadly transactional services,
  or at method level for finer control.
* For high-contention updates, acquire a pessimistic lock via a dedicated lockable
  repository (e.g. `lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(id)`)
  and/or use `@Version` optimistic locking on the entity.

### Return types: `CasResult<T>`

* Business operations return `CasResult<T>` (`common/results/CasResult.kt`), a
  sealed interface. Construct results with:
  * `CasResult.Success(value)`
  * `CasResult.NotFound(entityType, id)`
  * `CasResult.Unauthorised()`
  * `CasResult.GeneralValidationError(message)`
  * `CasResult.FieldValidationError(validationMessages)`
  * `CasResult.ConflictError(...)`
* Simple lookups that legitimately have no result may return a nullable entity
  (`fun findPremisesById(id: UUID): ApprovedPremisesEntity?`).
* Return domain (JPA) entities wrapped in `CasResult`; do **not** return API
  models from services — mapping to API models is the transformer/controller's
  job. Do not expose entities directly through the API.

### Validation

* Build validated flows with the `validatedCasResult { ... }` DSL
  (`common/results/**`). Record field errors with the
  `"$.fieldName" hasValidationError "errorCode"` infix syntax, then
  `return fieldValidationError` / `return errors()` when any exist, or
  `success(value)` on the happy path.

### Errors vs exceptions

* Prefer returning `CasResult.*` for expected outcomes (not found, unauthorised,
  validation). Reserve thrown problems/exceptions (`InternalServerErrorProblem`,
  `RuntimeException`) for genuinely unrecoverable/internal errors.

### Method naming

* `get*` — retrieve a single entity or computed value (often `CasResult`).
* `find*` — query that may return null / a collection.
* `create*`, `update*`, `delete*` / `cancel*`, `validate*` — as their names imply.

### Collaborators & logging

* Services call repositories directly (`repository.findByIdOrNull(id)`, custom
  `@Query` methods). Services may invoke transformers when they need to assemble a
  structured DTO/summary result.
* Log with SLF4J: `private val log = LoggerFactory.getLogger(this::class.java)`.
* Declare service-specific criteria/result containers as `data class`/`enum class`
  nested in the service (e.g. `Cas1PremisesService.Cas1PremisesInfo`).

---

## Data Layer (JPA Entity & Repository) Conventions

See also `doc/how-to/best-practice-jpa-entities.md`.

### Entities

* Declare entities as `data class` annotated with `@Entity` and
  `@Table(name = "snake_case_table")`. Always suffix the class name with `Entity`.
  Use abstract/regular classes only for JPA inheritance hierarchies
  (`@Inheritance`).
* **Always override `equals`, `hashCode`, and `toString` manually.** Data-class
  defaults cause eager loading and infinite loops across relationships:
  * `equals`/`hashCode`: base on the `id` plus a few scalar (non-collection)
    fields — never include lazy collections.
  * `toString`: minimal, e.g. `"Cas3BookingEntity:$id"`.
* Primary key: `@Id val id: UUID`, assigned by the application (no
  `@GeneratedValue`). Prefer `val` for immutable fields; use `var` only where the
  field is genuinely mutable.
* See CaseEntity for an example

### Relationships

* Default to `fetch = FetchType.LAZY` for `@ManyToOne`, `@OneToMany`, `@OneToOne`,
  `@ManyToMany`. Only make something eager deliberately.
* Eager-load what a specific query needs via `@EntityGraph(attributePaths = [...])`
  or an explicit fetch join in the repository method — not via eager mapping on
  the entity.
* Use `@JoinColumn(name = "...")` on the owning side and `mappedBy` on the inverse
  side. Add `cascade` (e.g. `CascadeType.REMOVE`) only where the child truly
  depends on the parent. `@Fetch(FetchMode.SUBSELECT)` is used to optimise loading
  of multiple lazy collections.

### Columns

* Rely on camelCase -> snake_case mapping; add `@Column(name = ...)` only when the
  names differ.
* Enums: `@Enumerated(EnumType.STRING)`.
* JSON columns: `@Type(JsonType::class)` on a `String` field.
* Custom conversions: `@Convert(converter = XConverter::class)`.
* Dates/times: `LocalDate` for dates, `OffsetDateTime` for timestamps with
  offset, `Instant` for UTC instants.
* Optimistic locking: `@Version var version: Long = 1` on contended entities.

### Repositories

* Declare as an interface extending `JpaRepository<Entity, UUID>`, annotated
  `@Repository`, and co-located in the **same file** as the entity (stack multiple
  repositories, e.g. a `Lockable*Repository`, and projection interfaces in that
  file too).
* Prefer derived query method names
  (`findByX`, `findTopByXOrderByCreatedAtDesc`, `existsByXIgnoreCase`) for simple
  queries; use `@Query` for anything complex.
* In `@Query`, prefer JPQL; use `nativeQuery = true` only when necessary. Use named
  parameters (`:param`). Annotate mutating queries with `@Modifying`.
* Paginate large result sets with `Pageable`, returning `Page<T>` (needs total
  count) or `Slice<T>` (lighter, no count).
* For read-only projections, declare a projection interface (getter methods
  matching column aliases) and return it from the query.
* Use `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a dedicated locking method for
  pessimistic locking.

### Service-scoped vs shared entities

* New, service-specific entities live under `<service>/entity/**` and are
  prefixed (`Cas1*`, `Cas3*`). Shared/legacy entities live in the root
  `jpa.entity/**`. Mark superseded shared types/fields with `@Deprecated` and
  prefer the service-scoped replacement.
* **When you call — or touch code that calls — a `@Deprecated` function, type, or
  field, switch to the replacement it names.** The replacement is given in the
  `ReplaceWith(...)` argument and/or the deprecation message (e.g.
  `@Deprecated("Update calling code to use CasResult", ReplaceWith("extractEntityFromCasResult"))`,
  or `@Deprecated("Use getOffenderByCrn to return a CasResult")`). Follow that
  guidance rather than propagating the deprecated symbol; if no clear replacement
  is indicated, flag it rather than guessing.

---

## Transformer Conventions

* Transformers map **entity/domain -> API DTO/model** (one direction only; there
  is no `toEntity`). Return types are the hand-written `api.model` classes or
  `casx/dto` DTOs — never JPA entities.
* Annotate with `@Component` (preferred) and constructor-inject any collaborating
  transformers.
* Method naming is not fully consistent across the codebase; prefer
  `transformJpaToApi(entity): ApiModel` for new transformers. Existing variants
  include `toApi`, `transformToApi`, and domain-specific `to<Model>()`.
* Accept nullable input and return nullable output where appropriate
  (`fun transformJpaToApi(jpa: XEntity?) = jpa?.let { ... }`).
* Transformers may contain presentation/derivation logic (status derivation,
  date calculations) but must not perform persistence or core business
  transactions — that belongs in services.

---

## Database migrations (Flyway)

* Add a **new** timestamped SQL file under
  `src/main/resources/db/migration/all/` — never edit an already-applied
  migration. Prefer `./script/generate_migration`, which stamps the current
  date/time.
* Filename format: `<version>__<snake_case_description>.sql`, where `<version>` is
  the actual current date/time as `yyyyMMddHHmmss` (today — never a future or
  arbitrary date). **Match the format of the most recently added migration** —
  check the newest file(s) and follow the same prefix convention (recent
  migrations use a 17-digit prefix: a 14-digit timestamp padded with three
  trailing zeros, e.g. `20260804163224000`).
* A new migration must sort LAST.

* Write plain, direct DDL — the codebase convention is **not** to use
  `IF EXISTS` / `IF NOT EXISTS` guards (only a small minority of migrations do).
  Destructive/rename migrations are written directly (e.g.
  `ALTER TABLE beds RENAME TO cas1_beds;`, `ALTER TABLE x DROP CONSTRAINT y;`),
  relying on migrations running exactly once in order. Match the surrounding
  migrations rather than adding defensive existence checks.
* If a new column is added and its value is not backfilled, we should document 
  on any corresponding JPA property when the column was added and that a backfill
  wasn't performed. This is helpful when determining if the column can be used
  for historic data

---

## Domain events

* When persisted data changes, the relevant service emits a **domain event**
  (ADR 0004). Events are stored in `domain_events` (`DomainEventEntity.kt`,
  typed by `DomainEventType`) and published to SQS for probation-integration
  consumers; CAS1 also renders them into user-facing timelines via
  `TimelineFactory`/`DomainEventDescriber` components under
  `service/cas1/domainevent/**`.
* Treat the event JSON schema as a **published contract**. Before changing an
  event's shape read `doc/how-to/modifying_domain_event_schemas.md`: existing
  persisted JSON must still deserialise. Prefer backwards-compatible additions
  (bump the schema version on `DomainEventType` and set it when creating the
  event); only introduce a new `..._V2` type when a change is genuinely
  breaking, and add migration/adaptation plus tests for the older version.

---

## Subject Access Requests

* We implement endpoints to retrieve data and templates to generate a subject
  access request for a given CRN/NOMS number. 
* General documentation on how this is tested is provided by `doc/how-to/sar-test-fixture-guide.md`
* There are automatic checks in the build process to ensure the user is prompted 
  to review/revise the SAR template whenever a change to the database model is made
* We should also consider if the SAR template needs updating when a new domain 
  event type is added. At a minimum, a human-readable version of the domain
  event type will need adding into the template

---

## Feature flags

* Feature flags are Spring config driven (`feature-flags.*` in
  `application*.yml`, overridden per-environment via env vars), not Flipt for new
  work. See `doc/how-to/add-a-feature-flag.md`. First decide whether a flag is
  actually warranted; provide a safe default (usually `false`) and a test default.

---

## Testing

**Frameworks:**

* **JUnit 5** — `@Test`, `@Nested`, `@ParameterizedTest` with
  `@MethodSource`/`@EnumSource`/`@CsvSource`.
* **AssertJ** — `assertThat(...)`, preferred over JUnit `assertEquals` for new code.
* **MockK** — `mockk()`, `every { } returns`, `verify { }`, `confirmVerified`,
  `just Runs`.
* **kfactory** — `Factory<T>` builders under `src/test/.../factory`.

**Naming:**

* Test classes end in `Test` and live in the package mirroring the class under test.
* Test methods use **backtick descriptive names** stating behaviour and, for
  endpoints, the expected HTTP status (e.g.
  `` `Create a Booking without a JWT returns 401` ``,
  `` `transformJpaToApi transforms correctly` ``).
* Group related cases with `@Nested inner class` (Kotlin requires the `inner`
  modifier so JUnit can instantiate it), named in PascalCase after the method or
  scenario under test (e.g. `TransformJpaToApi`, `WhenThereAreTimelineEvents`,
  `WithExternalUser`); nest further for sub-scenarios (e.g. a
  `FeatureFlagFalse`/`FeatureFlagTrue` split inside a per-method class).
* Before writing, read a neighbouring test in the same package and mirror its
  imports, structure, and assertion style.

### Unit tests

* Live under `unit/**` (e.g. `unit/service`, `unit/transformer`) and the
  service-scoped `cas1/unit/**`, `cas2/unit/**`, `cas3/unit/**`.
* Pure JVM — **no Spring context**. Mock collaborators with MockK; verify
  interactions with `verify`/`confirmVerified`.
* Build data with in-memory factories: `XxxEntityFactory().withY(...).produce()`.
* Cover the happy path plus **every branch, boundary, and error condition**. Use
  parameterised tests for role/enum/date matrices, following existing examples.
* Fast check while iterating: `./gradlew compileTestKotlin -q`.
* Tests mirror the main source package structure.

### Integration tests

* Live under `integration/**` and the `cas1/`, `cas2/`, `cas3/` peers; extend
  `IntegrationTestBase()`. Repository/query behaviour and N+1 checks go in
  `repository/**` / `integration/*QueryTest.kt` (see `NPlus1QueriesTest`).
* Use real beans via `@Autowired`, `webTestClient` for HTTP calls, real Postgres,
  and Wiremock (helpers under `integration/httpmocks`) for external APIs.
* Set up data with `givenA*` helpers (`givenAUser`, `givenAnApplication`,
  `givenAnOffender`, `givenACas3PremisesAndBedspace`, ...) and persisted factories
  `xxxFactory.produceAndPersist { withY(...) }`. Authenticate with the JWT from
  `givenAUser(roles = listOf(...))` as a bearer token.
* Keep tests deterministic and isolated: no reliance on ordering; fixed dates/ids
  where assertions depend on them. Needs the test DB up (`./script/test_database`).
* **For an endpoint, cover:** (1) **auth** — `401` without a JWT and `403` when
  the caller lacks the required role (parameterise across allowed/disallowed roles
  where the codebase does); (2) **happy path** — success status + response body;
  (3) **validation/error paths** — `400`/`404`/`409` as applicable;
  (4) **side effects** — persisted state changes and **domain events** raised.
  New/changed endpoints must have auth tests.

### Keeping tests in sync

* When adding/removing an entity field, update the entity, repository queries,
  services, transformers, seed/migration jobs, **all** test factories (backing
  field, `with*` builder, `produce()` assignment), `IntegrationTestBase` wiring,
  and affected tests. If a persisted factory is missing wiring in
  `IntegrationTestBase`, add it consistently with the others.

---

## Dead code

Actively highlight likely **dead code**, but **only within the files you are
modifying in the current change** — do not audit the wider codebase for dead code
unless explicitly asked. When you touch a file, flag any symbol *in that file*
(functions, classes, properties, endpoints) that is **not referenced by any
controller or service**, either because nothing references it at all or because
the **only** references are from tests. Surface these so they can be reviewed and
removed, but never delete on suspicion alone — expected outcomes and expected
error paths are still "used".

Before flagging a symbol as dead, confirm with a **deep, exhaustive** sweep (a
single "no results" grep does **not** prove it is dead):

* Search the whole tree — `src/main` **and** `src/test`, Kotlin **and** Java,
  plus resources (`*.yml`/`*.properties`, Flyway SQL, templates). Prefer an
  authoritative `grep -rn "symbol" .` because scoped search tools can truncate.
* Account for **indirect references** a text search misses: reflection /
  string-based lookup (`::member`, JPQL/native SQL names), Spring wiring
  (`@Component`/`@Bean` injected by type, `@Scheduled`, `@EventListener`,
  `@ConditionalOn*`), serialization / API contract (Jackson `@JsonProperty`, enum
  values, the hand-written `api/model` DTOs and legacy `*Delegate`/`*Api`
  overrides), interface/abstract overrides and sealed hierarchies, and
  seed/migration jobs or feature-flagged paths invoked via `script/` helpers.
* **Classify precisely:**
  * *Fully unreferenced* — no direct, reflective, wired, serialized, or overridden
    references anywhere: strong dead-code candidate.
  * *Test-only* — referenced **solely** by tests/factories with no production
    caller: flag it (the production code is dead even though tests keep it
    compiling), and note the orphaned tests/factories that would go with it.
* **Consider external consumers before concluding.** Public REST endpoints,
  published domain-event schemas, and library-style public functions may be used
  outside this repo — treat these as *not* dead without explicit confirmation.
* **Report with evidence** (where you searched, commands used, confidence), and if
  removal is approved, delete the symbol together with everything it orphans
  (now-unused imports, private helpers, test factories/builders, and — for a
  DB-backed entity — a Flyway drop migration), then re-run the lint/build gates.

---

## Post-change tidy up

Once you've finished generating code, tidy it up and validate:

```bash
./gradlew ktlintFormat   # auto-fix formatting
./gradlew ktlintCheck
./gradlew detekt
```

Fix every issue raised by `ktlintCheck` and `detekt`. For structural/cross-file
changes also run `./gradlew compileKotlin compileTestKotlin -q`. If a detekt rule
is genuinely unavoidable, add a justified entry to `detekt-baseline.xml` (and
remove stale baseline entries for code you delete).

---

## Continuous integration

* PRs to `main` run `.github/workflows/pr_verify.yml` → `verify.yml`, which
  enforces the same gates you can run locally, plus a couple of extras:
  * migration filename validation (`./script/verify_migration_scripts`),
  * `./gradlew assemble testClasses`, `ktlintCheck`, `detekt`, the test suite,
    and Helm lint.
  Code that fails these locally will fail CI — always run the lint/build/test
  commands above before pushing.
* `prek`/`pre-commit` hooks can run the baseline checks on every commit locally
  (`prek install` — see `README.md`).

---

## Trust these instructions

Trust the information in this guide and prefer it over re-deriving conventions by
searching. Only fall back to exploring the codebase when the information here is
incomplete, ambiguous, or demonstrably out of date — and when you find it wrong,
prefer updating this file so the next change benefits.
