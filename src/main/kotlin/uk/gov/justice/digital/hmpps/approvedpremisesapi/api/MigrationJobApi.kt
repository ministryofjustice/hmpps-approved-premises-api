
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobRequest

@RestController
interface MigrationJobApi {

  fun getDelegate(): MigrationJobApiDelegate = object : MigrationJobApiDelegate {}

  @Operation(
    tags = ["default"],
    summary = "Starts a migration job (process for data migrations that can't be achieved solely via SQL migrations), can only be called from a local connection",
    operationId = "migrationJobPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/migration-job"],
    consumes = ["application/json"],
  )
  fun migrationJobPost(@Parameter(description = "", required = true) @RequestBody migrationJobRequest: MigrationJobRequest): ResponseEntity<Unit> = getDelegate().migrationJobPost(migrationJobRequest)
}
