package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.ApplicationListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Convert
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.LockModeType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table
import javax.persistence.Version
import kotlin.time.Duration.Companion.days
import kotlin.time.toKotlinDuration

@Repository
interface ApplicationRepository : JpaRepository<ApplicationEntity, UUID> {

  @Modifying
  @Query("UPDATE ApprovedPremisesApplicationEntity ap set ap.status = :status where ap.id = :applicationId")
  fun updateStatus(applicationId: UUID, status: ApprovedPremisesApplicationStatus)

  @Query(
    """
SELECT
    CAST(a.id AS TEXT) as id,
    a.crn,
    CAST(a.created_by_user_id AS TEXT) as createdByUserId,
    a.created_at as createdAt,
    a.submitted_at as submittedAt,
    apa.is_womens_application as isWomensApplication,
    (apa.ap_type = 'PIPE') as isPipeApplication,
    apa.arrival_date as arrivalDate,
    CAST(apa.risk_ratings AS TEXT) as riskRatings,
    apa.status as status,
    apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
    apa.is_withdrawn as isWithdrawn,
    apa.release_type as releaseType,
    (
        (select count(1) from placement_applications where application_id = a.id) > 0 OR 
        (select count(1) from placement_requests where application_id = a.id) > 0
    ) as hasRequestsForPlacement
FROM approved_premises_applications apa
LEFT JOIN applications a ON a.id = apa.id
WHERE apa.is_inapplicable IS NOT TRUE 
AND (
      :crnOrName IS NULL OR 
      (
        a.crn = UPPER(:crnOrName) OR UPPER(apa.name) LIKE UPPER('%' || :crnOrName || '%')
      )
)
AND (
    :statusProvided is false OR apa.status IN (:status)
)
AND (
    (CAST(:apAreaId AS uuid) IS NULL) OR (apa.ap_area_id = :apAreaId)
)
AND (
    (:releaseType IS NULL) OR (apa.release_type = :releaseType)
)
""",
    countQuery = """
    SELECT COUNT(*)
      FROM approved_premises_applications apa
      LEFT JOIN applications a ON a.id = apa.id
      WHERE apa.is_inapplicable IS NOT TRUE
      AND (
        :crnOrName IS NULL OR 
        (
          a.crn = UPPER(:crnOrName) OR UPPER(apa.name) LIKE UPPER('%' || :crnOrName || '%')
        )
      )
      AND (
          :statusProvided is false OR apa.status IN (:status)
      )
      AND (
          (CAST(:apAreaId AS uuid) IS NULL) OR (apa.ap_area_id = :apAreaId)
      )
      AND (
          (:releaseType IS NULL) OR (apa.release_type = :releaseType)
      )
    """,
    nativeQuery = true,
  )
  fun findAllApprovedPremisesSummaries(
    pageable: Pageable?,
    crnOrName: String?,
    statusProvided: Boolean,
    status: List<String>,
    apAreaId: UUID?,
    releaseType: String?,
  ): Page<ApprovedPremisesApplicationSummary>

  @Query("SELECT a FROM ApplicationEntity a WHERE TYPE(a) = :type AND a.createdByUser.id = :id")
  fun <T : ApplicationEntity> findAllByCreatedByUser_Id(id: UUID, type: Class<T>): List<ApplicationEntity>

  @Query(
    "SELECT * FROM approved_premises_applications apa " +
      "LEFT JOIN applications a ON a.id = apa.id " +
      "WHERE apa.name IS NULL",
    nativeQuery = true,
  )
  fun <T : ApplicationEntity> findAllForServiceAndNameNull(
    type: Class<T>,
    pageable: Pageable?,
  ): Slice<ApprovedPremisesApplicationEntity>

  @Query(
    "SELECT * FROM approved_premises_applications apa " +
      "LEFT JOIN applications a ON a.id = apa.id " +
      "WHERE apa.status IS NULL",
    nativeQuery = true,
  )
  fun findAllWithNullStatus(pageable: Pageable?): Slice<ApprovedPremisesApplicationEntity>

