package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService

@Cas1ExternalController
class Cas1ExternalApplicationsController(
  private val cas1ApplicationService: Cas1ApplicationService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/applications/{type}")
  fun getApplicationsByCrnAndType(
    @PathVariable crn: String,
    @PathVariable type: String,
  ): ResponseEntity<Cas1SuitableApplication> = when (type) {
    "suitable" -> cas1ApplicationService.getSuitableApplicationByCrn(crn)
      ?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity.notFound().build()
    else -> ResponseEntity.badRequest().build()
  }
}
