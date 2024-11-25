package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param bedId
 * @param bedName
 * @param schedule
 */
data class BedOccupancyRange(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bedName", required = true) val bedName: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("schedule", required = true) val schedule: kotlin.collections.List<BedOccupancyEntry>,
)
