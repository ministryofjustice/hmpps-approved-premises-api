package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationRequestDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas1Controller
@Tag(name = "CAS1 Requests for Placement Controller")
class Cas1RequestsForPlacementController(
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
) {

  @Operation(summary = "Get request for placement duration")
  @PostMapping(
    value = ["/requests-for-placement/calc/durations"],
    produces = ["application/json"],
  )
  fun getRequestForPlacementDuration(
    @RequestBody requestDto: Cas1RequestsForPlacementDurationsCalculationRequestDto,
  ): ResponseEntity<Cas1RequestsForPlacementDurationsCalculationResponseDto> {
    val result = cas1RequestForPlacementService.defaultDurations(requestDto)

    return ResponseEntity.ok(extractEntityFromCasResult(result))
  }
}
