package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class AssessmentTransformer(
  private val objectMapper: ObjectMapper,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val userTransformer: UserTransformer
) {
  fun transformJpaToApi(jpa: AssessmentEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = when (jpa.application) {
    is ApprovedPremisesApplicationEntity -> ApprovedPremisesAssessment(
      id = jpa.id,
      application = applicationsTransformer.transformJpaToApi(jpa.application, offenderDetailSummary, inmateDetail) as ApprovedPremisesApplication,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      allocatedAt = jpa.allocatedAt.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
      allocatedToStaffMember = userTransformer.transformJpaToApi(jpa.allocatedToUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
      submittedAt = jpa.submittedAt?.toInstant(),
      decision = transformJpaDecisionToApi(jpa.decision),
      rejectionRationale = jpa.rejectionRationale,
      status = getStatus(jpa)
    )
    is TemporaryAccommodationApplicationEntity -> TemporaryAccommodationAssessment(
      id = jpa.id,
      application = applicationsTransformer.transformJpaToApi(jpa.application, offenderDetailSummary, inmateDetail) as TemporaryAccommodationApplication,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      allocatedAt = jpa.allocatedAt.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
      allocatedToStaffMember = userTransformer.transformJpaToApi(jpa.allocatedToUser, ServiceName.temporaryAccommodation) as TemporaryAccommodationUser,
      submittedAt = jpa.submittedAt?.toInstant(),
      decision = transformJpaDecisionToApi(jpa.decision),
      rejectionRationale = jpa.rejectionRationale,
      status = getStatus(jpa)
    )
    else -> throw RuntimeException("Unsupported Application type when transforming Assessment: ${jpa.application::class.qualifiedName}")
  }

  private fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }

  private fun getStatus(entity: AssessmentEntity) = when {
    entity.decision !== null -> AssessmentStatus.completed
    entity.clarificationNotes.any { it.response == null } -> AssessmentStatus.awaitingResponse
    entity.reallocatedAt != null -> AssessmentStatus.reallocated
    else -> AssessmentStatus.active
  }
}