  @Query(
    "SELECT application.created_at as createdAt, CAST(application.created_by_user_id as TEXT) as createdByUserId FROM approved_premises_applications apa " +
      "LEFT JOIN applications application ON application.id = apa.id " +
      "where date_part('month', application.created_at) = :month " +
      "AND date_part('year', application.created_at) = :year " +
      "AND application.service = 'approved-premises'",
    nativeQuery = true,
  )
  fun findAllApprovedPremisesApplicationsCreatedInMonth(
    month: Int,
    year: Int,
  ): List<ApprovedPremisesApplicationMetricsSummary>

  @Query(
    "SELECT a FROM ApplicationEntity a " +
      "LEFT JOIN ApplicationTeamCodeEntity atc ON a = atc.application " +
      "WHERE TYPE(a) = :type AND atc.teamCode IN (:managingTeamCodes)",
  )
  fun <T : ApplicationEntity> findAllByManagingTeam(
    managingTeamCodes: List<String>,
    type: Class<T>,
  ): List<ApplicationEntity>

  @Query("SELECT a FROM ApplicationEntity a WHERE TYPE(a) = :type AND a.crn = :crn")
  fun <T : ApplicationEntity> findByCrn(crn: String, type: Class<T>): List<ApplicationEntity>

  @Query("SELECT a.assessments FROM ApplicationEntity a WHERE a.id = :applicationId")
  fun findAllAssessmentsById(applicationId: UUID): MutableList<AssessmentEntity>

