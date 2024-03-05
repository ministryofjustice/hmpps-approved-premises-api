package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentClarificationNoteListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentListener
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
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
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table

@Repository
interface AssessmentRepository : JpaRepository<AssessmentEntity, UUID> {

  /**
   * Note that the logic to determine assessment status is duplicated in
   * [uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated]
   * and as such changes should be synchronized
   */
  @Query(
    value = """
    select distinct cast(a.id as text) as id,
           a.service as type,
           cast(a.application_id as text) as applicationId,
           a.created_at as createdAt,
           CAST(apa.risk_ratings AS TEXT) as riskRatings,
           apa.arrival_date as arrivalDate,
           a.decision is not null as completed,
           a.decision as decision,
           a.allocated_to_user_id is not null as allocated,
           ap.crn as crn,
           apa.name as personName,
           a.due_at as dueAt,
           CASE
             WHEN (a.decision is not null) THEN 'COMPLETED'
             WHEN (open_acn.id IS NOT NULL) THEN 'AWAITING_RESPONSE'
             WHEN (a.data IS NOT NULL) THEN 'IN_PROGRESS'
             ELSE 'NOT_STARTED'
           END as status
      from approved_premises_assessments aa
           join assessments a on aa.assessment_id = a.id
           join applications ap on a.application_id = ap.id
           left outer join approved_premises_applications apa on ap.id = apa.id
           left outer join assessment_clarification_notes open_acn on open_acn.assessment_id = a.id AND open_acn.response IS NULL
     where a.reallocated_at is null
           and a.is_withdrawn is false
           and (?1 is null or a.allocated_to_user_id = cast(?1 as UUID))
           AND ((?2) IS NULL OR 
             (
                CASE 
                  WHEN (a.decision is not null) THEN 'COMPLETED'
                  WHEN (open_acn.id IS NOT NULL) THEN 'AWAITING_RESPONSE'
                  WHEN (a.data IS NOT NULL) THEN 'IN_PROGRESS'
                  ELSE 'NOT_STARTED'
                END
             ) IN (?2)
           ) 
    """,
    countQuery = """
      select count(*) from (
        select distinct a.id
        from approved_premises_assessments aa
             join assessments a on aa.assessment_id = a.id
             join applications ap on a.application_id = ap.id
             left outer join approved_premises_applications apa on ap.id = apa.id
             left outer join assessment_clarification_notes open_acn on open_acn.assessment_id = a.id AND open_acn.response IS NULL
        where a.reallocated_at is null
             and a.is_withdrawn is false
             and (?1 is null or a.allocated_to_user_id = cast(?1 as UUID))
             AND ((?2) IS NULL OR 
               (
                  CASE 
                    WHEN (a.decision is not null) THEN 'COMPLETED'
                    WHEN (open_acn.id IS NOT NULL) THEN 'AWAITING_RESPONSE'
                    WHEN (a.data IS NOT NULL) THEN 'IN_PROGRESS'
                    ELSE 'NOT_STARTED'
                  END
               ) IN (?2)
             ) 
      ) as resultToCount
    """,
    nativeQuery = true,
  )
  fun findAllApprovedPremisesAssessmentSummariesNotReallocated(userIdString: String? = null, statuses: List<String> = emptyList(), page: Pageable? = null): Page<DomainAssessmentSummary>

