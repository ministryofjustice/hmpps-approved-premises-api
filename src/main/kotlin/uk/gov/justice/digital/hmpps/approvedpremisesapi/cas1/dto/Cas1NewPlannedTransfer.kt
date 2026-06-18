package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Deprecated("This class will be removed soon")
data class Cas1NewPlannedTransfer(

  @get:JsonProperty("destinationPremisesId", required = true) val destinationPremisesId: UUID,

  @Schema(example = "null", required = true, description = "The expected arrival date for the new space booking. The existing space booking will be updated to end on this date")
  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

  @Schema(example = "null", required = true, description = "The expected departure date for the new space booking")
  @get:JsonProperty("departureDate", required = true) val departureDate: LocalDate,

  @get:JsonProperty("changeRequestId", required = true) val changeRequestId: UUID,

  @Schema(example = "null", description = "If not provided, it is assumed that no characteristics are required")
  @get:JsonProperty("characteristics") val characteristics: List<Cas1SpaceCharacteristic>? = null,
)
