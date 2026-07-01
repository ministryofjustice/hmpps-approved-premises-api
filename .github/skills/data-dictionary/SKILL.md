---
name: data-dictionary
description: 'Generate or update a data dictionary documenting the database schema for this repository (hmpps-approved-premises-api). USE FOR: building a reference of JPA entities, database tables, columns, data types, relationships, and enums; documenting the schema for CAS1 (Approved Premises), CAS2 (Transitional Accommodation), CAS3 (Temporary Accommodation), and shared domains; producing table-to-entity mappings; onboarding docs for the data model. Trigger words: data dictionary, schema documentation, entity reference, table catalog, document the database, ERD, column reference.'
argument-hint: '[domain: cas1 | cas2 | cas3 | shared | all] [format: csv | markdown | both] [diagram: on | off]'
---

# Data Dictionary Generator

Produce a structured, accurate data dictionary for the database schema of this Kotlin/Spring Boot repository by reading the JPA entities (source of truth for the object model) and Flyway migrations (source of truth for the physical schema).

## When to Use

- The user asks to create, regenerate, or update a data dictionary / schema reference.
- Documenting tables, columns, types, relationships, or enums for one or more domains.
- Onboarding material describing the data model.

## Where Things Live

| Concern | Location |
|---------|----------|
| Shared entities | `src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/jpa/entity/` |
| CAS1 entities | `.../jpa/entity/cas1/` and `.../cas1/entity/` |
| CAS2 entities | `.../cas2/jpa/entity/` |
| CAS3 entities | `.../cas3/jpa/entity/` (plus `v2/` variants) |
| Flyway migrations | `src/main/resources/db/migration/` (`all/`, `dev/`, `integration/`, etc.) |
| Entity conventions | [doc/how-to/best-practice-jpa-entities.md](../../../doc/how-to/best-practice-jpa-entities.md) |

Migration file naming: `YYYYMMDDhhmmss__description.sql`; repeatable migrations use the `R__` prefix.

## Domain Scope

If the user names a domain (`cas1`, `cas2`, `cas3`, `shared`, or `all`), limit the work to that scope. If unspecified, ask which domain(s) to document — generating all 80+ entities at once is large, so default to confirming scope before a full run.

## Generation Options

This skill is configurable. Read any options the user gives in their request; for anything not specified, use the **default** and (for large runs) confirm before generating. Don't interrogate the user for every option — infer sensible defaults and state what you chose.

| Option | Values | Default | Effect |
|--------|--------|---------|--------|
| `domain` | `cas1` \| `cas2` \| `cas3` \| `shared` \| `all` | ask / confirm | Which domain(s) to document. `all` is a large run — confirm first. |
| `format` | `csv` \| `markdown` \| `both` | `both` | Which artefact(s) to produce. `csv` = machine-readable only; `markdown` = wiki/Confluence document only; `both` = CSV + markdown kept in sync. |
| `diagram` | `on` \| `off` | `on` | Whether the markdown document includes the Mermaid ER diagram. Ignored when `format = csv`. |
| `tables` | `on` \| `off` | `on` | Whether the markdown document includes the full per-table column tables. Ignored when `format = csv`. |
| `write-mode` | `overwrite` \| `update` \| `dry-run` | `update` | `overwrite` regenerates files wholesale; `update` refreshes in place (preserving hand-written prose/notes where possible); `dry-run` reports what would change without writing. |
| `index` | `on` \| `off` | `on` | Whether to create/refresh `doc/data-dictionary/README.md`. |

Notes on combinations:

- `format = csv` skips the markdown document entirely (`diagram`/`tables` have no effect).
- `format = markdown` produces only `<domain>.md`. If a `<domain>.csv` already exists it is used as the data source; otherwise extract from entities directly.
- `diagram = off` with `tables = off` and `format = markdown` produces only the heading + sources — warn the user this is near-empty and confirm.
- Always honour the chosen `domain` scope regardless of other options.

## Output

Produce, per domain in scope, under `doc/data-dictionary/`, the artefact(s) selected by the `format` option (default `both`):

1. **`<domain>.csv`** (when `format` is `csv` or `both`) — the machine-readable dictionary, one row per column. Columns:
   `domain,table,entity,column,sql_type,kotlin_type,nullable,key,enum_values,relationship,notes`
   See the [CSV template](./references/template.md) for exact formatting and quoting rules.