  @Query(
    value = """
      select a.service as type,
             cast(a.id as text) as id,
             cast(a.application_id as text) as applicationId,
             a.created_at as createdAt,
             CAST(taa.risk_ratings AS TEXT) as riskRatings,
             taa.arrival_date as arrivalDate,
             null as dateOfInfoRequest,
             aa.completed_at is not null as completed,
             a.decision as decision,
             a.data is not null as isStarted,
             a.allocated_to_user_id is not null as allocated,
             ap.crn as crn,
             CASE
                  WHEN a.decision='REJECTED' THEN 'REJECTED'
                  WHEN a.decision='ACCEPTED' AND aa.completed_at is not null THEN 'CLOSED'
                  WHEN a.decision='ACCEPTED' AND aa.completed_at is null THEN 'READY_TO_PLACE'
                  WHEN a.decision is null AND a.allocated_to_user_id is not null THEN 'IN_REVIEW'
                  WHEN a.decision is null AND a.allocated_to_user_id is null THEN 'UNALLOCATED'
             END  as status
        from temporary_accommodation_assessments aa
             join assessments a on aa.assessment_id = a.id
             join applications ap on a.application_id = ap.id
             left outer join temporary_accommodation_applications taa on ap.id = taa.id
       where taa.probation_region_id = ?1
             and (?2 is null OR ap.crn = ?2)
             and a.reallocated_at is null
             and ((?3) is null OR
                                (
                                  CASE
                                    WHEN a.decision='REJECTED' THEN 'REJECTED'
                                    WHEN a.decision='ACCEPTED' AND aa.completed_at is not null THEN 'CLOSED'
                                    WHEN a.decision='ACCEPTED' AND aa.completed_at is null THEN 'READY_TO_PLACE'
                                    WHEN a.decision is null AND a.allocated_to_user_id is not null THEN 'IN_REVIEW'
                                    WHEN a.decision is null AND a.allocated_to_user_id is null THEN 'UNALLOCATED'
                                  END  
                                ) IN (?3)                       
                )
    """,
    countQuery = """
       select count(*)  
          from temporary_accommodation_assessments aa
               join assessments a on aa.assessment_id = a.id
               join applications ap on a.application_id = ap.id
               left outer join temporary_accommodation_applications taa on ap.id = taa.id
         where taa.probation_region_id = ?1
               and (?2 is null OR ap.crn = ?2)
               and a.reallocated_at is null
               and (?3 is null OR
                                (
                                  CASE
                                    WHEN a.decision='REJECTED' THEN 'REJECTED'
                                    WHEN a.decision='ACCEPTED' AND aa.completed_at is not null THEN 'CLOSED'
                                    WHEN a.decision='ACCEPTED' AND aa.completed_at is null THEN 'READY_TO_PLACE'
                                    WHEN a.decision is null AND a.allocated_to_user_id is not null THEN 'IN_REVIEW'
                                    WHEN a.decision is null AND a.allocated_to_user_id is null THEN 'UNALLOCATED'
                                  END  
                                ) IN (?3)                       
                )
    """,
    nativeQuery = true,
  )
  fun findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
    probationRegionId: UUID,
    crn: String?,
    statuses: List<String> = emptyList(),
    page: Pageable? = null,
  ): Page<DomainAssessmentSummary>

  fun findByApplication_IdAndReallocatedAtNull(applicationId: UUID): AssessmentEntity?

  fun findAllByApplicationId(applicationId: UUID): List<AssessmentEntity>

  @Query(
    """
    SELECT
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      apa.is_esap_application as isEsapApplication,
      apa.is_pipe_application as isPipeApplication,
      assessment.decision as decision,
      application.submitted_at as applicationSubmittedAt,
      assessment.submitted_at as assessmentSubmittedAt,
      assessment.rejection_rationale as rejectionRationale,
      apa.release_type as releaseType,
      (
        SELECT
          count(id)
        from
          assessment_clarification_notes
        where
          assessment_id = assessment_id
      ) as clarificationNoteCount
    FROM
      assessments assessment
      left join applications application on application.id = assessment.application_id
      left join approved_premises_applications apa on apa.id = application.id
    WHERE
      date_part('month', assessment.created_at) = :month
      AND date_part('year', assessment.created_at) = :year
      AND assessment.reallocated_at is null
      AND assessment.service = 'approved-premises'
    """,
    nativeQuery = true,
  )
  fun findAllReferralsDataForMonthAndYear(month: Int, year: Int): List<ReferralsDataResult>

  @Query("SELECT a from ApprovedPremisesAssessmentEntity a WHERE a.dueAt IS NULL")
  fun findAllWithNullDueAt(pageable: Pageable?): Slice<ApprovedPremisesAssessmentEntity>

  @Modifying
  @Query("UPDATE ApprovedPremisesAssessmentEntity a SET a.dueAt = :dueAt WHERE a.id = :id")
  fun updateDueAt(id: UUID, dueAt: OffsetDateTime?)
}

@Entity
@Table(name = "assessments")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class AssessmentEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  var allocatedToUser: UserEntity?,

  var allocatedAt: OffsetDateTime?,
  var reallocatedAt: OffsetDateTime?,

  val createdAt: OffsetDateTime,

  var submittedAt: OffsetDateTime?,
  @Enumerated(value = EnumType.STRING)
  var decision: AssessmentDecision?,
  var rejectionRationale: String?,

  @OneToMany(mappedBy = "assessment")
  var clarificationNotes: MutableList<AssessmentClarificationNoteEntity>,

  @OneToMany(mappedBy = "assessment")
  var referralHistoryNotes: MutableList<AssessmentReferralHistoryNoteEntity>,

  @Transient
  var schemaUpToDate: Boolean,

  var isWithdrawn: Boolean,

  var dueAt: OffsetDateTime?,
)

@EntityListeners(AssessmentListener::class)
@Entity
@DiscriminatorValue("approved-premises")
@Table(name = "approved_premises_assessments")
@PrimaryKeyJoinColumn(name = "assessment_id")
class ApprovedPremisesAssessmentEntity(
  id: UUID,
  application: ApplicationEntity,
  data: String?,
  document: String?,
  schemaVersion: JsonSchemaEntity,
  allocatedToUser: UserEntity?,
  allocatedAt: OffsetDateTime?,
  reallocatedAt: OffsetDateTime?,
  createdAt: OffsetDateTime,
  submittedAt: OffsetDateTime?,
  decision: AssessmentDecision?,
  rejectionRationale: String?,
  clarificationNotes: MutableList<AssessmentClarificationNoteEntity>,
  referralHistoryNotes: MutableList<AssessmentReferralHistoryNoteEntity>,
  schemaUpToDate: Boolean,
  isWithdrawn: Boolean,
  dueAt: OffsetDateTime?,
  val createdFromAppeal: Boolean,
) : AssessmentEntity(
  id,
  application,
  data,
  document,
  schemaVersion,
  allocatedToUser,
  allocatedAt,
  reallocatedAt,
  createdAt,
  submittedAt,
  decision,
  rejectionRationale,
  clarificationNotes,
  referralHistoryNotes,
  schemaUpToDate,
  isWithdrawn,
  dueAt,
) {
  fun isPendingAssessment() = this.submittedAt == null && this.reallocatedAt == null && !this.isWithdrawn
}

