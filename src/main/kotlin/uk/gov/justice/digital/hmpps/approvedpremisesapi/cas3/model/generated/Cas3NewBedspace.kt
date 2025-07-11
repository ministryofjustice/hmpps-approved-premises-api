package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param reference
 * @param startDate Start date of the bedspace availability.
 * @param characteristicIds
 * @param notes
 */
data class Cas3NewBedspace(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reference", required = true) val reference: String,

  @Schema(
    example = "Sat Mar 30 00:00:00 GMT 2024",
    required = true,
    description = "Start date of the bedspace availability.",
  )
  @get:JsonProperty("startDate", required = true) val startDate: LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("characteristicIds", required = true) val characteristicIds: List<UUID>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: String? = null,
)
