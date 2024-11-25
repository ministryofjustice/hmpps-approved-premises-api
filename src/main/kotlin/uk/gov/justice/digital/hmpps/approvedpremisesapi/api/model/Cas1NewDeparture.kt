package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param departureDateTime
 * @param reasonId
 * @param moveOnCategoryId
 * @param notes
 */
data class Cas1NewDeparture(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("departureDateTime", required = true) val departureDateTime: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,

  @Schema(example = "null", description = "")
  @get:JsonProperty("moveOnCategoryId") val moveOnCategoryId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