@Entity
@DiscriminatorValue("temporary-accommodation")
@Table(name = "temporary_accommodation_assessments")
@PrimaryKeyJoinColumn(name = "assessment_id")
class TemporaryAccommodationAssessmentEntity(
  id: UUID,
  application: ApplicationEntity,
  data: String?,
  document: String?,
  schemaVersion: JsonSchemaEntity,
  allocatedToUser: UserEntity?,
  allocatedAt: OffsetDateTime?,
  reallocatedAt: OffsetDateTime?,
  createdAt: OffsetDateTime,
  submittedAt: OffsetDateTime?,
  decision: AssessmentDecision?,
  rejectionRationale: String?,
  clarificationNotes: MutableList<AssessmentClarificationNoteEntity>,
  referralHistoryNotes: MutableList<AssessmentReferralHistoryNoteEntity>,
  schemaUpToDate: Boolean,
  var completedAt: OffsetDateTime?,
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var summaryData: String,
  isWithdrawn: Boolean,
  dueAt: OffsetDateTime?,
) : AssessmentEntity(
  id,
  application,
  data,
  document,
  schemaVersion,
  allocatedToUser,
  allocatedAt,
  reallocatedAt,
  createdAt,
  submittedAt,
  decision,
  rejectionRationale,
  clarificationNotes,
  referralHistoryNotes,
  schemaUpToDate,
  isWithdrawn,
  dueAt,
)

interface DomainAssessmentSummary {
  val type: String
  val id: UUID
  val applicationId: UUID
  val createdAt: Timestamp
  val riskRatings: String?
  val arrivalDate: Timestamp?
  val completed: Boolean
  val allocated: Boolean
  val decision: String?
  val crn: String
  val status: DomainAssessmentSummaryStatus?
  val dueAt: Timestamp?
}

enum class DomainAssessmentSummaryStatus {
  COMPLETED,
  AWAITING_RESPONSE,
  IN_PROGRESS,
  NOT_STARTED,
  REALLOCATED,
  REJECTED,
  CLOSED,
  READY_TO_PLACE,
  IN_REVIEW,
  UNALLOCATED,
}

enum class AssessmentDecision {
  ACCEPTED,
  REJECTED,
}

@Repository
interface AssessmentClarificationNoteRepository : JpaRepository<AssessmentClarificationNoteEntity, UUID> {
  fun findByAssessmentIdAndId(assessmentId: UUID, id: UUID): AssessmentClarificationNoteEntity?
}

@EntityListeners(AssessmentClarificationNoteListener::class)
@Entity
@Table(name = "assessment_clarification_notes")
data class AssessmentClarificationNoteEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  val assessment: AssessmentEntity,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,
  val createdAt: OffsetDateTime,

  val query: String,

  var response: String?,

  var responseReceivedOn: LocalDate?,
)

@Repository
interface AssessmentReferralHistoryNoteRepository : JpaRepository<AssessmentReferralHistoryNoteEntity, UUID>

@Entity
@Table(name = "assessment_referral_history_notes")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class AssessmentReferralHistoryNoteEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  val assessment: AssessmentEntity,

  val createdAt: OffsetDateTime,

  val message: String,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,
)

@Entity
@Table(name = "assessment_referral_history_user_notes")
class AssessmentReferralHistoryUserNoteEntity(
  id: UUID,
  assessment: AssessmentEntity,
  createdAt: OffsetDateTime,
  message: String,
  createdByUser: UserEntity,
) : AssessmentReferralHistoryNoteEntity(
  id,
  assessment,
  createdAt,
  message,
  createdByUser,
)

@Entity
@Table(name = "assessment_referral_history_system_notes")
class AssessmentReferralHistorySystemNoteEntity(
  id: UUID,
  assessment: AssessmentEntity,
  createdAt: OffsetDateTime,
  message: String,
  createdByUser: UserEntity,
  @Enumerated(EnumType.STRING)
  val type: ReferralHistorySystemNoteType,
) : AssessmentReferralHistoryNoteEntity(
  id,
  assessment,
  createdAt,
  message,
  createdByUser,
)

enum class ReferralHistorySystemNoteType {
  SUBMITTED,
  UNALLOCATED,
  IN_REVIEW,
  READY_TO_PLACE,
  REJECTED,
  COMPLETED,
}

interface ReferralsDataResult {
  fun getTier(): String?
  fun getIsEsapApplication(): Boolean?
  fun getIsPipeApplication(): Boolean?
  fun getDecision(): String?
  fun getApplicationSubmittedAt(): Timestamp?
  fun getAssessmentSubmittedAt(): Timestamp?
  fun getRejectionRationale(): String?
  fun getReleaseType(): String?
  fun getClarificationNoteCount(): Int
}
