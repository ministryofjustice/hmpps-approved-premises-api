package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.SeedApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedService

@Service
class SeedController(private val seedService: SeedService) : SeedApiDelegate {
  override fun seedPost(seedRequest: SeedRequest): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedService.seedDataAsync(seedRequest.seedType, seedRequest.fileName)

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
