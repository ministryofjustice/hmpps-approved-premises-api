package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJobService

@RestController
@Tag(name = "default")
class MigrationJobController(private val migrationJobService: MigrationJobService) {

  @Operation(
    summary = "Starts a migration job (process for data migrations that can't be achieved solely via SQL migrations), can only be called from a local connection",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @PostMapping(
    value = ["/migration-job"],
    consumes = ["application/json"],
  )
  fun migrationJobPost(
    @RequestBody migrationJobRequest: MigrationJobRequest,
  ): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    migrationJobService.runMigrationJobAsync(migrationJobRequest.jobType)

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
