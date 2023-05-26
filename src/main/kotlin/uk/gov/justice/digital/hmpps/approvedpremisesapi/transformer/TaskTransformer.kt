package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task.Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class TaskTransformer(
  private val personTransformer: PersonTransformer,
  private val userTransformer: UserTransformer,
) {
  fun transformAssessmentToTask(assessment: AssessmentEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = Task(
    applicationId = assessment.application.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    dueDate = assessment.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = userTransformer.transformJpaToApi(assessment.allocatedToUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
    status = getAssessmentStatus(assessment),
    taskType = TaskType.assessment,
  )

  fun transformPlacementRequestToTask(placementRequest: PlacementRequestEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = Task(
    applicationId = placementRequest.application.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    dueDate = placementRequest.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = userTransformer.transformJpaToApi(placementRequest.allocatedToUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
    status = getPlacementRequestStatus(placementRequest),
    taskType = TaskType.placementRequest,
  )

  private fun getAssessmentStatus(entity: AssessmentEntity): Status = when {
    entity.data.isNullOrEmpty() -> Status.notStarted
    entity.decision !== null -> Status.complete
    else -> Status.inProgress
  }

  private fun getPlacementRequestStatus(entity: PlacementRequestEntity): Status = when {
    entity.booking !== null -> Status.complete
    else -> Status.notStarted
  }
}