  @Query("SELECT a FROM ApplicationEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun findByIdOrNullWithWriteLock(id: UUID): ApplicationEntity?

  @Query(
    """
SELECT
    CAST(a.id AS TEXT) as id,
    a.crn,
    CAST(a.created_by_user_id AS TEXT) as createdByUserId,
    a.created_at as createdAt,
    a.submitted_at as submittedAt,
    apa.is_womens_application as isWomensApplication,
    (apa.ap_type = 'PIPE') as isPipeApplication,
    apa.arrival_date as arrivalDate,
    apa.status as status,
    CAST(apa.risk_ratings AS TEXT) as riskRatings,
    apa.is_withdrawn as isWithdrawn,
    (
        (select count(1) from placement_applications where application_id = a.id) > 0 OR 
        (select count(1) from placement_requests where application_id = a.id) > 0
    ) as hasRequestsForPlacement
FROM approved_premises_applications apa
LEFT JOIN applications a ON a.id = apa.id
WHERE a.created_by_user_id = :userId 
AND apa.is_inapplicable IS NOT TRUE 
AND apa.is_withdrawn = FALSE;
""",
    nativeQuery = true,
  )
  fun findNonWithdrawnApprovedPremisesSummariesForUser(userId: UUID): List<ApprovedPremisesApplicationSummary>

  @Query(
    """
SELECT
    CAST(a.id AS TEXT) as id,
    a.crn,
    CAST(a.created_by_user_id AS TEXT) as createdByUserId,
    a.created_at as createdAt,
    a.submitted_at as submittedAt,
    ass.submitted_at as latestAssessmentSubmittedAt,
    ass.decision as latestAssessmentDecision,
    (SELECT COUNT(1) FROM assessment_clarification_notes acn WHERE acn.assessment_id = ass.id AND acn.response IS NULL) > 0 as latestAssessmentHasClarificationNotesWithoutResponse,
    (SELECT COUNT(1) FROM bookings b WHERE b.application_id = taa.id) > 0 as hasBooking,
    CAST(taa.risk_ratings AS TEXT) as riskRatings
FROM temporary_accommodation_applications taa 
LEFT JOIN applications a ON a.id = taa.id 
LEFT JOIN assessments ass ON ass.application_id = taa.id AND ass.reallocated_at IS NULL 
WHERE a.created_by_user_id = :userId
""",
    nativeQuery = true,
  )
  fun findAllTemporaryAccommodationSummariesCreatedByUser(userId: UUID): List<TemporaryAccommodationApplicationSummary>

  @Query(
    """
SELECT
    CAST(a.id AS TEXT) as id,
    a.crn,
    CAST(a.created_by_user_id AS TEXT) as createdByUserId,
    a.created_at as createdAt,
    a.submitted_at as submittedAt,
    ass.submitted_at as latestAssessmentSubmittedAt,
    ass.decision as latestAssessmentDecision,
    (SELECT COUNT(1) FROM assessment_clarification_notes acn WHERE acn.assessment_id = ass.id AND acn.response IS NULL) > 0 as latestAssessmentHasClarificationNotesWithoutResponse,
    (SELECT COUNT(1) FROM bookings b WHERE b.application_id = taa.id) > 0 as hasBooking,
    CAST(taa.risk_ratings AS TEXT) as riskRatings
FROM temporary_accommodation_applications taa 
LEFT JOIN applications a ON a.id = taa.id 
LEFT JOIN assessments ass ON ass.application_id = taa.id AND ass.reallocated_at IS NULL 
WHERE taa.probation_region_id = :probationRegionId AND a.submitted_at IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findAllSubmittedTemporaryAccommodationSummariesByRegion(probationRegionId: UUID): List<TemporaryAccommodationApplicationSummary>

  @Query("SELECT DISTINCT(a.crn) FROM ApplicationEntity a")
  fun getDistinctCrns(): List<String>

  @Query("SELECT DISTINCT(a.nomsNumber) FROM ApplicationEntity a WHERE a.nomsNumber IS NOT NULL")
  fun getDistinctNomsNumbers(): List<String>

  @Query("SELECT ap FROM ApprovedPremisesApplicationEntity ap WHERE ap.submittedAt IS NOT NULL and ap.applicantUserDetails IS NULL")
  fun getSubmittedApprovedPremisesApplicationsWithoutApplicantUserDetails(pageable: Pageable?): Slice<ApprovedPremisesApplicationEntity>

  @Query("SELECT ap FROM ApprovedPremisesApplicationEntity ap WHERE ap.submittedAt IS NOT NULL AND ap.inmateInOutStatusOnSubmission IS NULL")
  fun getSubmittedApprovedPremisesApplicationsWithoutInOutStatus(pageable: Pageable?): Slice<ApprovedPremisesApplicationEntity>

  @Modifying
  @Query("UPDATE ApprovedPremisesApplicationEntity ap set ap.inmateInOutStatusOnSubmission = :inOutStatus where ap.id = :applicationId")
  fun updateInOutStatus(applicationId: UUID, inOutStatus: String)

  @Query("SELECT taa FROM TemporaryAccommodationApplicationEntity taa WHERE taa.id = :id")
  fun findTemporaryAccommodationApplicationById(id: UUID): TemporaryAccommodationApplicationEntity?

  @Query(
    "SELECT * FROM  temporary_accommodation_applications taa " +
      "LEFT JOIN applications a ON a.id = taa.id " +
      "WHERE taa.name IS NULL",
    nativeQuery = true,
  )
  fun <T : ApplicationEntity> findAllTemporaryAccommodationApplicationsAndNameNull(
    type: Class<T>,
    pageable: Pageable?,
  ): Slice<TemporaryAccommodationApplicationEntity>
}

@Entity
@Table(name = "applications")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class ApplicationEntity(
  @Id
  val id: UUID,

  val crn: String,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,
  val createdAt: OffsetDateTime,
  var submittedAt: OffsetDateTime?,

  @Transient
  var schemaUpToDate: Boolean,

  @OneToMany(mappedBy = "application")
  var assessments: MutableList<AssessmentEntity>,

  var nomsNumber: String?,

  @Version
  var version: Long = 1,
) {
  fun getLatestAssessment(): AssessmentEntity? = this.assessments.maxByOrNull { it.createdAt }
  abstract fun getRequiredQualifications(): List<UserQualification>
}

@EntityListeners(ApplicationListener::class)
@Entity
@DiscriminatorValue("approved-premises")
@Table(name = "approved_premises_applications")
@PrimaryKeyJoinColumn(name = "id")
class ApprovedPremisesApplicationEntity(
  id: UUID,
  crn: String,
  createdByUser: UserEntity,
  data: String?,
  document: String?,
  schemaVersion: JsonSchemaEntity,
  createdAt: OffsetDateTime,
  submittedAt: OffsetDateTime?,
  schemaUpToDate: Boolean,
  assessments: MutableList<AssessmentEntity>,
  var isWomensApplication: Boolean?,
  @Deprecated("Use noticeType=emergency instead")
  var isEmergencyApplication: Boolean?,
  @Enumerated(value = EnumType.STRING)
  var apType: ApprovedPremisesType,
  var isInapplicable: Boolean?,
  var isWithdrawn: Boolean,
  var withdrawalReason: String?,
  var otherWithdrawalReason: String?,
  val convictionId: Long,
  val eventNumber: String,
  val offenceId: String,
  nomsNumber: String?,
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  @Convert(disableConversion = true)
  val riskRatings: PersonRisks?,
  @OneToMany(mappedBy = "application")
  val teamCodes: MutableList<ApplicationTeamCodeEntity>,
  @OneToMany(mappedBy = "application")
  var placementRequests: MutableList<PlacementRequestEntity>,
  var releaseType: String?,
  var sentenceType: String?,
  var situation: String?,
  var arrivalDate: OffsetDateTime?,
  var name: String,
  var targetLocation: String?,
  @Enumerated(value = EnumType.STRING)
  var status: ApprovedPremisesApplicationStatus,
  var inmateInOutStatusOnSubmission: String?,
  @ManyToOne
  @JoinColumn(name = "ap_area_id")
  var apArea: ApAreaEntity?,
  @OneToOne
  @JoinColumn(name = "applicant_cas1_application_user_details_id")
  var applicantUserDetails: Cas1ApplicationUserDetailsEntity?,
  var caseManagerIsNotApplicant: Boolean?,
  @OneToOne
  @JoinColumn(name = "case_manager_cas1_application_user_details_id")
  var caseManagerUserDetails: Cas1ApplicationUserDetailsEntity?,
  @Enumerated(value = EnumType.STRING)
  var noticeType: Cas1ApplicationTimelinessCategory?,
) : ApplicationEntity(
  id,
  crn,
  createdByUser,
  data,
  document,
  schemaVersion,
  createdAt,
  submittedAt,
  schemaUpToDate,
  assessments,
  nomsNumber,
) {
  val isPipeApplication: Boolean
    get() = apType == ApprovedPremisesType.PIPE
  val isEsapApplication: Boolean
    get() = apType == ApprovedPremisesType.ESAP

  fun hasTeamCode(code: String) = teamCodes.any { it.teamCode == code }
  fun hasAnyTeamCode(codes: List<String>) = codes.any(::hasTeamCode)
  override fun getRequiredQualifications(): List<UserQualification> {
    val requiredQualifications = mutableListOf<UserQualification>()

    when (apType) {
      ApprovedPremisesType.PIPE -> requiredQualifications += UserQualification.PIPE
      ApprovedPremisesType.ESAP -> requiredQualifications += UserQualification.ESAP
      ApprovedPremisesType.RFAP -> requiredQualifications += UserQualification.RECOVERY_FOCUSED
      ApprovedPremisesType.MHAP_ST_JOSEPHS -> requiredQualifications += UserQualification.MENTAL_HEALTH_SPECIALIST
      ApprovedPremisesType.MHAP_ELLIOTT_HOUSE -> requiredQualifications += UserQualification.MENTAL_HEALTH_SPECIALIST
      else -> {}
    }

    if (isWomensApplication == true) {
      requiredQualifications += UserQualification.WOMENS
    }

    if (noticeType == Cas1ApplicationTimelinessCategory.emergency || noticeType == Cas1ApplicationTimelinessCategory.shortNotice) {
      requiredQualifications += UserQualification.EMERGENCY
    }

    return requiredQualifications
  }

  fun getLatestPlacementRequest(): PlacementRequestEntity? = this.placementRequests.maxByOrNull { it.createdAt }
  fun getLatestBooking(): BookingEntity? = getLatestPlacementRequest()?.booking
  fun isSubmitted() = submittedAt != null

  fun isShortNoticeApplication() = this.arrivalDate?.let { Duration.between(this.createdAt, this.arrivalDate).toKotlinDuration() < 28.days }
}

@Repository
interface ApplicationTeamCodeRepository : JpaRepository<ApplicationTeamCodeEntity, UUID>

@Entity
@Table(name = "approved_premises_application_team_codes")
data class ApplicationTeamCodeEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,
  val teamCode: String,
)

@Entity
@DiscriminatorValue("temporary-accommodation")
@Table(name = "temporary_accommodation_applications")
@PrimaryKeyJoinColumn(name = "id")
class TemporaryAccommodationApplicationEntity(
  id: UUID,
  crn: String,
  createdByUser: UserEntity,
  data: String?,
  document: String?,
  schemaVersion: JsonSchemaEntity,
  createdAt: OffsetDateTime,
  submittedAt: OffsetDateTime?,
  schemaUpToDate: Boolean,
  assessments: MutableList<AssessmentEntity>,
  nomsNumber: String?,
  val convictionId: Long,
  val eventNumber: String,
  val offenceId: String,
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  @Convert(disableConversion = true)
  val riskRatings: PersonRisks?,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  val probationRegion: ProbationRegionEntity,
  var arrivalDate: OffsetDateTime?,
  var isRegisteredSexOffender: Boolean?,
  var isHistoryOfSexualOffence: Boolean?,
  var isConcerningSexualBehaviour: Boolean?,
  var needsAccessibleProperty: Boolean?,
  var hasHistoryOfArson: Boolean?,
  var isConcerningArsonBehaviour: Boolean?,
  var isDutyToReferSubmitted: Boolean?,
  var dutyToReferSubmissionDate: LocalDate?,
  var dutyToReferOutcome: String?,
  var isEligible: Boolean?,
  var eligibilityReason: String?,
  var dutyToReferLocalAuthorityAreaName: String?,
  var prisonNameOnCreation: String?,
  var personReleaseDate: LocalDate?,
  var pdu: String?,
  var name: String?,
) : ApplicationEntity(
  id,
  crn,
  createdByUser,
  data,
  document,
  schemaVersion,
  createdAt,
  submittedAt,
  schemaUpToDate,
  assessments,
  nomsNumber,
) {
  override fun getRequiredQualifications(): List<UserQualification> = emptyList()
}

interface ApplicationSummary {
  fun getId(): UUID
  fun getCrn(): String
  fun getCreatedByUserId(): UUID
  fun getCreatedAt(): Timestamp
  fun getSubmittedAt(): Timestamp?
  fun getRiskRatings(): String?
}

interface ApprovedPremisesApplicationSummary : ApplicationSummary {
  fun getIsWomensApplication(): Boolean?
  fun getIsPipeApplication(): Boolean?
  fun getIsEmergencyApplication(): Boolean?
  fun getIsEsapApplication(): Boolean?
  fun getArrivalDate(): Timestamp?
  fun getTier(): String?
  fun getStatus(): String
  fun getIsWithdrawn(): Boolean
  fun getReleaseType(): String?
  fun getHasRequestsForPlacement(): Boolean
}

interface TemporaryAccommodationApplicationSummary : ApplicationSummary {
  fun getLatestAssessmentSubmittedAt(): Timestamp?
  fun getLatestAssessmentDecision(): AssessmentDecision?
  fun getLatestAssessmentHasClarificationNotesWithoutResponse(): Boolean
  fun getHasBooking(): Boolean
}

interface ApprovedPremisesApplicationMetricsSummary {
  fun getCreatedAt(): Timestamp
  fun getCreatedByUserId(): String
}
