package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Joins each application to its latest status update and latest assignment and only returns applications.
 *
 * An application is excluded when one of the following is true
 * 1. The application has been abandoned (i.e. before submission)
 * 2. The application has no status, has a cohort 'HDC', 'PRISON_BAIL' or 'COURT_BAIL' and has a conditional release date < today
 * 3. The application has a status of WITHDRAWN, CANCELLED or AWAITING_ARRIVAL (see [uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus]) set 32 or more days ago
 *
 * Note that 'has no status' in point 2 is mostly likely a bug, because as soon as a status is set it'll become visible again
 *
 * The latest status update/assignment joins deliberately avoid `ORDER BY` (e.g. `LATERAL ... LIMIT 1` or `DISTINCT ON`) because
 * Spring Data appends the [Pageable] sort to an existing `ORDER BY` clause, producing invalid SQL.
 */
private const val CAS2_APPLICATION_SUMMARY_FROM = """
FROM cas_2_applications a
LEFT JOIN cas_2_status_updates latest_status_update
  ON latest_status_update.application_id = a.id
  AND NOT EXISTS (
    SELECT 1
    FROM cas_2_status_updates newer_status_update
    WHERE newer_status_update.application_id = a.id
    AND (
      newer_status_update.created_at > latest_status_update.created_at
      OR (newer_status_update.created_at = latest_status_update.created_at AND newer_status_update.id > latest_status_update.id)
    )
  )
LEFT JOIN cas_2_application_assignments latest_assignment
  ON latest_assignment.application_id = a.id
  AND NOT EXISTS (
    SELECT 1
    FROM cas_2_application_assignments newer_assignment
    WHERE newer_assignment.application_id = a.id
    AND (
      newer_assignment.created_at > latest_assignment.created_at
      OR (newer_assignment.created_at = latest_assignment.created_at AND newer_assignment.id > latest_assignment.id)
    )
  )
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
  OR (
    latest_status_update.status_id IN (
      '004e2419-9614-4c1e-a207-a8418009f23d',
      'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9',
      '89458555-3219-44a2-9584-c4f715d6b565'
    )
    AND latest_status_update.created_at > CURRENT_DATE - INTERVAL '32 days'
  )
  OR latest_status_update.status_id NOT IN (
    '004e2419-9614-4c1e-a207-a8418009f23d',
    'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9',
    '89458555-3219-44a2-9584-c4f715d6b565'
  )
)
AND a.service_origin = :serviceOrigin
"""

private const val CAS2_APPLICATION_SUMMARY_QUERY = """
SELECT
  CAST(a.id AS TEXT) AS id,
  a.crn AS crn,
  a.noms_number AS nomsNumber,
  CAST(a.created_by_cas2_user_id AS TEXT) AS userId,
  created_by_user.name AS userName,
  CAST(latest_assignment.allocated_pom_cas_2_user_id AS TEXT) AS allocatedPomUserId,
  allocated_pom_user.name AS allocatedPomName,
  a.created_at AS createdAt,
  a.submitted_at AS submittedAt,
  a.hdc_eligibility_date AS hdcEligibilityDate,
  latest_status_update.label AS latestStatusUpdateLabel,
  CAST(latest_status_update.status_id AS TEXT) AS latestStatusUpdateStatusId,
  a.referring_prison_code AS prisonCode,
  latest_assignment.prison_code AS currentPrisonCode,
  latest_assignment.created_at AS assignmentDate,
  a.bail_hearing_date AS bailHearingDate,
  a.application_origin AS applicationOrigin,
  a.cohort AS cohort
""" + CAS2_APPLICATION_SUMMARY_FROM

private const val CAS2_APPLICATION_SUMMARY_COUNT_QUERY = "SELECT COUNT(*) " + CAS2_APPLICATION_SUMMARY_FROM

/**
 * Returns summaries of CAS2 applications. See [CAS2_APPLICATION_SUMMARY_FROM] for the definition.
 *
 * Results can be sorted using the [Cas2ApplicationSummary] property names (e.g. `createdAt`, `submittedAt`).
 */