2. **`<domain>.md`** (when `format` is `markdown` or `both`) — a **wiki-ready** (Confluence-friendly) document containing, subject to the `diagram` and `tables` options:
   - a per-domain **Mermaid ER diagram link** plus source links (when `diagram = on`); see below, and
   - the **full dictionary as markdown tables** (one table per database table, one row per column) when `tables = on`,
     so the document can be pasted/imported into a wiki such as Confluence without the CSV.
   See the [markdown template](./references/template.md#markdown-tables-wiki--confluence) and [diagram template](./references/template.md#mermaid-er-diagram).

3. **`erd-<domain>.mermaid`** (when `format` is `markdown` or `both` and `diagram = on`) — a standalone Mermaid file containing the entity–relationship diagram in left-to-right layout. The markdown file links to this file so readers can view the diagram source or render it in other tools. See [diagram template](./references/template.md#mermaid-er-diagram).

When `format = both`, the CSV and the markdown tables must contain the **same data** — the CSV is the source of truth for machine use, the markdown is the human/wiki view. Keep them in sync.

When `index = on` (default), also maintain `doc/data-dictionary/README.md` as an index linking each domain's generated artefact(s), including the `.mermaid` files.

> Confluence note: Confluence can import/render Markdown tables directly. If you embed the diagrams inline instead of linking to `.mermaid` files, Mermaid diagrams may require a Mermaid macro/plugin; if unavailable, the markdown tables and source links still stand on their own. The separate `.mermaid` files allow external tools and CI/CD pipelines to process diagrams independently.

## Procedure

1. **Confirm scope and options.** Determine the `domain` scope and resolve each [Generation Option](#generation-options) (`format`, `diagram`, `tables`, `write-mode`, `index`) from the user's request, applying defaults for anything unspecified. Briefly state the resolved options. For an `all` run or when `write-mode = overwrite` would replace an existing dictionary, confirm first. If `write-mode = dry-run`, perform extraction and report the diff/counts but do not write files.

2. **Enumerate entities in scope.** List the entity files in the relevant package(s). For each `@Entity` class, extract:
   - **Table name** from `@Table(name = "...")` (fall back to the class name if absent).
   - **Entity class name** and source file path.
   - **Domain** (CAS1 / CAS2 / CAS3 / shared) based on the package.
   - **Whether it is a physical table.** An `@Entity` is a *physical table* only if it has a `@Table` (or default-named table) backed by a `CREATE TABLE` migration. Treat as a **query-backed projection** (not a physical table) any `@Entity` that has no backing `CREATE TABLE`/`CREATE VIEW` and is hydrated by a native query — e.g. `@Subselect`, a `nativeQuery = true` repository method, or a `UNION ALL` over other tables (e.g. `Task`). Treat as a **view-backed** entity any whose backing object is a `CREATE VIEW` (e.g. cas2 `cas_2_application_live_summary`). Record which kind each entity is; these do not go in the physical `## Tables` section (see step 7).
   - **Deprecation status.** Check for `@Deprecated` annotations on the entity class; also cross-reference against known deprecation docs (e.g. `doc/deprecations/*.md`). Record the deprecation message if present.

3. **Extract columns.** For each entity property, capture:
   - Column name (`@Column(name = ...)` / `@JoinColumn(name = ...)`, else the Kotlin property name).
   - Kotlin type and nullability (`?` denotes nullable).
   - Key/constraint markers: `@Id`, `@GeneratedValue`, unique constraints.
   - Enum columns (`@Enumerated(EnumType.STRING)`) — record the enum type and its allowed values.
   - Audit fields (`@CreationTimestamp`, `createdAt`, `updatedAt`).

4. **Extract relationships.** Record each `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne` with its target entity, join column / join table, and cardinality. Note inheritance hierarchies (`@Inheritance`, `@DiscriminatorColumn`, `@DiscriminatorValue`).

5. **Cross-check against migrations and extract queries.** For ambiguous types, precise SQL types, defaults, indexes, or constraints not visible in the entity, search `src/main/resources/db/migration/` for the table's `CREATE TABLE` / `ALTER TABLE` / `CREATE VIEW` statements and reconcile. Flag any mismatch between entity and migration. If an entity has **no** `CREATE TABLE`/`CREATE VIEW` at all, do not flag it as a missing table — classify it as a query-backed projection (see step 2). Beware renamed tables (e.g. `confirmations` → `cas3_confirmations`) which can look like missing migrations. For projections and views, extract and record the **native SQL query** that hydrates them (e.g. from `@Query(value = "...", nativeQuery = true)`, `@Subselect`, or repository method code).

6. **Write the CSV** (when `format` is `csv` or `both`). Emit one row per column following the [CSV template](./references/template.md). Sort by `table`, then by column order in the entity. Quote any field containing commas.

7. **Write the markdown document** `<domain>.md` (when `format` is `markdown` or `both`). Generate, per the resolved options:
   - a Mermaid `erDiagram` shown via link to a separate `.mermaid` file (when `diagram = on`; see step 8 below);
   - the **full dictionary as markdown tables** — one `### <table>` section per database table, each with a markdown table of its columns (mirroring the CSV rows for that table) — only when `tables = on`; **each `### <table>` heading must be followed by an entity line that includes both the entity class name *and* the physical table name**, plus any deprecation status;
   - a separate `## Query-backed projections (not physical tables)` section for any projection/view-backed entities identified in step 2, with the associated **SQL query** for each, so they are not mistaken for physical tables (see the [projections template](./references/template.md#query-backed-projections-not-physical-tables)); keep these out of the physical `## Tables` section and out of the ER diagram;
   - a `## Sources` table with workspace-relative links to entity and migration sources (always).
   See the [markdown template](./references/template.md#markdown-tables-wiki--confluence) and [diagram template](./references/template.md#mermaid-er-diagram).

   **Link paths:** the files live in `doc/data-dictionary/`, so links to repo sources are relative to that folder — prefix entity/migration paths with `../../` (e.g. `../../src/main/kotlin/...`), other `doc/` pages with `../` (e.g. `../how-to/...`), and sibling CSV/README/`.mermaid` files with `./`. Keep each link's display text matching its target. See [Link paths](./references/template.md#link-paths-important).

8. **Create the separate Mermaid diagram file** `erd-<domain>.mermaid` (when `format` is `markdown` or `both` and `diagram = on`). Generate a Mermaid `erDiagram` with left-to-right layout configuration (`%%{init: {'flowchart': {'direction': 'LR'}}}%%` at the start) showing each physical table and its relationships (cardinality from the JPA annotations). Query-backed projections and view-backed entities are excluded from the diagram. The markdown document links to this file with `![Entity Relationship Diagram](./erd-<domain>.mermaid)` plus a text link to the source.

9. **Update the index** `doc/data-dictionary/README.md` (when `index = on`) to link the new/updated artefact(s) for each domain, including any `.mermaid` files generated.

10. **Validate.** Confirm every in-scope entity is represented in the produced artefact(s). For the CSV: table names match migrations, relationships are bidirectionally consistent, enum value lists are complete. For the markdown: every table appears in the Mermaid diagram (if `diagram = on`) and has a table section (if `tables = on`), and the markdown tables contain the same rows as the CSV (when `format = both`). If `.mermaid` files are generated, verify they render without errors in standard Mermaid viewers. Report counts (entities, tables, columns, relationships), the resolved options, and any unresolved flags.

## Quality Checks

Apply the checks relevant to the produced artefact(s):

- Table name in the CSV matches the `@Table` annotation **and** the migration `CREATE TABLE` — unless the entity is a query-backed projection or view-backed entity, which by definition has no `CREATE TABLE` and belongs under `## Query-backed projections`, not `## Tables`.
- Deprecated entities are noted in the markdown header and clearly flagged so consumers know not to use them.
- Nullability and types reflect the Kotlin source; `sql_type` reflects the migration.
- Every relationship names a concrete target entity and join mechanism.
- Enum columns list all values in `enum_values`, not just the type name.
- When the markdown is produced: every table appears as a node in the Mermaid diagram (if `diagram = on`) **and** has a markdown table section (if `tables = on`).
- When `format = both`: the markdown tables and the CSV describe the same columns (same count and order per table).
- All source links in `<domain>.md` resolve: entity/migration links use `../../`, other `doc/` links use `../`, sibling files use `./`, and each link's display text matches its target.
- No entity in scope is silently skipped; note any entity intentionally excluded.

## Notes

- Entities are the authoritative object model; migrations are authoritative for physical column types, defaults, and indexes. When they disagree, document both and flag it.
- Not every `@Entity` is a physical table. Query-backed projections (no `@Table`/`CREATE TABLE`, hydrated by native queries, `@Subselect`, or `UNION ALL` — e.g. `Task`) and view-backed entities (`CREATE VIEW`, e.g. cas2 `cas_2_application_live_summary`) describe a derived result shape, not stored columns. Document them under `## Query-backed projections (not physical tables)` with their backing SQL query and label each clearly so readers and tooling don't treat them as real tables.
- Include the **physical table name** in the markdown entity line (alongside the entity class name) so teams can map between the JPA object model and physical database artifacts.
- Flag **deprecated entities** (via `@Deprecated` annotation or known deprecation docs like `doc/deprecations/*.md`) in the markdown header so consumers know not to use them.
- CAS2 is partly deprecated/migrating to v2 and CAS3 has `v2/` entities — note version when documenting these.
- Keep the dictionary regenerable: re-running this skill should reproduce the same structure so diffs reflect real schema changes.
