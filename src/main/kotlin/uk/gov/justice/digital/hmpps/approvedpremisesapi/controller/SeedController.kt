package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.SeedApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.UnauthenticatedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SeedService

@Service
class SeedController(private val seedService: SeedService) : SeedApiDelegate {
  override fun seedPost(seedRequest: SeedRequest): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedService.seedData(seedRequest.seedType, seedRequest.fileName)

    return ResponseEntity(HttpStatus.ACCEPTED)
  }

  private fun throwIfNotLoopbackRequest() {
    val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
    val remoteAddress = request.remoteAddr

    if (! listOf("127.0.0.1", "localhost").contains(remoteAddress)) {
      throw UnauthenticatedProblem("This endpoint can only be called locally, was requested from: $remoteAddress")
    }
  }
}
