package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1RequestsForPlacementDurationsCalculationResponseDto(
  @Schema(description = "If null, a default duration cannot be calculated")
  val defaultDurationDays: Int?,
  val maxDurationDays: Int?,
)
