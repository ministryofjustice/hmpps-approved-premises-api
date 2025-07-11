package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param reference
 * @param characteristicIds
 * @param notes
 */
data class Cas3UpdateBedspace(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reference", required = true) val reference: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("characteristicIds", required = true) val characteristicIds: List<UUID>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: String? = null,
)
