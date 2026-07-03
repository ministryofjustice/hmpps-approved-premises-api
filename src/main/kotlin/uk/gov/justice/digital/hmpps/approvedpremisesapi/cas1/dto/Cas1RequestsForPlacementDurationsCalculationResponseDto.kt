package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

data class Cas1RequestsForPlacementDurationsCalculationResponseDto(
  val defaultDurationDays: Int,
  val maxDurationDays: Int?,
)
