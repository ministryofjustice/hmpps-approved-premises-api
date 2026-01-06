package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ApplicationService

@Cas3ExternalController
class Cas3ExternalSuitableApplicationsController(
  private val cas3ApplicationService: Cas3ApplicationService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/suitable-application/{crn}")
  fun getSuitableApplicationsByCrn(@PathVariable crn: String): ResponseEntity<Cas3SuitableApplication> = cas3ApplicationService.getSuitableApplicationByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.notFound().build()
}