@Repository
interface Cas2ApplicationSummaryRepository : JpaRepository<Cas2ApplicationEntity, UUID> {

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + """
      AND (CAST(:userId AS uuid) IS NULL OR a.created_by_cas2_user_id = :userId)
      AND (:crnOrNomsNumber IS NULL OR a.crn = :crnOrNomsNumber OR a.noms_number = :crnOrNomsNumber)
      AND (:prisonCode IS NULL OR a.referring_prison_code = :prisonCode)
      AND (:applicationOrigin IS NULL OR a.application_origin = :applicationOrigin)
      AND (
        :isSubmittedProvided IS FALSE
        OR (:isSubmitted IS TRUE AND a.submitted_at IS NOT NULL)
        OR (:isSubmitted IS FALSE AND a.submitted_at IS NULL)
      )
    """,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + """
      AND (CAST(:userId AS uuid) IS NULL OR a.created_by_cas2_user_id = :userId)
      AND (:crnOrNomsNumber IS NULL OR a.crn = :crnOrNomsNumber OR a.noms_number = :crnOrNomsNumber)
      AND (:prisonCode IS NULL OR a.referring_prison_code = :prisonCode)
      AND (:applicationOrigin IS NULL OR a.application_origin = :applicationOrigin)
      AND (
        :isSubmittedProvided IS FALSE
        OR (:isSubmitted IS TRUE AND a.submitted_at IS NOT NULL)
        OR (:isSubmitted IS FALSE AND a.submitted_at IS NULL)
      )
    """,
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
    value = CAS2_APPLICATION_SUMMARY_QUERY + """
      AND a.submitted_at IS NULL
      AND a.created_by_cas2_user_id = :userId
    """,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + """
      AND a.submitted_at IS NULL
      AND a.created_by_cas2_user_id = :userId
    """,
    nativeQuery = true,
  )
  fun findInProgressApplications(userId: UUID, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + """
      AND a.submitted_at IS NOT NULL
      AND latest_assignment.allocated_pom_cas_2_user_id = :userId
    """,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + """
      AND a.submitted_at IS NOT NULL
      AND latest_assignment.allocated_pom_cas_2_user_id = :userId
    """,
    nativeQuery = true,
  )
  fun findApplicationsAssignedToUser(userId: UUID, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + """
      AND a.submitted_at IS NOT NULL
      AND latest_assignment.prison_code = :prisonCode
      AND latest_assignment.allocated_pom_cas_2_user_id IS NOT NULL
    """,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + """
      AND a.submitted_at IS NOT NULL
      AND latest_assignment.prison_code = :prisonCode
      AND latest_assignment.allocated_pom_cas_2_user_id IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findAllocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + """
      AND a.submitted_at IS NOT NULL
      AND latest_assignment.prison_code = :prisonCode
      AND latest_assignment.allocated_pom_cas_2_user_id IS NULL
    """,
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + """
      AND a.submitted_at IS NOT NULL
      AND latest_assignment.prison_code = :prisonCode
      AND latest_assignment.allocated_pom_cas_2_user_id IS NULL
    """,
    nativeQuery = true,
  )
  fun findUnallocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + "AND a.id IN (:ids)",
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + "AND a.id IN (:ids)",
    nativeQuery = true,
  )
  fun findAllByServiceOriginAndIdIn(serviceOrigin: String, ids: List<UUID>, pageable: Pageable): Page<Cas2ApplicationSummary>

  @Query(
    value = CAS2_APPLICATION_SUMMARY_QUERY + "AND a.submitted_at IS NOT NULL",
    countQuery = CAS2_APPLICATION_SUMMARY_COUNT_QUERY + "AND a.submitted_at IS NOT NULL",
    nativeQuery = true,
  )
  fun findSubmittedApplications(serviceOrigin: String, pageable: Pageable): Page<Cas2ApplicationSummary>
}

interface Cas2ApplicationSummary {
  val id: UUID
  val crn: String
  val nomsNumber: String?
  val userId: UUID
  val userName: String
  val allocatedPomUserId: UUID?
  val allocatedPomName: String?
  val createdAt: Instant
  val submittedAt: Instant?
  val hdcEligibilityDate: LocalDate?
  val latestStatusUpdateLabel: String?
  val latestStatusUpdateStatusId: UUID?
  val prisonCode: String?
  val currentPrisonCode: String?
  val assignmentDate: Instant?
  val bailHearingDate: LocalDate?
  val applicationOrigin: String?
  val cohort: Cas2Cohort?
}
