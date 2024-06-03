package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OrderBy
import org.hibernate.annotations.Type
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

const val SUBMITTED_APPLICATION_SUMMARY_FIELDS =
  """
    ,
    a.hdc_eligibility_date as hdcEligibilityDate,
    asu.label as latestStatusUpdateLabel,
    CAST(asu.status_id AS TEXT) as latestStatusUpdateStatusId
  """

const val DEFAULT_APPLICATION_SUMMARY_FIELDS =
  """
    CAST(a.id AS TEXT) as id,
    a.crn,
    a.noms_number as nomsNumber,
    CAST(a.created_by_user_id AS TEXT) as createdByUserId,
    nu.name as createdByUserName,
    a.created_at as createdAt,
    a.submitted_at as submittedAt
  """

const val ALL_APPLICATION_SUMMARY_FIELDS =
  DEFAULT_APPLICATION_SUMMARY_FIELDS + SUBMITTED_APPLICATION_SUMMARY_FIELDS

@Suppress("TooManyFunctions")
@Repository
interface Cas2ApplicationRepository : JpaRepository<Cas2ApplicationEntity, UUID> {

  @Query(
    """
SELECT 
    $ALL_APPLICATION_SUMMARY_FIELDS
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id
WHERE a.created_by_user_id = :userId
AND (a.conditional_release_date IS NULL OR a.conditional_release_date >= current_date)
ORDER BY createdAt DESC
""",
    countQuery =
    """
    SELECT COUNT(*)
      FROM cas_2_applications a
    WHERE a.created_by_user_id = :userId
    """,
    nativeQuery = true,
  )
  fun findAllCas2ApplicationSummariesCreatedByUser(userId: UUID, pageable: Pageable?): Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $ALL_APPLICATION_SUMMARY_FIELDS
FROM cas_2_applications a
    LEFT JOIN
        (SELECT DISTINCT ON (application_id) su.application_id, 
          su.label, su.status_id
        FROM cas_2_status_updates su
        ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id
WHERE a.referring_prison_code = :prisonCode
AND (a.conditional_release_date IS NULL OR a.conditional_release_date >= current_date)
ORDER BY createdAt DESC
""",
    countQuery =
    """
    SELECT COUNT(*)
      FROM cas_2_applications a
    WHERE a.referring_prison_code = :prisonCode
    """,
    nativeQuery = true,
  )
  fun findAllCas2ApplicationSummariesByPrison(prisonCode: String, pageable: Pageable?): Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $ALL_APPLICATION_SUMMARY_FIELDS
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id
WHERE a.created_by_user_id = :userId
AND a.submitted_at IS NOT NULL
AND a.conditional_release_date >= current_date
ORDER BY createdAt DESC
""",
    countQuery =
    """
    SELECT COUNT(*)
      FROM cas_2_applications a
    WHERE a.created_by_user_id = :userId
    AND a.submitted_at IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findSubmittedCas2ApplicationSummariesCreatedByUser(userId: UUID, pageable: Pageable?): Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $ALL_APPLICATION_SUMMARY_FIELDS
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id
WHERE a.referring_prison_code = :prisonCode
AND a.submitted_at IS NOT NULL
AND a.conditional_release_date >= current_date
ORDER BY createdAt DESC
""",
    countQuery =
    """
    SELECT COUNT(*)
      FROM cas_2_applications a
    WHERE a.referring_prison_code = :prisonCode
    AND a.submitted_at IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findSubmittedCas2ApplicationSummariesByPrison(prisonCode: String, pageable: Pageable?): Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $DEFAULT_APPLICATION_SUMMARY_FIELDS
FROM cas_2_applications a
JOIN nomis_users nu ON nu.id = a.created_by_user_id
WHERE a.created_by_user_id = :userId
AND a.submitted_at IS NULL
ORDER BY createdAt DESC
""",
    nativeQuery = true,
  )
  fun findUnsubmittedCas2ApplicationSummariesCreatedByUser(userId: UUID, pageable: Pageable?): Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $DEFAULT_APPLICATION_SUMMARY_FIELDS
FROM cas_2_applications a
JOIN nomis_users nu ON nu.id = a.created_by_user_id
WHERE a.referring_prison_code = :prisonCode
AND a.submitted_at IS NULL
ORDER BY createdAt DESC
""",
    nativeQuery = true,
  )
  fun findUnsubmittedCas2ApplicationSummariesByPrison(prisonCode: String, pageable: Pageable?): Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $ALL_APPLICATION_SUMMARY_FIELDS
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id
WHERE a.submitted_at IS NOT NULL
""",
    countQuery =
    """
    SELECT COUNT(*)
      FROM cas_2_applications a
      WHERE a.submitted_at IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findAllSubmittedCas2ApplicationSummaries(pageable: Pageable?): Page<Cas2ApplicationSummary>

  @Query(
    "SELECT a FROM Cas2ApplicationEntity a WHERE a.id = :id AND " +
      "a.submittedAt IS NOT NULL",
  )
  fun findSubmittedApplicationById(id: UUID): Cas2ApplicationEntity?

  @Query("SELECT a FROM Cas2ApplicationEntity a WHERE a.createdByUser.id = :id")
  fun findAllByCreatedByUserId(id: UUID): List<Cas2ApplicationEntity>

  @Query("SELECT a FROM Cas2ApplicationEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun findByIdOrNullWithWriteLock(id: UUID): Cas2ApplicationEntity?

  @Query(
    "SELECT a FROM Cas2ApplicationEntity a WHERE a.submittedAt IS NOT NULL " +
      "AND a NOT IN (SELECT application FROM Cas2AssessmentEntity)",
  )
  fun findAllSubmittedApplicationsWithoutAssessments(): Slice<Cas2ApplicationEntity>
}

@Entity
@Table(name = "cas_2_applications")
data class Cas2ApplicationEntity(
  @Id
  val id: UUID,

  val crn: String,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: NomisUserEntity,

  @Type(JsonType::class)
  var data: String?,

  @Type(JsonType::class)
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,
  val createdAt: OffsetDateTime,
  var submittedAt: OffsetDateTime?,

  @OneToMany(mappedBy = "application")
  @OrderBy(clause = "createdAt DESC")
  var statusUpdates: MutableList<Cas2StatusUpdateEntity>? = null,

  @OneToMany(mappedBy = "application")
  @OrderBy(clause = "createdAt DESC")
  var notes: MutableList<Cas2ApplicationNoteEntity>? = null,

  @OneToOne(mappedBy = "application")
  var assessment: Cas2AssessmentEntity? = null,

  @Transient
  var schemaUpToDate: Boolean,

  var nomsNumber: String?,

  var referringPrisonCode: String? = null,
  var preferredAreas: String? = null,
  var hdcEligibilityDate: LocalDate? = null,
  var conditionalReleaseDate: LocalDate? = null,
  var telephoneNumber: String? = null,
) {
  override fun toString() = "Cas2ApplicationEntity: $id"
}

interface AppSummary {
  fun getId(): UUID
  fun getCrn(): String
  fun getNomsNumber(): String
  fun getCreatedByUserId(): UUID
  fun getCreatedByUserName(): String
  fun getCreatedAt(): Instant
  fun getSubmittedAt(): Instant?
  fun getHdcEligibilityDate(): LocalDate?
  fun getLatestStatusUpdateLabel(): String?
  fun getLatestStatusUpdateStatusId(): UUID?
}

interface Cas2ApplicationSummary : AppSummary
