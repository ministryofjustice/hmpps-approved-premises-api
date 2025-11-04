package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewPlannedTransfer(

  @get:JsonProperty("destinationPremisesId", required = true) val destinationPremisesId: java.util.UUID,

  @Schema(example = "null", required = true, description = "The expected arrival date for the new space booking. The existing space booking will be updated to end on this date")
  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "The expected departure date for the new space booking")
  @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

  @get:JsonProperty("changeRequestId", required = true) val changeRequestId: java.util.UUID,

  @Schema(example = "null", description = "If not provided, it is assumed that no characteristics are required")
  @get:JsonProperty("characteristics") val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,
)
