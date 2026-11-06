# Data Dictionary — CAS2 (Transitional Accommodation / HDC & Bail)

Generated from JPA entities and Flyway migrations. Entities are the authoritative object
model; migrations are authoritative for physical column types and constraints.

Dictionary data: [cas2.csv](./cas2.csv)

CAS2 entities live in `cas2hdc/jpa/entity/` and `cas2/jpa/entity/`. The service is
mid-migration: `cas_2_users` is the user model

## Entity–Relationship Diagram

![Entity Relationship Diagram](./erd-cas2.mermaid)

_See also: [erd-cas2.mermaid](./erd-cas2.mermaid) (Mermaid source)_

## Tables

Full column reference (same data as the CSV). One table per database table.

### cas_2_application_assignments

Table: `cas_2_application_assignments` | Entity: `Cas2ApplicationAssignmentEntity`

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK |  |  |  |
| `application_id` | uuid | UUID | no | FK |  | ManyToOne → cas_2_applications | FetchType.LAZY |
| `prison_code` | text | String | no |  |  |  |  |
| `allocated_pom_cas_2_user_id` | uuid | UUID? | yes | FK |  | ManyToOne → cas_2_users |  |
| `created_at` | timestamptz | OffsetDateTime | no |  |  |  |  |

### cas_2_application_notes

Table: `cas_2_application_notes` | Entity: `Cas2ApplicationNoteEntity`

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK |  |  |  |
| `created_by_cas2_user_id` | uuid | UUID | no | FK |  | ManyToOne → cas_2_users |  |
| `application_id` | uuid | UUID | no | FK |  | ManyToOne → cas_2_applications |  |
| `created_at` | timestamptz | OffsetDateTime | no |  |  |  |  |
| `body` | text | String | no |  |  |  |  |
| `assessment_id` | uuid | UUID? | yes | FK |  | ManyToOne → cas_2_assessments |  |

### cas_2_applications

Table: `cas_2_applications` | Entity: `Cas2ApplicationEntity`

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK |  |  |  |
| `crn` | text | String | no |  |  |  |  |
| `created_by_cas2_user_id` | uuid | UUID | no | FK |  | ManyToOne → cas_2_users |  |
| `data` | jsonb | String? | yes |  |  |  |  |
| `document` | jsonb | String? | yes |  |  |  |  |
| `created_at` | timestamptz | OffsetDateTime | no |  |  |  |  |
| `submitted_at` | timestamptz | OffsetDateTime? | yes |  |  |  |  |
| `abandoned_at` | timestamptz | OffsetDateTime? | yes |  |  |  |  |
| `noms_number` | text | String? | yes |  |  |  |  |
| `referring_prison_code` | text | String? | yes |  |  |  |  |
| `preferred_areas` | text | String? | yes |  |  |  |  |
| `hdc_eligibility_date` | date | LocalDate? | yes |  |  |  |  |
| `conditional_release_date` | date | LocalDate? | yes |  |  |  |  |
| `telephone_number` | text | String? | yes |  |  |  |  |
| `bail_hearing_date` | date | LocalDate? | yes |  |  |  |  |
| `application_origin` | text | ApplicationOrigin | no |  | courtBail / prisonBail / homeDetentionCurfew / other |  |  |
| `service_origin` | text | Cas2ServiceOrigin | no |  | BAIL / HDC |  |  |
| `cohort` | text | Cas2Cohort? | yes |  | HDC / PRISON_BAIL / COURT_BAIL / ATCR / HCRD / HEFR / ISC / RARR / FROM_AP |  |  |

### cas_2_assessments

Table: `cas_2_assessments` | Entity: `Cas2AssessmentEntity`

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK |  |  |  |
| `application_id` | uuid | UUID | no | FK |  | OneToOne → cas_2_applications | owning side |
| `created_at` | timestamptz | OffsetDateTime | no |  |  |  |  |
| `nacro_referral_id` | text | String? | yes |  |  |  |  |
| `assessor_name` | text | String? | yes |  |  |  |  |
| `service_origin` | text | Cas2ServiceOrigin | no |  | BAIL / HDC |  |  |

### cas_2_status_update_details

Table: `cas_2_status_update_details` | Entity: `Cas2StatusUpdateDetailEntity`

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK |  |  |  |
| `status_detail_id` | uuid | UUID | no |  |  |  |  |
| `label` | text | String | no |  |  |  |  |
| `status_update_id` | uuid | UUID | no | FK |  | ManyToOne → cas_2_status_updates |  |
| `created_at` | timestamptz | OffsetDateTime | no |  |  |  |  |

### cas_2_status_updates

