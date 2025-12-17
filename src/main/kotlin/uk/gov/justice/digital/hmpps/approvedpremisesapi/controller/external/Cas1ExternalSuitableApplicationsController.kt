package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService

@Cas1ExternalController
class Cas1ExternalSuitableApplicationsController(
  private val cas1ApplicationService: Cas1ApplicationService,
) {
  @PreAuthorize("hasRole('ACCOMMODATION_API__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/suitable-application/{crn}")
  fun getSuitableApplicationsByCrn(@PathVariable crn: String): ResponseEntity<Cas1SuitableApplication> = cas1ApplicationService.getSuitableApplicationByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.notFound().build()
}
