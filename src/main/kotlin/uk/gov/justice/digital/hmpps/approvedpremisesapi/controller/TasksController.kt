package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1TasksController
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Service
class TasksController(
  private val cas1TasksController: Cas1TasksController,
) : TasksApiDelegate {

  @SuppressWarnings("MaxLineLength")
  override fun tasksGet(
    type: TaskType?,
    types: List<TaskType>?,
    page: Int?,
    perPage: Int?,
    sortBy: TaskSortField?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
    apAreaId: UUID?,
    cruManagementAreaId: UUID?,
    allocatedToUserId: UUID?,
    requiredQualification: ApiUserQualification?,
    crnOrName: String?,
    isCompleted: Boolean?,
  ): ResponseEntity<List<Task>> = cas1TasksController.tasksGet(type, types, page, perPage, sortBy, sortDirection, allocatedFilter, apAreaId, cruManagementAreaId, allocatedToUserId, requiredQualification, crnOrName, isCompleted)

  override fun tasksTaskTypeIdGet(id: UUID, taskType: String): ResponseEntity<TaskWrapper> = cas1TasksController.tasksTaskTypeIdGet(id, taskType)

  @Transactional
  override fun tasksTaskTypeIdAllocationsPost(
    id: UUID,
    taskType: String,
    xServiceName: ServiceName,
    body: NewReallocation?,
  ): ResponseEntity<Reallocation> = cas1TasksController.tasksTaskTypeIdAllocationsPost(id, taskType, xServiceName, body)

  @Transactional
  override fun tasksTaskTypeIdAllocationsDelete(id: UUID, taskType: String): ResponseEntity<Unit> = cas1TasksController.tasksTaskTypeIdAllocationsDelete(id, taskType)
}
