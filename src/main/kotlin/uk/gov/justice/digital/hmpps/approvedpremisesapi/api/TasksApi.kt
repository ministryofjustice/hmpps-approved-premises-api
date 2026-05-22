
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError

@RestController
interface TasksApi {

  fun getDelegate(): TasksApiDelegate = object : TasksApiDelegate {}

  @Operation(
    tags = ["Operations on applications"],
    summary = "Unallocates a task for an application",
    operationId = "tasksTaskTypeIdAllocationsDelete",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation"),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.DELETE],
    value = ["/tasks/{taskType}/{id}/allocations"],
    produces = ["application/problem+json"],
  )
  fun tasksTaskTypeIdAllocationsDelete(@Parameter(description = "ID of the task", required = true) @PathVariable("id") id: java.util.UUID, @Parameter(description = "Task type", required = true) @PathVariable("taskType") taskType: String): ResponseEntity<Unit> = getDelegate().tasksTaskTypeIdAllocationsDelete(id, taskType)
}
