package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationService

@Cas2ExternalController
class Cas2ExternalApplicationsController(
  private val cas2ApplicationService: Cas2ApplicationService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/applications/suitable")
  fun getSuitableApplicationsByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2SuitableApplication> = cas2ApplicationService.getSuitableApplicationByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.noContent().build()
}
