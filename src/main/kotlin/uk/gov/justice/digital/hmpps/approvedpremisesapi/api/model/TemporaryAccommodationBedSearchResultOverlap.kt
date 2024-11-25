package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param name
 * @param crn
 * @param days
 * @param bookingId
 * @param roomId
 * @param sex
 * @param assessmentId
 */
data class TemporaryAccommodationBedSearchResultOverlap(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("days", required = true) val days: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("roomId", required = true) val roomId: java.util.UUID,

  @Schema(example = "null", description = "")
  @get:JsonProperty("sex") val sex: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,
)
