package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewPlannedTransfer(

  val destinationPremisesId: java.util.UUID,

  @Schema(example = "null", required = true, description = "The expected arrival date for the new space booking. The existing space booking will be updated to end on this date")
  val arrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "The expected departure date for the new space booking")
  val departureDate: java.time.LocalDate,

  val changeRequestId: java.util.UUID,

  @Schema(example = "null", description = "If not provided, it is assumed that no characteristics are required")
  val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,
)
