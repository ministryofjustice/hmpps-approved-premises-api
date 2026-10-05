package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller.external

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ArrivalDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external.Cas2ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external.Cas2ExternalDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.util.UUID

@Cas2ExternalController
class Cas2ExternalApplicationsController(
  private val cas2ExternalApplicationService: Cas2ExternalApplicationService,
  private val cas2ExternalDomainEventService: Cas2ExternalDomainEventService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/cases/{crn}/applications/suitable")
  fun getSuitableApplicationsByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2SuitableApplication> = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.noContent().build()

  @Operation(summary = "Create a new arrival")
  @PostMapping("/cases/{crn}/applications/{applicationId}/arrival")
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  fun createArrival(
    @PathVariable crn: String,
    @PathVariable applicationId: UUID,
    @RequestBody body: Cas2ArrivalDto,
  ): ResponseEntity<Unit> {
    val casResult = cas2ExternalApplicationService.recordArrival(crn, applicationId, body.arrivedByUsername, body.arrivalDateTime)

    ensureEntityFromCasResultIsSuccess(casResult)

    return ResponseEntity(HttpStatus.CREATED)
  }
}
