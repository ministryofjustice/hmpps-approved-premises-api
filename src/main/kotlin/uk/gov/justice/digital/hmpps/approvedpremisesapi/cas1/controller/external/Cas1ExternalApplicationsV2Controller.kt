package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationV2Service

@Cas1ExternalController
class Cas1ExternalApplicationsV2Controller(
  private val cas1ExternalApplicationV2Service: Cas1ExternalApplicationV2Service,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/applications/current")
  fun getCurrentApplicationByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas1ExternalApplicationV2Dto> = cas1ExternalApplicationV2Service.getSuitableApplicationByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.noContent().build()
}
