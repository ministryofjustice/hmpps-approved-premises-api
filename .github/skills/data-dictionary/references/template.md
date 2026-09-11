# Data Dictionary Output Templates

Three artefacts per domain, written to `doc/data-dictionary/`:

1. `<domain>.csv` — the machine-readable dictionary (one row per column).
2. `<domain>.md` — a wiki-ready (Confluence-friendly) document with a link to the Mermaid ER diagram,
   the full dictionary as markdown tables, and source links.
3. `erd-<domain>.mermaid` — the separate Mermaid entity–relationship diagram file (when `diagram = on`).

Plus `doc/data-dictionary/README.md` as an index.

---

## CSV format

Header row (exact column order):

```
domain,table,entity,column,sql_type,kotlin_type,nullable,key,enum_values,relationship,notes
```

Field rules:

- `domain` — `cas1` | `cas2` | `cas3` | `shared`.
- `table` — value from `@Table(name = ...)`, else the class name.
- `entity` — Kotlin entity class name.
- `column` — `@Column`/`@JoinColumn` name, else the Kotlin property name.
- `sql_type` — physical type from the migration (e.g. `uuid`, `varchar`, `timestamptz`, `date`). Leave blank if not found and add a note.
- `kotlin_type` — the property type (e.g. `UUID`, `String`, `OffsetDateTime`).
- `nullable` — `yes` / `no` (Kotlin `?` = yes).
- `key` — `PK`, `FK`, `UNIQUE`, or blank. For FKs put the target in `relationship`.
- `enum_values` — for `@Enumerated` columns, pipe-separated allowed values, e.g. `provisional|confirmed|arrived`.
- `relationship` — for FK / association columns: `<Type> -> <TargetTable>` e.g. `ManyToOne -> cas3_premises`.
- `notes` — anything else (audit timestamp, default, discriminator, entity/migration mismatch flag).

Quoting: wrap any field containing a comma, quote, or newline in double quotes and escape inner quotes by doubling them. Sort rows by `table`, then by the column order within the entity.

### Example rows

```
domain,table,entity,column,sql_type,kotlin_type,nullable,key,enum_values,relationship,notes
cas3,cas3_bookings,Cas3BookingEntity,id,uuid,UUID,no,PK,,,
cas3,cas3_bookings,Cas3BookingEntity,arrival_date,date,LocalDate,no,,,,
cas3,cas3_bookings,Cas3BookingEntity,status,varchar,BookingStatus,no,,provisional|confirmed|arrived|departed|cancelled,,"@Enumerated(STRING)"
cas3,cas3_bookings,Cas3BookingEntity,premises_id,uuid,UUID,no,FK,,ManyToOne -> cas3_premises,
cas3,cas3_bookings,Cas3BookingEntity,created_at,timestamptz,OffsetDateTime,no,,,,@CreationTimestamp
```

---

## Markdown tables (wiki / Confluence)

The `<domain>.md` document must include the **full dictionary as markdown tables** so it can be
pasted or imported into a wiki (e.g. Confluence) without the CSV. Structure:

- A `## Tables` section.
- One `### <table>` subsection per database table (sorted by table name).
  - A one-line caption: the entity class name and a link to its source file.
  - A markdown table with one row per column, mirroring the CSV rows for that table.

Use this column order (same data as the CSV, minus the redundant `domain`/`table`/`entity` columns
which are implied by the section heading):

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK | | | |
| `arrival_date` | date | LocalDate | no | | | | |
| `status` | varchar | BookingStatus | no | | provisional / confirmed / arrived / departed / cancelled | | `@Enumerated(STRING)` |
| `premises_id` | uuid | UUID | no | FK | | ManyToOne → cas3_premises | |
| `created_at` | timestamptz | OffsetDateTime | no | | | | `@CreationTimestamp` |

Formatting rules for wiki compatibility:

- Wrap `column` names in backticks.
- In `enum_values`, separate values with ` / ` (slash) rather than `|`, because `|` breaks markdown table cells.
- Use `→` for relationship arrows (renders cleanly in Confluence).
- Escape or avoid literal `|` inside any cell; replace with `\|` or a slash.
- Keep one table per database table so wiki pages stay navigable; add a `### <table>` anchor for each.

### Example section (physical table)

```markdown
### cas3_bookings

Table: `cas3_bookings` | Entity: `Cas3BookingEntity` — [source](../../src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/cas3/jpa/entity/Cas3BookingEntity.kt)

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK | | | |
| `arrival_date` | date | LocalDate | no | | | | |
| `status` | varchar | BookingStatus | no | | provisional / confirmed / arrived / departed / cancelled | | `@Enumerated(STRING)` |
| `premises_id` | uuid | UUID | no | FK | | ManyToOne → cas3_premises | |
| `created_at` | timestamptz | OffsetDateTime | no | | | | `@CreationTimestamp` |
```

### Example section (deprecated table)

```markdown
### arrivals

Table: `arrivals` | Entity: `ArrivalEntity` (⚠️ **deprecated** — see [doc/deprecations/deprecations-bookings.md](../deprecations/deprecations-bookings.md)) — [source](../../src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/jpa/entity/ArrivalEntity.kt)

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK | | | |
```

---

## Query-backed projections (not physical tables)

Some `@Entity` classes are **not** physical tables: they have no `@Table`/`CREATE TABLE` and are
hydrated by a native query (`@Subselect`, a `nativeQuery = true` repository method, or a `UNION ALL`
over other tables \u2014 e.g. `Task`), or they are backed by a database `CREATE VIEW` (e.g. cas2
`cas_2_application_live_summary`). Their columns describe a derived result shape, not stored columns.