Table: `cas_2_status_updates` | Entity: `Cas2StatusUpdateEntity`

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK |  |  |  |
| `status_id` | uuid | UUID | no |  |  |  |  |
| `description` | text | String | no |  |  |  |  |
| `label` | text | String | no |  |  |  |  |
| `cas2_user_assessor_id` | uuid | UUID | no | FK |  | ManyToOne → cas_2_users |  |
| `application_id` | uuid | UUID | no | FK |  | ManyToOne → cas_2_applications |  |
| `assessment_id` | uuid | UUID? | yes | FK |  | ManyToOne → cas_2_assessments |  |
| `created_at` | timestamptz | OffsetDateTime | no |  |  |  | audit timestamp |

### cas_2_users

Table: `cas_2_users` | Entity: `Cas2UserEntity`

| Column | Type (SQL) | Kotlin | Nullable | Key | Enum values | Relationship | Notes |
|--------|-----------|--------|----------|-----|-------------|--------------|-------|
| `id` | uuid | UUID | no | PK |  |  |  |
| `username` | text | String | no |  |  |  |  |
| `email` | text | String? | yes |  |  |  |  |
| `name` | text | String | no |  |  |  |  |
| `user_type` | text | Cas2UserType | no |  | DELIUS / NOMIS / EXTERNAL |  |  |
| `external_type` | text | String? | yes |  |  |  |  |
| `nomis_staff_id` | bigint | Long? | yes |  |  |  |  |
| `active_nomis_caseload_id` | text | String? | yes |  |  |  | corresponds to prison code |
| `nomis_account_type` | text | String? | yes |  |  |  |  |
| `delius_team_codes` | text | List<String>? | yes |  |  |  | StringListConverter |
| `delius_staff_code` | text | String? | yes |  |  |  |  |
| `is_enabled` | boolean | Boolean | no |  |  |  |  |
| `is_active` | boolean | Boolean | no |  |  |  |  |
| `created_at` | timestamptz | OffsetDateTime | no |  |  |  |  |
| `service_origin` | text | Cas2ServiceOrigin | no |  | BAIL / HDC |  |  |

## Query-backed projections (not physical tables)

These entities are backed by database views or native queries; they are read-only and not physical tables.

### Cas2ApplicationSummary

Native query projection | Interface: `Cas2ApplicationSummary` — [source](../../src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/cas2hdc/jpa/entity/Cas2ApplicationSummaryRepository.kt)

Read-only summary of CAS2 applications returned by the native queries in `Cas2ApplicationSummaryRepository`. Each application in `cas_2_applications` is joined to its latest `cas_2_status_updates` row, its latest `cas_2_application_assignments` row and the relevant `cas_2_users`. Abandoned applications, applications without a status in the HDC/bail cohorts whose conditional release date has passed, and applications whose latest status (withdrawn, cancelled or awaiting arrival) was set 32 or more days ago are excluded.

This replaces the `cas_2_application_summary` and `cas_2_application_live_summary` views. The views are no longer used by the application but are retained so that the previous release keeps working during rollout and rollback; they will be dropped in a follow-up release.

| Property | Source | Kotlin | Nullable |
|----------|--------|--------|----------|
| `id` | `cas_2_applications.id` | UUID | no |
| `crn` | `cas_2_applications.crn` | String | no |
| `nomsNumber` | `cas_2_applications.noms_number` | String? | yes |
| `userId` | `cas_2_applications.created_by_cas2_user_id` | UUID | no |
| `userName` | `cas_2_users.name` (creator) | String | no |
| `allocatedPomUserId` | latest `cas_2_application_assignments.allocated_pom_cas_2_user_id` | UUID? | yes |
| `allocatedPomName` | `cas_2_users.name` (allocated POM) | String? | yes |
| `createdAt` | `cas_2_applications.created_at` | Instant | no |
| `submittedAt` | `cas_2_applications.submitted_at` | Instant? | yes |
| `hdcEligibilityDate` | `cas_2_applications.hdc_eligibility_date` | LocalDate? | yes |
| `latestStatusUpdateLabel` | latest `cas_2_status_updates.label` | String? | yes |
| `latestStatusUpdateStatusId` | latest `cas_2_status_updates.status_id` | UUID? | yes |
| `prisonCode` | `cas_2_applications.referring_prison_code` | String? | yes |
| `currentPrisonCode` | latest `cas_2_application_assignments.prison_code` | String? | yes |
| `assignmentDate` | latest `cas_2_application_assignments.created_at` | Instant? | yes |
| `bailHearingDate` | `cas_2_applications.bail_hearing_date` | LocalDate? | yes |
| `applicationOrigin` | `cas_2_applications.application_origin` | String? | yes |
| `cohort` | `cas_2_applications.cohort` | Cas2Cohort? | yes |

## Sources

| Area | Location |
|------|----------|
| Entity packages | [cas2hdc/jpa/entity/](../../src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/cas2hdc/jpa/entity), [cas2/jpa/entity/](../../src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/cas2/jpa/entity) |
| Migrations | [db/migration/all/](../../src/main/resources/db/migration/all) |

