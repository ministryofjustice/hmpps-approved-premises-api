package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalCurrentApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalLatestBookingPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ApplicationService

@Cas3ExternalController
class Cas3ExternalApplicationsController(
  private val cas3ApplicationService: Cas3ApplicationService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/applications/suitable")
  @Deprecated("Use [getCurrentApplication]")
  fun getSuitableApplication(
    @PathVariable crn: String,
  ): ResponseEntity<Cas3ExternalCurrentApplicationDto> = cas3ApplicationService.getCurrentApplication(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.noContent().build()

  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/applications/current")
  fun getCurrentApplication(
    @PathVariable crn: String,
  ): ResponseEntity<Cas3ExternalCurrentApplicationDto> = cas3ApplicationService.getCurrentApplication(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.noContent().build()

  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/premises/current")
  fun getCurrentPremisesByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas3ExternalLatestBookingPremisesDto> = cas3ApplicationService.getCurrentPremisesByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.noContent().build()
}
