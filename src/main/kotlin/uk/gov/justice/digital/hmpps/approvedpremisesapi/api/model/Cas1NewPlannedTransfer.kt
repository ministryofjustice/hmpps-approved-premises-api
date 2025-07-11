package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param destinationPremisesId
 * @param arrivalDate The expected arrival date for the new space booking. The existing space booking will be updated to end on this date
 * @param departureDate The expected departure date for the new space booking
 * @param changeRequestId
 * @param characteristics If not provided, it is assumed that no characteristics are required
 */
data class Cas1NewPlannedTransfer(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("destinationPremisesId", required = true) val destinationPremisesId: java.util.UUID,

  @Schema(example = "null", required = true, description = "The expected arrival date for the new space booking. The existing space booking will be updated to end on this date")
  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "The expected departure date for the new space booking")
  @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("changeRequestId", required = true) val changeRequestId: java.util.UUID,

  @Schema(example = "null", description = "If not provided, it is assumed that no characteristics are required")
  @get:JsonProperty("characteristics") val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,
)
