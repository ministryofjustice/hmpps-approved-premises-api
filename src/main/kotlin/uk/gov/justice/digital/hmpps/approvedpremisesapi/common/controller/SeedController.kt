package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedService

@RestController
@Tag(name = "default")
class SeedController(private val seedService: SeedService) {

  @Operation(
    summary = "Starts the data seeding process, can only be called from a local connection",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @PostMapping(
    value = ["/seed"],
    consumes = ["application/json"],
  )
  fun seedPost(
    @RequestBody seedRequest: SeedRequest,
  ): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedService.seedDataAsync(seedRequest.seedType, seedRequest.fileName)

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
