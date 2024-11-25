package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 */
class BedOccupancyOpenEntry(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: BedOccupancyEntryType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("length", required = true) override val length: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("startDate", required = true) override val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("endDate", required = true) override val endDate: java.time.LocalDate,
) : BedOccupancyEntry
