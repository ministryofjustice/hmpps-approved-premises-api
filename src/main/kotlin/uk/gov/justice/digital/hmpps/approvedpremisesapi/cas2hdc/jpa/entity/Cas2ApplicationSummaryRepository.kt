package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Common table expressions shared by the summary and count queries:
 *
 * - `latest_status_updates`: the most recent status update for each application (ties on `created_at` are broken by the highest id),
 *   flagged with whether it is a closed status, i.e. WITHDRAWN, CANCELLED or AWAITING_ARRIVAL
 *   (see [uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus])
 * - `latest_assignments`: the most recent assignment for each application (ties on `created_at` are broken by the highest id)
 *
 * The CTEs deliberately avoid `ORDER BY` (e.g. `DISTINCT ON` or `ROW_NUMBER() OVER (... ORDER BY ...)`) because Spring Data
 * appends the [Pageable] sort to an existing `ORDER BY` clause, producing invalid SQL.
 */
private const val CAS2_APPLICATION_SUMMARY_CTES = """
WITH latest_status_updates AS (
  SELECT
    status_update.*,
    status_update.status_id IN (
      '004e2419-9614-4c1e-a207-a8418009f23d',
      'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9',
      '89458555-3219-44a2-9584-c4f715d6b565'
    ) AS is_closed_status
  FROM cas_2_status_updates status_update
  WHERE NOT EXISTS (
    SELECT 1
    FROM cas_2_status_updates newer_status_update
    WHERE newer_status_update.application_id = status_update.application_id
    AND (
      newer_status_update.created_at > status_update.created_at
      OR (newer_status_update.created_at = status_update.created_at AND newer_status_update.id > status_update.id)
    )
  )
),
latest_assignments AS (
  SELECT assignment.*
  FROM cas_2_application_assignments assignment
  WHERE NOT EXISTS (
    SELECT 1
    FROM cas_2_application_assignments newer_assignment
    WHERE newer_assignment.application_id = assignment.application_id
    AND (
      newer_assignment.created_at > assignment.created_at
      OR (newer_assignment.created_at = assignment.created_at AND newer_assignment.id > assignment.id)
    )
  )
)
"""

/**
 * `application_summaries`: joins each application to its latest status update and latest assignment
 * (see [CAS2_APPLICATION_SUMMARY_CTES]), projecting the [Cas2ApplicationSummary] properties.
 *
 * An application is excluded when one of the following is true
 * 1. The application has been abandoned (i.e. before submission)
 * 2. The application has no status, has a cohort 'HDC', 'PRISON_BAIL' or 'COURT_BAIL' and has a conditional release date < today
 * 3. The application has a closed status set 32 or more days ago
 *
 * Note that 'has no status' in point 2 is mostly likely a bug, because as soon as a status is set it'll become visible again
 *
 * Postgres inlines this CTE (it is referenced once), so filters applied to `application_summaries` are pushed down onto the underlying tables.
 */
private const val CAS2_APPLICATION_SUMMARIES_CTE = CAS2_APPLICATION_SUMMARY_CTES + """,
application_summaries AS (
  SELECT
    a.id AS id,
    a.crn AS crn,
    a.noms_number AS nomsNumber,
    a.created_by_cas2_user_id AS userId,
    created_by_user.name AS userName,
    latest_assignment.allocated_pom_cas_2_user_id AS allocatedPomUserId,
    allocated_pom_user.name AS allocatedPomName,
    a.created_at AS createdAt,
    a.submitted_at AS submittedAt,
    a.hdc_eligibility_date AS hdcEligibilityDate,
    latest_status_update.label AS latestStatusUpdateLabel,
    latest_status_update.status_id AS latestStatusUpdateStatusId,
    a.referring_prison_code AS prisonCode,
    latest_assignment.prison_code AS currentPrisonCode,
    latest_assignment.created_at AS assignmentDate,
    a.bail_hearing_date AS bailHearingDate,
    a.application_origin AS applicationOrigin,
    a.cohort AS cohort
  FROM cas_2_applications a
  LEFT JOIN latest_status_updates latest_status_update ON latest_status_update.application_id = a.id
  LEFT JOIN latest_assignments latest_assignment ON latest_assignment.application_id = a.id
  LEFT JOIN cas_2_users created_by_user ON created_by_user.id = a.created_by_cas2_user_id
  LEFT JOIN cas_2_users allocated_pom_user ON allocated_pom_user.id = latest_assignment.allocated_pom_cas_2_user_id
  WHERE (
    (
      latest_status_update.status_id IS NULL
      AND a.abandoned_at IS NULL
      AND (
        a.conditional_release_date IS NULL
        OR a.conditional_release_date >= CURRENT_DATE
        OR a.cohort IS NULL
        OR a.cohort NOT IN ('HDC', 'PRISON_BAIL', 'COURT_BAIL')
      )
    )
    OR NOT latest_status_update.is_closed_status
    OR (latest_status_update.is_closed_status AND latest_status_update.created_at > CURRENT_DATE - INTERVAL '32 days')
  )
  AND a.service_origin = :serviceOrigin
)
"""

