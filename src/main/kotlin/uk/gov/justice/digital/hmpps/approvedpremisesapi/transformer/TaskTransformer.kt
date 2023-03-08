package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task.Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class TaskTransformer(
  private val personTransformer: PersonTransformer,
  private val userTransformer: UserTransformer
) {
  fun transformAssessmentToTask(assessment: AssessmentEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = Task(
    applicationId = assessment.application.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    dueDate = assessment.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = userTransformer.transformJpaToApi(assessment.allocatedToUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
    status = getStatus(assessment),
    taskType = TaskType.assessment
  )

  private fun getStatus(entity: AssessmentEntity): Status = when {
    entity.data.isNullOrEmpty() -> Status.notStarted
    entity.decision !== null -> Status.complete
    else -> Status.inProgress
  }
}
