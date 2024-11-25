package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param section
 * @param name
 * @param linkedToHarm
 * @param linkedToReOffending
 */
data class OASysSection(

  @Schema(example = "10", required = true, description = "")
  @get:JsonProperty("section", required = true) val section: kotlin.Int,

  @Schema(example = "Emotional wellbeing", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("linkedToHarm") val linkedToHarm: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("linkedToReOffending") val linkedToReOffending: kotlin.Boolean? = null,
)
