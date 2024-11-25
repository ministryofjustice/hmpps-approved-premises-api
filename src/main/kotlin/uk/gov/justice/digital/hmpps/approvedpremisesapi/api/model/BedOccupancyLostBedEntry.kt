package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param lostBedId
 */
data class BedOccupancyLostBedEntry(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("lostBedId", required = true) val lostBedId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: BedOccupancyEntryType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("length", required = true) override val length: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("startDate", required = true) override val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("endDate", required = true) override val endDate: java.time.LocalDate,
) : BedOccupancyEntry {
}
