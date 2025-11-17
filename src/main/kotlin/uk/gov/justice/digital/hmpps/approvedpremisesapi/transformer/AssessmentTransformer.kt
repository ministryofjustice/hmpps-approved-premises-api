package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class AssessmentTransformer(
  private val objectMapper: ObjectMapper,
  private val assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val userService: UserService,
) {
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

  private fun transformDomainSummaryDecisionToApi(decision: String?) = when (decision) {
    "ACCEPTED" -> ApiAssessmentDecision.accepted
    "REJECTED" -> ApiAssessmentDecision.rejected
    else -> null
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

  private fun getStatusForTemporaryAccommodationAssessment(ase: DomainAssessmentSummary) = when {
    ase.decision == "REJECTED" -> TemporaryAccommodationAssessmentStatus.rejected
    ase.decision == "ACCEPTED" && ase.completed -> TemporaryAccommodationAssessmentStatus.closed
    ase.decision == "ACCEPTED" -> TemporaryAccommodationAssessmentStatus.readyToPlace
    ase.allocated -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }
}
