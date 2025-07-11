package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param bookingId
 * @param dateTime
 * @param reason
 * @param moveOnCategory
 * @param createdAt
 * @param notes
 * @param destinationProvider
 */
data class Departure(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("dateTime", required = true) val dateTime: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: DepartureReason,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("moveOnCategory", required = true) val moveOnCategory: MoveOnCategory,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("destinationProvider") val destinationProvider: DestinationProvider? = null,
)
