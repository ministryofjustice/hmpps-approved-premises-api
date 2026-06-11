package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationService

@Cas1ExternalController
class Cas1ExternalApplicationsController(
  private val cas1ExternalApplicationService: Cas1ExternalApplicationService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/applications/suitable")
  fun getSuitableApplicationsByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas1SuitableApplication> = cas1ExternalApplicationService.getSuitableApplicationByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.notFound().build()

  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/premises/current")
  fun getCurrentPremisesByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas1ExternalPremisesDto> = cas1ExternalApplicationService.getCurrentPremisesByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.notFound().build()
}