Document these in a dedicated section **after** `## Tables`, and keep them **out** of the physical
`## Tables` section and the ER diagram, so they are not mistaken for real tables:

- Add a `## Query-backed projections (not physical tables)` heading with a one-line explanation.
- One `### <name>` subsection per projection, with the entity name and a short prose note stating it
  is not a physical table/view and how it is populated.
- A markdown column table in the same format as physical tables.
- In the CSV, still emit one row per column (so `format = both` stays in sync), but make the
  non-physical status explicit \u2014 e.g. a `notes` value like
  `projection; native UNION ALL over assessments + placement_applications; no physical table`.

### Example section

```markdown
## Query-backed projections (not physical tables)

These entities have no `@Table` annotation and no `CREATE TABLE`/`CREATE VIEW` migration. They are
read-only projections hydrated by native queries and are listed here for reference only — they do
not exist as physical tables in the database.

### tasks

Table: `tasks` (projection) | Entity: `Task` — [source](../../src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/jpa/entity/TaskEntity.kt)

Not a physical table or view. Populated by `TaskRepository.getAll(...)` as a native `UNION ALL` over
`assessments` and `placement_applications`. The columns below describe the projection's result
shape, not stored columns.

**Backing query (simplified):**
```sql
(SELECT ... FROM assessments ...)
UNION ALL
(SELECT ... FROM placement_applications ...)
```

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK | | | projection; native UNION ALL over assessments + placement_applications; no physical table |
| `type` | text | TaskEntityType | no | | ASSESSMENT / PLACEMENT_APPLICATION | | |

### Example: view-backed entity

Table: `cas_2_application_live_summary` (view) | Entity: `Cas2ApplicationSummaryEntity` — [source](../../src/main/kotlin/.../Cas2ApplicationSummaryEntity.kt)

View-backed entity created by `CREATE VIEW` migration. Read-only projection of CAS2 application
state. See [20240101120000__create_cas2_application_live_summary_view.sql](../../src/main/resources/db/migration/all).

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK | | | view-backed entity |

---

## Mermaid ER diagram

In `<domain>.md`, include a link to the diagram and source links. Structure:

- An `# Data Dictionary — <Domain>` heading.
- A link to the domain CSV.
- A `## Entity–Relationship Diagram` section containing a **link** to the separate `erd-<domain>.mermaid` file, plus a source code link.
- A `## Tables` section with the full per-table markdown tables (see [Markdown tables](#markdown-tables-wiki--confluence)).
- Optional `## Query-backed projections (not physical tables)` section for view/projection entities.
- A `## Sources` table mapping each table to its entity and migration source links.

**Markdown syntax for the diagram section:**

```markdown
## Entity–Relationship Diagram

![Entity Relationship Diagram](./erd-<domain>.mermaid)

_See also: [erd-<domain>.mermaid](./erd-<domain>.mermaid) (Mermaid source)_
```

**Content of `erd-<domain>.mermaid` file:**

The file contains a standard Mermaid `erDiagram` with left-to-right layout configuration at the top:

```mermaid
%%{init: {'flowchart': {'direction': 'LR'}}}%%
erDiagram
    tableA ||--o{ tableB : "relationship"
    ...
```

- Declare relationships as `tableA <relation> tableB : "label"`.
- Optionally include key columns inside each table block (`uuid id PK`, `uuid premises_id FK`). You need not list every column — the CSV is the full reference.
- Only include **physical tables** (those backed by `CREATE TABLE` migrations). Exclude query-backed projections and view-backed entities.

### Link paths (important)

The generated files live in `doc/data-dictionary/`, so **all links to repository sources must be
relative to that folder**. Two directories up reaches the repo root:

- Entities/migrations: prefix with `../../` — e.g. `../../src/main/kotlin/.../Cas3BookingEntity.kt`, `../../src/main/resources/db/migration/all`.
- Other docs (e.g. `doc/how-to/...`): prefix with `../` — e.g. `../how-to/best-practice-jpa-entities.md`.
- Sibling artefacts in the same folder (the CSV, README, `.mermaid` files): use `./` — e.g. `./cas3.csv`, `./erd-cas3.mermaid`.
- Make the link **display text** match the path it points at (don't show `doc/how-to/x.md` while linking to `../how-to/x.md`); use the short repo-relative form as the label, e.g. `[src/.../Cas3BookingEntity.kt](../../src/.../Cas3BookingEntity.kt)`.

Verify every link target resolves to an existing file/dir before finishing.

Cardinality mapping from JPA annotations:

| Annotation | Mermaid relation |
|------------|------------------|
| `@OneToMany` | `\|\|--o{` |
| `@ManyToOne` | `}o--\|\|` |
| `@OneToOne` | `\|\|--\|\|` |
| `@ManyToMany` | `}o--o{` |

---

## Index (`README.md`)

A table linking each domain to its CSV, markdown document, and diagram file:

| Domain | Dictionary (CSV) | Document (Markdown) | Diagram (Mermaid) |
|--------|------------------|---------------------|-------------------|
| CAS1 — Approved Premises | `cas1.csv` | `cas1.md` | `erd-cas1.mermaid` |
| CAS2 — Transitional Accommodation | `cas2.csv` | `cas2.md` | `erd-cas2.mermaid` |
| CAS3 — Temporary Accommodation | `cas3.csv` | `cas3.md` | `erd-cas3.mermaid` |
| Shared | `shared.csv` | `shared.md` | `erd-shared.mermaid` |

Each `<domain>.md` is wiki-ready: it contains a link to the Mermaid ER diagram and the full dictionary as markdown tables for pasting into Confluence. The separate `.mermaid` files allow diagram rendering in external tools and CI/CD pipelines.

Add a note: "Generated with the `data-dictionary` skill. Re-run to refresh after schema changes."
