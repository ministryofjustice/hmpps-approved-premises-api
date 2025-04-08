package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class AssessmentTransformer(
  private val objectMapper: ObjectMapper,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer,
  private val userTransformer: UserTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val userService: UserService,
) {
  @Deprecated("This will be removed shortly. Please use the CAS-specific version instead.")
  fun transformJpaToApi(
    jpa: AssessmentEntity,
    personInfo: PersonInfoResult,
  ) = when (jpa) {
    is ApprovedPremisesAssessmentEntity -> ApprovedPremisesAssessment(
      id = jpa.id,
      application = applicationsTransformer.transformJpaToApi(
        jpa.application,
        personInfo,
      ) as ApprovedPremisesApplication,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      allocatedAt = jpa.allocatedAt?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
      allocatedToStaffMember = jpa.allocatedToUser?.let {
        userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) as ApprovedPremisesUser
      },
      submittedAt = jpa.submittedAt?.toInstant(),
      decision = transformJpaDecisionToApi(jpa.decision),
      rejectionRationale = jpa.rejectionRationale,
      status = getStatusForApprovedPremisesAssessment(jpa),
      service = "CAS1",
      createdFromAppeal = jpa.createdFromAppeal,
    )

    is TemporaryAccommodationAssessmentEntity -> {
      val application = applicationsTransformer.transformJpaToApi(
        jpa.application,
        personInfo,
      ) as TemporaryAccommodationApplication

      TemporaryAccommodationAssessment(
        id = jpa.id,
        application = application,
        schemaVersion = jpa.schemaVersion.id,
        outdatedSchema = jpa.schemaUpToDate,
        createdAt = jpa.createdAt.toInstant(),
        allocatedAt = jpa.allocatedAt?.toInstant(),
        data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
        clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
        allocatedToStaffMember = jpa.allocatedToUser?.let {
          userTransformer.transformJpaToApi(
            it,
            ServiceName.temporaryAccommodation,
          ) as TemporaryAccommodationUser
        },
        submittedAt = jpa.submittedAt?.toInstant(),
        decision = transformJpaDecisionToApi(jpa.decision),
        rejectionRationale = jpa.rejectionRationale,
        status = getStatusForTemporaryAccommodationAssessment(jpa),
        summaryData = objectMapper.readTree(jpa.summaryData),
        service = "CAS3",
        releaseDate = jpa.releaseDate ?: jpa.typedApplication<TemporaryAccommodationApplicationEntity>().personReleaseDate,
        accommodationRequiredFromDate = jpa.accommodationRequiredFromDate ?: LocalDate.from(jpa.typedApplication<TemporaryAccommodationApplicationEntity>().arrivalDate),
      )
    }

    else -> throw RuntimeException("Unsupported Application type when transforming Assessment: ${jpa.application::class.qualifiedName}")
  }

  fun getSortedReferralHistoryNotes(
    assessment: TemporaryAccommodationAssessmentEntity,
    cas3Events: List<DomainEventEntity>,
    includeUserNotes: Boolean = true,
  ): List<ReferralHistoryNote> {
    val lastReferralRejectedHistoryNote =
      assessment.referralHistoryNotes.filter { it is AssessmentReferralHistorySystemNoteEntity && it.type == ReferralHistorySystemNoteType.REJECTED }
        .maxByOrNull { it.createdAt }

    val notes = assessment.referralHistoryNotes.map {
      if (it.id == lastReferralRejectedHistoryNote?.id) {
        assessmentReferralHistoryNoteTransformer.transformJpaToApi(it, assessment, includeUserNotes)
      } else {
        assessmentReferralHistoryNoteTransformer.transformJpaToApi(it)
      }
    }
      .filter { includeUserNotes || it.type != "user" }
      .toMutableList()

    notes.addAll(
      cas3Events.map {
        val user = userService.findByIdOrNull(it.triggeredByUserId!!)!!
        assessmentReferralHistoryNoteTransformer.transformToReferralHistoryDomainEventNote(it, user)
      },
    )
    return notes.sortedByDescending { it.createdAt }
  }

  fun transformDomainToApiSummary(ase: DomainAssessmentSummary, personInfo: PersonInfoResult): AssessmentSummary = when (ase.type) {
    "approved-premises" -> ApprovedPremisesAssessmentSummary(
      type = "CAS1",
      id = ase.id,
      applicationId = ase.applicationId,
      createdAt = ase.createdAt,
      arrivalDate = ase.arrivalDate,
      status = getStatusForApprovedPremisesAssessment(ase),
      decision = transformDomainSummaryDecisionToApi(ase.decision),
      risks = ase.riskRatings?.let {
        risksTransformer.transformDomainToApi(
          objectMapper.readValue<PersonRisks>(it),
          ase.crn,
        )
      },
      person = personTransformer.transformModelToPersonApi(personInfo),
      dueAt = ase.dueAt!!,
    )

    "temporary-accommodation" -> TemporaryAccommodationAssessmentSummary(
      type = "CAS3",
      id = ase.id,
      applicationId = ase.applicationId,
      createdAt = ase.createdAt,
      arrivalDate = ase.arrivalDate,
      status = getStatusForTemporaryAccommodationAssessment(ase),
      decision = transformDomainSummaryDecisionToApi(ase.decision),
      risks = ase.riskRatings?.let {
        risksTransformer.transformDomainToApi(
          objectMapper.readValue<PersonRisks>(it),
          ase.crn,
        )
      },
      person = personTransformer.transformModelToPersonApi(personInfo),
      probationDeliveryUnitName = ase.probationDeliveryUnitName,
    )

    else -> throw RuntimeException("Unsupported type: ${ase.type}")
  }

  fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }

  fun transformApiStatusToDomainSummaryState(status: AssessmentStatus) = when (status) {
    AssessmentStatus.cas1Completed -> DomainAssessmentSummaryStatus.COMPLETED
    AssessmentStatus.cas1AwaitingResponse -> DomainAssessmentSummaryStatus.AWAITING_RESPONSE
    AssessmentStatus.cas1InProgress -> DomainAssessmentSummaryStatus.IN_PROGRESS
    AssessmentStatus.cas1NotStarted -> DomainAssessmentSummaryStatus.NOT_STARTED
    AssessmentStatus.cas1Reallocated -> DomainAssessmentSummaryStatus.REALLOCATED
    AssessmentStatus.cas3InReview -> DomainAssessmentSummaryStatus.IN_REVIEW
    AssessmentStatus.cas3Unallocated -> DomainAssessmentSummaryStatus.UNALLOCATED
    AssessmentStatus.cas3Rejected -> DomainAssessmentSummaryStatus.REJECTED
    AssessmentStatus.cas3Closed -> DomainAssessmentSummaryStatus.CLOSED
    AssessmentStatus.cas3ReadyToPlace -> DomainAssessmentSummaryStatus.READY_TO_PLACE
  }

  fun transformCas1AssessmentStatusToDomainSummaryState(status: Cas1AssessmentStatus) = when (status) {
    Cas1AssessmentStatus.awaitingResponse -> DomainAssessmentSummaryStatus.AWAITING_RESPONSE
    Cas1AssessmentStatus.completed -> DomainAssessmentSummaryStatus.COMPLETED
    Cas1AssessmentStatus.reallocated -> DomainAssessmentSummaryStatus.REALLOCATED
    Cas1AssessmentStatus.inProgress -> DomainAssessmentSummaryStatus.IN_PROGRESS
    Cas1AssessmentStatus.notStarted -> DomainAssessmentSummaryStatus.NOT_STARTED
  }

  fun transformDomainToCas1AssessmentSummary(ase: DomainAssessmentSummary, personInfo: PersonInfoResult): Cas1AssessmentSummary = Cas1AssessmentSummary(
    id = ase.id,
    applicationId = ase.applicationId,
    createdAt = ase.createdAt,
    arrivalDate = ase.arrivalDate,
    status = getStatusForCas1Assessment(ase),
    decision = transformDomainSummaryDecisionToApi(ase.decision),
    risks = ase.riskRatings?.let {
      risksTransformer.transformDomainToApi(
        objectMapper.readValue<PersonRisks>(it),
        ase.crn,
      )
    },
    person = personTransformer.transformModelToPersonApi(personInfo),
    dueAt = ase.dueAt!!,
  )

  fun transformJpaToCas1Assessment(
    jpa: ApprovedPremisesAssessmentEntity,
    personInfo: PersonInfoResult,
  ): Cas1Assessment = Cas1Assessment(
    id = jpa.id,
    application = applicationsTransformer.transformJpaToCas1Application(
      jpa.application as ApprovedPremisesApplicationEntity,
      personInfo,
    ),
    schemaVersion = jpa.schemaVersion.id,
    outdatedSchema = jpa.schemaUpToDate,
    createdAt = jpa.createdAt.toInstant(),
    allocatedAt = jpa.allocatedAt?.toInstant(),
    data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
    document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
    clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
    allocatedToStaffMember = jpa.allocatedToUser?.let {
      userTransformer.transformCas1JpaToApi(it)
    },
    submittedAt = jpa.submittedAt?.toInstant(),
    decision = transformJpaDecisionToApi(jpa.decision),
    rejectionRationale = jpa.rejectionRationale,
    status = getStatusForCas1Assessment(jpa),
    createdFromAppeal = jpa.createdFromAppeal,
  )

  private fun transformDomainSummaryDecisionToApi(decision: String?) = when (decision) {
    "ACCEPTED" -> ApiAssessmentDecision.accepted
    "REJECTED" -> ApiAssessmentDecision.rejected
    else -> null
  }

  /**
   * Note that the logic to determine assessment status is duplicated in
   * [uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated]
   * and as such changes should be synchronized
   */
  private fun getStatusForApprovedPremisesAssessment(entity: AssessmentEntity) = when {
    entity.decision !== null -> ApprovedPremisesAssessmentStatus.completed
    entity.clarificationNotes.any { it.response == null } -> ApprovedPremisesAssessmentStatus.awaitingResponse
    entity.reallocatedAt != null -> ApprovedPremisesAssessmentStatus.reallocated
    entity.data != null -> ApprovedPremisesAssessmentStatus.inProgress
    else -> ApprovedPremisesAssessmentStatus.notStarted
  }

  private fun getStatusForApprovedPremisesAssessment(ase: DomainAssessmentSummary): ApprovedPremisesAssessmentStatus = when (ase.status) {
    DomainAssessmentSummaryStatus.COMPLETED -> ApprovedPremisesAssessmentStatus.completed
    DomainAssessmentSummaryStatus.AWAITING_RESPONSE -> ApprovedPremisesAssessmentStatus.awaitingResponse
    DomainAssessmentSummaryStatus.IN_PROGRESS -> ApprovedPremisesAssessmentStatus.inProgress
    DomainAssessmentSummaryStatus.REALLOCATED -> ApprovedPremisesAssessmentStatus.reallocated
    else -> ApprovedPremisesAssessmentStatus.notStarted
  }

  private fun getStatusForCas1Assessment(ase: DomainAssessmentSummary): Cas1AssessmentStatus = when (ase.status) {
    DomainAssessmentSummaryStatus.COMPLETED -> Cas1AssessmentStatus.completed
    DomainAssessmentSummaryStatus.AWAITING_RESPONSE -> Cas1AssessmentStatus.awaitingResponse
    DomainAssessmentSummaryStatus.IN_PROGRESS -> Cas1AssessmentStatus.inProgress
    DomainAssessmentSummaryStatus.REALLOCATED -> Cas1AssessmentStatus.reallocated
    else -> Cas1AssessmentStatus.notStarted
  }

  private fun getStatusForCas1Assessment(entity: AssessmentEntity) = when {
    entity.decision !== null -> Cas1AssessmentStatus.completed
    entity.clarificationNotes.any { it.response == null } -> Cas1AssessmentStatus.awaitingResponse
    entity.reallocatedAt != null -> Cas1AssessmentStatus.reallocated
    entity.data != null -> Cas1AssessmentStatus.inProgress
    else -> Cas1AssessmentStatus.notStarted
  }

  private fun getStatusForTemporaryAccommodationAssessment(entity: AssessmentEntity) = when {
    entity.decision == AssessmentDecision.REJECTED -> TemporaryAccommodationAssessmentStatus.rejected
    entity.decision == AssessmentDecision.ACCEPTED && (entity as TemporaryAccommodationAssessmentEntity).completedAt != null ->
      TemporaryAccommodationAssessmentStatus.closed

    entity.decision == AssessmentDecision.ACCEPTED -> TemporaryAccommodationAssessmentStatus.readyToPlace
    entity.allocatedToUser != null -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }

  private fun getStatusForTemporaryAccommodationAssessment(ase: DomainAssessmentSummary) = when {
    ase.decision == "REJECTED" -> TemporaryAccommodationAssessmentStatus.rejected
    ase.decision == "ACCEPTED" && ase.completed -> TemporaryAccommodationAssessmentStatus.closed
    ase.decision == "ACCEPTED" -> TemporaryAccommodationAssessmentStatus.readyToPlace
    ase.allocated -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }
}
