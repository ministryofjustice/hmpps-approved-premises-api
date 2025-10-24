package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class Cas3AssessmentTransformer(
  private val applicationsTransformer: ApplicationsTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val userTransformer: UserTransformer,
  private val objectMapper: ObjectMapper,
) {
  fun transformJpaToApi(
    jpa: TemporaryAccommodationAssessmentEntity,
    personInfo: PersonInfoResult,
  ) = Cas3Assessment(
    id = jpa.id,
    application = applicationsTransformer.transformJpaToApi(jpa.application, personInfo) as TemporaryAccommodationApplication,
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
    status = getAssessmentStatus(jpa),
    summaryData = objectMapper.readTree(jpa.summaryData),
    releaseDate = jpa.releaseDate ?: jpa.typedApplication<TemporaryAccommodationApplicationEntity>().personReleaseDate,
    accommodationRequiredFromDate = jpa.accommodationRequiredFromDate ?: LocalDate.from(jpa.typedApplication<TemporaryAccommodationApplicationEntity>().arrivalDate),
  )

  fun transformDomainToApiSummary(ase: DomainAssessmentSummary, personInfo: PersonInfoResult): Cas3AssessmentSummary = Cas3AssessmentSummary(
    id = ase.id,
    applicationId = ase.applicationId,
    createdAt = ase.createdAt,
    arrivalDate = ase.arrivalDate,
    status = getAssessmentStatus(ase),
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

  private fun getAssessmentStatus(ase: DomainAssessmentSummary) = when {
    ase.decision == "REJECTED" -> TemporaryAccommodationAssessmentStatus.rejected
    ase.decision == "ACCEPTED" && ase.completed -> TemporaryAccommodationAssessmentStatus.closed
    ase.decision == "ACCEPTED" -> TemporaryAccommodationAssessmentStatus.readyToPlace
    ase.allocated -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }

  private fun getAssessmentStatus(entity: TemporaryAccommodationAssessmentEntity) = when {
    entity.decision == AssessmentDecision.REJECTED -> TemporaryAccommodationAssessmentStatus.rejected
    entity.decision == AssessmentDecision.ACCEPTED && (entity as TemporaryAccommodationAssessmentEntity).completedAt != null ->
      TemporaryAccommodationAssessmentStatus.closed

    entity.decision == AssessmentDecision.ACCEPTED -> TemporaryAccommodationAssessmentStatus.readyToPlace
    entity.allocatedToUser != null -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }

  private fun transformDomainSummaryDecisionToApi(decision: String?) = when (decision) {
    "ACCEPTED" -> ApiAssessmentDecision.accepted
    "REJECTED" -> ApiAssessmentDecision.rejected
    else -> null
  }

  fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }
}