private const val CAS2_APPLICATION_SUMMARY_QUERY = CAS2_APPLICATION_SUMMARIES_CTE + """
SELECT application_summaries.* FROM application_summaries WHERE
"""

private const val CAS2_APPLICATION_SUMMARY_COUNT_QUERY = CAS2_APPLICATION_SUMMARIES_CTE + """
SELECT COUNT(*) FROM application_summaries WHERE
"""

private const val SEARCH_FILTER = """
  (CAST(:userId AS uuid) IS NULL OR application_summaries.userId = :userId)
  AND (
    :crnOrNomsNumber IS NULL
    OR application_summaries.crn = :crnOrNomsNumber
    OR application_summaries.nomsNumber = :crnOrNomsNumber
  )
  AND (:prisonCode IS NULL OR application_summaries.prisonCode = :prisonCode)
  AND (:applicationOrigin IS NULL OR application_summaries.applicationOrigin = :applicationOrigin)
  AND (
    :isSubmittedProvided IS FALSE
    OR (:isSubmitted IS TRUE AND application_summaries.submittedAt IS NOT NULL)
    OR (:isSubmitted IS FALSE AND application_summaries.submittedAt IS NULL)
  )
"""

private const val IN_PROGRESS_FILTER = """
  application_summaries.submittedAt IS NULL
  AND application_summaries.userId = :userId
"""

private const val ASSIGNED_TO_USER_FILTER = """
  application_summaries.submittedAt IS NOT NULL
  AND application_summaries.allocatedPomUserId = :userId
"""

private const val ALLOCATED_IN_PRISON_FILTER = """
  application_summaries.submittedAt IS NOT NULL
  AND application_summaries.currentPrisonCode = :prisonCode
  AND application_summaries.allocatedPomUserId IS NOT NULL
"""

private const val UNALLOCATED_IN_PRISON_FILTER = """
  application_summaries.submittedAt IS NOT NULL
  AND application_summaries.currentPrisonCode = :prisonCode
  AND application_summaries.allocatedPomUserId IS NULL
"""

private const val SUBMITTED_FILTER = "application_summaries.submittedAt IS NOT NULL"

/**
 * Returns summaries of CAS2 applications. See [CAS2_APPLICATION_SUMMARIES_CTE] for the definition.
 *
 * Results can be sorted using the [Cas2ApplicationSummary] property names (e.g. `createdAt`, `submittedAt`).
 */
@Repository
interface Cas2ApplicationSummaryRepository : JpaRepository<Cas2ApplicationEntity, UUID> {

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + SEARCH_FILTER,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + SEARCH_FILTER,
    nativeQuery = true,
  )
  @Suppress("LongParameterList")
  fun findCas2ApplicationSummaries(
    serviceOrigin: String,
    userId: UUID?,
    crnOrNomsNumber: String?,
    prisonCode: String?,
    applicationOrigin: String?,
    isSubmittedProvided: Boolean,
    isSubmitted: Boolean,
    pageable: Pageable,
  ): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + IN_PROGRESS_FILTER,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + IN_PROGRESS_FILTER,
    nativeQuery = true,
  )
  fun findInProgressApplications(userId: UUID, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + ASSIGNED_TO_USER_FILTER,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + ASSIGNED_TO_USER_FILTER,
    nativeQuery = true,
  )
  fun findApplicationsAssignedToUser(userId: UUID, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + ALLOCATED_IN_PRISON_FILTER,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + ALLOCATED_IN_PRISON_FILTER,
    nativeQuery = true,
  )
  fun findAllocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + UNALLOCATED_IN_PRISON_FILTER,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + UNALLOCATED_IN_PRISON_FILTER,
    nativeQuery = true,
  )
  fun findUnallocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + "application_summaries.id IN (:ids)",
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + "application_summaries.id IN (:ids)",
    nativeQuery = true,
  )
  fun findAllByServiceOriginAndIdIn(serviceOrigin: String, ids: List<UUID>, pageable: Pageable): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + SUBMITTED_FILTER,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + SUBMITTED_FILTER,
    nativeQuery = true,
  )
  fun findSubmittedApplications(serviceOrigin: String, pageable: Pageable): Page<Cas2ApplicationSummary>
}
