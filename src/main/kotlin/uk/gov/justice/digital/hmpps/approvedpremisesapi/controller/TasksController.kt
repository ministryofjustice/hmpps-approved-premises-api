package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1TasksController
import java.util.UUID

@Service
class TasksController(
  private val cas1TasksController: Cas1TasksController,
) : TasksApiDelegate {

  @Deprecated("Superseded by Cas3AssessmentController.deallocateAssessment()")
  @Transactional
  override fun tasksTaskTypeIdAllocationsDelete(id: UUID, taskType: String): ResponseEntity<Unit> = cas1TasksController.unallocateTask(id, taskType)
}
