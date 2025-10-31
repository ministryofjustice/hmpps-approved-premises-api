package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.transaction.Transactional
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1TasksController
import java.util.UUID

@RestController
@RequestMapping(
  "\${api.base-path:}",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
class TasksController(
  private val cas1TasksController: Cas1TasksController,
) {

  @Operation(
    tags = ["Operations on applications"],
    summary = "Reallocates a task for an application",
    operationId = "tasksTaskTypeIdAllocationsPost",
    deprecated = true,
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Reallocation::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/tasks/{taskType}/{id}/allocations"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  @Deprecated("Superseded by Cas3AssessmentController.reallocateAssessment()")
  @Transactional
  fun tasksTaskTypeIdAllocationsPost(
    @PathVariable id: UUID,
    @PathVariable taskType: String,
    @RequestHeader xServiceName: ServiceName,
    @RequestBody body: NewReallocation?,
  ): ResponseEntity<Reallocation> = cas1TasksController.reallocateTask(id, taskType, xServiceName, body)

  @Operation(
    tags = ["Operations on applications"],
    summary = "Unallocates a task for an application",
    operationId = "tasksTaskTypeIdAllocationsDelete",
    description = """""",
    deprecated = true,
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation"),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.DELETE],
    value = ["/tasks/{taskType}/{id}/allocations"],
    produces = ["application/problem+json"],
  )
  @Deprecated("Superseded by Cas3AssessmentController.deallocateAssessment()")
  @Transactional
  fun tasksTaskTypeIdAllocationsDelete(@PathVariable id: UUID, @PathVariable taskType: String): ResponseEntity<Unit> = cas1TasksController.unallocateTask(id, taskType)
}
