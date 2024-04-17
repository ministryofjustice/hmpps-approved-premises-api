package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.OrderBy
import org.hibernate.annotations.Type
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.LockModeType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

const val submittedApplicationSummaryFields = ",\n" +
    "    a.hdc_eligibility_date as hdcEligibilityDate,\n" +
    "    a.noms_number as nomsNumber,\n" +
    "    a.hdc_eligibility_date as hdcEligibilityDate,\n" +
  "    asu.label as latestStatusUpdateLabel,\n" +
  "    CAST(asu.status_id AS TEXT) as latestStatusUpdateStatusId"

const val unsubmittedApplicationSummaryFields = "" +
  "    CAST(a.id AS TEXT) as id,\n" +
  "    a.crn,\n" +
  "    CAST(a.created_by_user_id AS TEXT) as createdByUserId,\n" +
  "    a.created_at as createdAt,\n" +
  "    a.submitted_at as submittedAt"

const val submittedAndUnsubmittedApplicationSummaryFields =
  unsubmittedApplicationSummaryFields + submittedApplicationSummaryFields

@Suppress("TooManyFunctions")
@Repository
interface Cas2ApplicationRepository : JpaRepository<Cas2ApplicationEntity, UUID> {

  @Query(
    """
SELECT 
    $submittedAndUnsubmittedApplicationSummaryFields
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
WHERE a.created_by_user_id = :userId
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
  fun findAllCas2ApplicationSummariesCreatedByUser(userId: UUID, pageable: Pageable?):
    Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $submittedAndUnsubmittedApplicationSummaryFields
FROM cas_2_applications a
    LEFT JOIN
        (SELECT DISTINCT ON (application_id) su.application_id, 
          su.label, su.status_id
        FROM cas_2_status_updates su
        ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
WHERE a.referring_prison_code = :prisonCode
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
  fun findAllCas2ApplicationSummariesByPrison(prisonCode: String, pageable: Pageable?):
    Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $submittedAndUnsubmittedApplicationSummaryFields
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
WHERE a.created_by_user_id = :userId
AND a.submitted_at IS NOT NULL
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
  fun findSubmittedCas2ApplicationSummariesCreatedByUser(userId: UUID, pageable: Pageable?):
    Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $submittedAndUnsubmittedApplicationSummaryFields
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
WHERE a.referring_prison_code = :prisonCode
AND a.submitted_at IS NOT NULL
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
  fun findSubmittedCas2ApplicationSummariesByPrison(prisonCode: String, pageable: Pageable?):
    Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $unsubmittedApplicationSummaryFields
FROM cas_2_applications a
WHERE a.created_by_user_id = :userId
AND a.submitted_at IS NULL
ORDER BY createdAt DESC
""",
    nativeQuery = true,
  )
  fun findUnsubmittedCas2ApplicationSummariesCreatedByUser(userId: UUID, pageable: Pageable?):
    Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $unsubmittedApplicationSummaryFields
FROM cas_2_applications a
WHERE a.referring_prison_code = :prisonCode
AND a.submitted_at IS NULL
ORDER BY createdAt DESC
""",
    nativeQuery = true,
  )
  fun findUnsubmittedCas2ApplicationSummariesByPrison(prisonCode: String, pageable: Pageable?):
    Page<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    $submittedAndUnsubmittedApplicationSummaryFields
FROM cas_2_applications a
LEFT JOIN
    (SELECT DISTINCT ON (application_id) su.application_id, 
      su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
ON a.id = asu.application_id
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
  fun findAllByCreatedByUser_Id(id: UUID): List<Cas2ApplicationEntity>

  @Query("SELECT a FROM Cas2ApplicationEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun findByIdOrNullWithWriteLock(id: UUID): Cas2ApplicationEntity?

  @Query(
    "SELECT a FROM Cas2ApplicationEntity a WHERE a.submittedAt IS NOT NULL " +
      "AND a.id NOT IN (SELECT application FROM Cas2AssessmentEntity)",
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

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
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
  fun getCreatedAt(): Timestamp
  fun getSubmittedAt(): Timestamp?
  fun getHdcEligibilityDate(): LocalDate?
  fun getLatestStatusUpdateLabel(): String?
  fun getLatestStatusUpdateStatusId(): UUID?
}

interface Cas2ApplicationSummary : AppSummary
