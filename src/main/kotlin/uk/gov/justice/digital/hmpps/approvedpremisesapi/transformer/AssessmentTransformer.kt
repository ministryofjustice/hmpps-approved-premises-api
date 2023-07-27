package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class AssessmentTransformer(
  private val objectMapper: ObjectMapper,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val userTransformer: UserTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
) {
  fun transformJpaToApi(jpa: AssessmentEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail?) = when (jpa.application) {
    is ApprovedPremisesApplicationEntity -> ApprovedPremisesAssessment(
      id = jpa.id,
      application = applicationsTransformer.transformJpaToApi(jpa.application, offenderDetailSummary, inmateDetail) as ApprovedPremisesApplication,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      allocatedAt = jpa.allocatedAt!!.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
      allocatedToStaffMember = userTransformer.transformJpaToApi(jpa.allocatedToUser!!, ServiceName.approvedPremises) as ApprovedPremisesUser,
      submittedAt = jpa.submittedAt?.toInstant(),
      decision = transformJpaDecisionToApi(jpa.decision),
      rejectionRationale = jpa.rejectionRationale,
      status = getStatus(jpa),
      service = "CAS1",
    )

    is TemporaryAccommodationApplicationEntity -> TemporaryAccommodationAssessment(
      id = jpa.id,
      application = applicationsTransformer.transformJpaToApi(jpa.application, offenderDetailSummary, inmateDetail) as TemporaryAccommodationApplication,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      allocatedAt = jpa.allocatedAt?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
      allocatedToStaffMember = jpa.allocatedToUser?.let { userTransformer.transformJpaToApi(it, ServiceName.temporaryAccommodation) as TemporaryAccommodationUser },
      submittedAt = jpa.submittedAt?.toInstant(),
      decision = transformJpaDecisionToApi(jpa.decision),
      rejectionRationale = jpa.rejectionRationale,
      status = getStatus(jpa),
      service = "CAS3",
    )

    else -> throw RuntimeException("Unsupported Application type when transforming Assessment: ${jpa.application::class.qualifiedName}")
  }

  fun transformDomainToApiSummary(ase: DomainAssessmentSummary, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail?): AssessmentSummary =
    AssessmentSummary(
      type = when (ase.type) {
        "approved-premises" -> AssessmentSummary.Type.cAS1
        "temporary-accommodation" -> AssessmentSummary.Type.cAS3
        else -> throw RuntimeException("Unsupported type: ${ase.type}")
      },
      id = ase.id,
      applicationId = ase.applicationId,
      createdAt = ase.createdAt.toInstant(),
      arrivalDate = ase.arrivalDate?.toInstant(),
      status = when {
        ase.completed -> AssessmentStatus.completed
        ase.dateOfInfoRequest != null -> AssessmentStatus.awaitingResponse
        ase.isStarted -> AssessmentStatus.inProgress
        else -> AssessmentStatus.notStarted
      },
      decision = when (ase.decision) {
        "ACCEPTED" -> ApiAssessmentDecision.accepted
        "REJECTED" -> ApiAssessmentDecision.rejected
        else -> null
      },
      risks = ase.riskRatings?.let { risksTransformer.transformDomainToApi(objectMapper.readValue<PersonRisks>(it), ase.crn) },
      person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    )

  fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }

  private fun getStatus(entity: AssessmentEntity) = when {
    entity.decision !== null -> AssessmentStatus.completed
    entity.clarificationNotes.any { it.response == null } -> AssessmentStatus.awaitingResponse
    entity.reallocatedAt != null -> AssessmentStatus.reallocated
    entity.data != null -> AssessmentStatus.inProgress
    else -> AssessmentStatus.notStarted
  }
}
