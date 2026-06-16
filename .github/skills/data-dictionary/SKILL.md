---
name: data-dictionary
description: 'Generate or update a data dictionary documenting the database schema for this repository (hmpps-approved-premises-api). USE FOR: building a reference of JPA entities, database tables, columns, data types, relationships, and enums; documenting the schema for CAS1 (Approved Premises), CAS2 (Transitional Accommodation), CAS3 (Temporary Accommodation), and shared domains; producing table-to-entity mappings; onboarding docs for the data model. Trigger words: data dictionary, schema documentation, entity reference, table catalog, document the database, ERD, column reference.'
argument-hint: '[optional: domain to document — cas1 | cas2 | cas3 | shared | all]'
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

## Output

Produce two artefacts per domain in scope, under `doc/data-dictionary/`:

1. **`<domain>.csv`** — the machine-readable dictionary, one row per column. Columns:
   `domain,table,entity,column,sql_type,kotlin_type,nullable,key,enum_values,relationship,notes`
   See the [CSV template](./references/template.md) for exact formatting and quoting rules.
2. **`<domain>.md`** — a **wiki-ready** (Confluence-friendly) document containing:
   - a per-domain **Mermaid ER diagram** plus source links, and
   - the **full dictionary as markdown tables** (one table per database table, one row per column),
     so the document can be pasted/imported into a wiki such as Confluence without the CSV.
   See the [markdown template](./references/template.md#markdown-tables-wiki--confluence) and [diagram template](./references/template.md#mermaid-er-diagram).

The CSV and the markdown tables must contain the **same data** — the CSV is the source of truth for machine use, the markdown is the human/wiki view. Keep them in sync.

Also maintain `doc/data-dictionary/README.md` as an index linking each domain's CSV and markdown document.

> Confluence note: Confluence can import/render Markdown tables directly. Mermaid diagrams may require a Mermaid macro/plugin; if unavailable, the markdown tables and source links still stand on their own.

## Procedure

1. **Confirm scope.** Determine which domain(s) to document. Ask before overwriting an existing dictionary.

2. **Enumerate entities in scope.** List the entity files in the relevant package(s). For each `@Entity` class, extract:
   - **Table name** from `@Table(name = "...")` (fall back to the class name if absent).
   - **Entity class name** and source file path.
   - **Domain** (CAS1 / CAS2 / CAS3 / shared) based on the package.

3. **Extract columns.** For each entity property, capture:
   - Column name (`@Column(name = ...)` / `@JoinColumn(name = ...)`, else the Kotlin property name).
   - Kotlin type and nullability (`?` denotes nullable).
   - Key/constraint markers: `@Id`, `@GeneratedValue`, unique constraints.
   - Enum columns (`@Enumerated(EnumType.STRING)`) — record the enum type and its allowed values.
   - Audit fields (`@CreationTimestamp`, `createdAt`, `updatedAt`).

4. **Extract relationships.** Record each `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne` with its target entity, join column / join table, and cardinality. Note inheritance hierarchies (`@Inheritance`, `@DiscriminatorColumn`, `@DiscriminatorValue`).

5. **Cross-check against migrations.** For ambiguous types, precise SQL types, defaults, indexes, or constraints not visible in the entity, search `src/main/resources/db/migration/` for the table's `CREATE TABLE` / `ALTER TABLE` statements and reconcile. Flag any mismatch between entity and migration.

6. **Write the CSV.** Emit one row per column following the [CSV template](./references/template.md). Sort by `table`, then by column order in the entity. Quote any field containing commas.

7. **Write the markdown document** `<domain>.md`. Generate:
   - a Mermaid `erDiagram` showing each table and its relationships (cardinality from the JPA annotations);
   - the **full dictionary as markdown tables** — one `### <table>` section per database table, each with a markdown table of its columns (mirroring the CSV rows for that table);
   - a `## Sources` table with workspace-relative links to entity and migration sources.
   See the [markdown template](./references/template.md#markdown-tables-wiki--confluence) and [diagram template](./references/template.md#mermaid-er-diagram).

8. **Update the index** `doc/data-dictionary/README.md` to link the new/updated CSV and markdown document.

9. **Validate.** Confirm every in-scope entity is in the CSV, table names match migrations, relationships are bidirectionally consistent, enum value lists are complete, every table in the CSV appears in the Mermaid diagram, and the markdown tables contain the same rows as the CSV. Report counts (entities, tables, columns, relationships) and any unresolved flags.

## Quality Checks

- Table name in the CSV matches the `@Table` annotation **and** the migration `CREATE TABLE`.
- Nullability and types reflect the Kotlin source; `sql_type` reflects the migration.
- Every relationship names a concrete target entity and join mechanism.
- Enum columns list all values in `enum_values`, not just the type name.
- Every table in the CSV appears as a node in the domain's Mermaid diagram **and** has a markdown table section in `<domain>.md`.
- The markdown tables and the CSV describe the same columns (same count and order per table).
- No entity in scope is silently skipped; note any entity intentionally excluded.

## Notes

- Entities are the authoritative object model; migrations are authoritative for physical column types, defaults, and indexes. When they disagree, document both and flag it.
- CAS2 is partly deprecated/migrating to v2 and CAS3 has `v2/` entities — note version when documenting these.
- Keep the dictionary regenerable: re-running this skill should reproduce the same structure so diffs reflect real schema changes.
