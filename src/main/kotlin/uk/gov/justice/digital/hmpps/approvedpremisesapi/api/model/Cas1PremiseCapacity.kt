package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param premisesId
 * @param startDate
 * @param endDate
 * @param capacity
 */
data class Cas1PremiseCapacity(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premisesId", required = true) val premisesId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("capacity", required = true) val capacity: kotlin.collections.List<Cas1PremiseCapacityForDay>,
)
