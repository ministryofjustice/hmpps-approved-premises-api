package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param name
 * @param characteristicIds
 * @param notes
 * @param bedEndDate End date of the bed availability, open for availability if not specified.
 */
data class NewRoom(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("characteristicIds", required = true) val characteristicIds: kotlin.collections.List<java.util.UUID>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "Sat Mar 30 00:00:00 GMT 2024", description = "End date of the bed availability, open for availability if not specified.")
  @get:JsonProperty("bedEndDate") val bedEndDate: java.time.LocalDate? = null,
)
