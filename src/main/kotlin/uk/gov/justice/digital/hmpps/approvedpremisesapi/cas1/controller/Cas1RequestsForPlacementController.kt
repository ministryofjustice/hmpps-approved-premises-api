package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Requests for Placement Controller")
class Cas1RequestsForPlacementController(
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
) {

  @Operation(summary = "Get request for placement duration")
  @GetMapping(
    value = ["/applications/{applicationId}/requests-for-placement/calc/durations"],
  )
  @SuppressWarnings("UnusedParameter")
  fun getRequestForPlacementDuration(
    @PathVariable applicationId: UUID,
    @RequestParam("apType") apType: ApType,
    @RequestParam("sentenceType") sentenceType: SentenceTypeOption,
  ): ResponseEntity<Cas1RequestsForPlacementDurationsCalculationResponseDto> {
    val result = cas1RequestForPlacementService.defaultDurations(apType)

    return ResponseEntity.ok(extractEntityFromCasResult(result))
  }
}
