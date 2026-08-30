# Data Dictionary

Generated with the `data-dictionary` skill from the JPA entities in this repository.
Re-run the skill to refresh after schema changes.

The object model is derived from the JPA `@Entity` / `@Table` / `@Column` annotations;
SQL types are inferred from Kotlin types and cross-checked against the Flyway migrations
in `src/main/resources/db/migration/all/`.

## Domains

| Domain | Description | Dictionary (CSV) | Diagram |
| --- | --- | --- | --- |
| CAS1 | Approved Premises | [cas1.csv](cas1.csv) | [cas1.md](cas1.md) |
| CAS2 | Short-Term Accommodation (HDC / Bail) | [cas2.csv](cas2.csv) | [cas2.md](cas2.md) |
| CAS3 | Temporary Accommodation | [cas3.csv](cas3.csv) | [cas3.md](cas3.md) |
| Shared | Common tables used across services | [shared.csv](shared.csv) | [shared.md](shared.md) |

## CSV columns

Each `*.csv` file has one row per column with the following fields:

`domain, table, entity, column, sql_type, kotlin_type, nullable, key, enum_values, relationship, notes`

## Notes

- **JOINED inheritance**: `applications`, `assessments` and `premises` are base tables
  with a `service` discriminator and per-service subtype tables
  (e.g. `approved_premises_applications`, `temporary_accommodation_applications`).
  These are listed under **Shared**.
- **CAS3 reuses shared tables**: the CAS3 booking entities (`Cas3BookingEntity`,
  `Cas3ArrivalEntity`, `Cas3DepartureEntity`, `Cas3CancellationEntity`,
  `Cas3ExtensionEntity`, `Cas3NonArrivalEntity`) map to the same physical tables
  (`bookings`, `arrivals`, `departures`, `cancellations`, `extensions`, `non_arrivals`)
  as the legacy shared entities. CAS3-specific tables use the `cas3_` prefix.
- **Deprecated**: some tables/entities are marked deprecated in the CSV `notes`
  (e.g. `cas1_change_requests`, legacy `nomis_users` / `external_users`,
  and the legacy booking-related shared tables). Prefer the documented replacements.
