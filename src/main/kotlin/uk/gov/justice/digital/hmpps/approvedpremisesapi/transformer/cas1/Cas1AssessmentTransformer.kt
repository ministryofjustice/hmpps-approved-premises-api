package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class Cas1AssessmentTransformer(
  private val objectMapper: ObjectMapper,
  private val applicationsTransformer: ApplicationsTransformer,
  private val cas1AssessmentClarificationNoteTransformer: Cas1AssessmentClarificationNoteTransformer,
  private val userTransformer: UserTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
) {

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
    clarificationNotes = jpa.clarificationNotes.map(cas1AssessmentClarificationNoteTransformer::transformJpaToCas1ClarificationNote),
    allocatedToStaffMember = jpa.allocatedToUser?.let {
      userTransformer.transformCas1JpaToApi(it)
    },
    submittedAt = jpa.submittedAt?.toInstant(),
    decision = transformJpaDecisionToApi(jpa.decision),
    rejectionRationale = jpa.rejectionRationale,
    status = getStatusForCas1Assessment(jpa),
    createdFromAppeal = jpa.createdFromAppeal,
  )

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

  fun transformCas1AssessmentStatusToDomainSummaryState(status: Cas1AssessmentStatus) = when (status) {
    Cas1AssessmentStatus.awaitingResponse -> DomainAssessmentSummaryStatus.AWAITING_RESPONSE
    Cas1AssessmentStatus.completed -> DomainAssessmentSummaryStatus.COMPLETED
    Cas1AssessmentStatus.reallocated -> DomainAssessmentSummaryStatus.REALLOCATED
    Cas1AssessmentStatus.inProgress -> DomainAssessmentSummaryStatus.IN_PROGRESS
    Cas1AssessmentStatus.notStarted -> DomainAssessmentSummaryStatus.NOT_STARTED
  }

  fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }

  private fun getStatusForCas1Assessment(entity: AssessmentEntity) = when {
    entity.decision !== null -> Cas1AssessmentStatus.completed
    entity.clarificationNotes.any { it.response == null } -> Cas1AssessmentStatus.awaitingResponse
    entity.reallocatedAt != null -> Cas1AssessmentStatus.reallocated
    entity.data != null -> Cas1AssessmentStatus.inProgress
    else -> Cas1AssessmentStatus.notStarted
  }

  private fun getStatusForCas1Assessment(ase: DomainAssessmentSummary): Cas1AssessmentStatus = when (ase.status) {
    DomainAssessmentSummaryStatus.COMPLETED -> Cas1AssessmentStatus.completed
    DomainAssessmentSummaryStatus.AWAITING_RESPONSE -> Cas1AssessmentStatus.awaitingResponse
    DomainAssessmentSummaryStatus.IN_PROGRESS -> Cas1AssessmentStatus.inProgress
    DomainAssessmentSummaryStatus.REALLOCATED -> Cas1AssessmentStatus.reallocated
    else -> Cas1AssessmentStatus.notStarted
  }

  private fun transformDomainSummaryDecisionToApi(decision: String?) = when (decision) {
    "ACCEPTED" -> ApiAssessmentDecision.accepted
    "REJECTED" -> ApiAssessmentDecision.rejected
    else -> null
  }
}
