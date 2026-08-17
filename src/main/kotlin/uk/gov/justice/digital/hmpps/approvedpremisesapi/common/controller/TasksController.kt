package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller.Cas1TasksController
import java.util.UUID

@RestController
class TasksController(
  private val cas1TasksController: Cas1TasksController,
) {
  @Operation(
    tags = ["Application data"],
    summary = "Gets a task for an application",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved task", content = [Content(schema = Schema(implementation = TaskWrapper::class))]),
    ],
  )
  @GetMapping(
    value = ["/tasks/{taskType}/{id}"],
    produces = ["application/json"],
  )
  @SuppressWarnings("UnusedParameter")
  fun tasksTaskTypeIdGet(
    @Parameter(description = "ID of the task")
    @PathVariable
    id: UUID,
    @Parameter(description = "Task type")
    @PathVariable
    taskType: String,
  ): ResponseEntity<TaskWrapper> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  @Operation(
    tags = ["Operations on applications"],
    summary = "Reallocates a task for an application",
    responses = [
      ApiResponse(responseCode = "201", description = "successful operation", content = [Content(schema = Schema(implementation = Reallocation::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @PostMapping(
    value = ["/tasks/{taskType}/{id}/allocations"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  @Deprecated("Superseded by Cas3AssessmentController.reallocateAssessment()")
  @Transactional
  fun tasksTaskTypeIdAllocationsPost(
    @Parameter(description = "ID of the task")
    @PathVariable
    id: UUID,
    @Parameter(description = "Task type")
    @PathVariable
    taskType: String,
    @Parameter(
      description = "Only assessments for this service will be returned",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader("X-Service-Name")
    xServiceName: ServiceName,
    @RequestBody body: NewReallocation?,
  ): ResponseEntity<Reallocation> = cas1TasksController.reallocateTask(id, taskType, xServiceName, body)

  @Operation(
    tags = ["Operations on applications"],
    summary = "Unallocates a task for an application",
    responses = [
      ApiResponse(responseCode = "204", description = "successful operation"),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @DeleteMapping(
    value = ["/tasks/{taskType}/{id}/allocations"],
    produces = ["application/problem+json"],
  )
  @Deprecated("Superseded by Cas3AssessmentController.deallocateAssessment()")
  @Transactional
  fun tasksTaskTypeIdAllocationsDelete(
    @Parameter(description = "ID of the task")
    @PathVariable
    id: UUID,
    @Parameter(description = "Task type")
    @PathVariable
    taskType: String,
  ): ResponseEntity<Unit> = cas1TasksController.unallocateTask(id, taskType)
}
