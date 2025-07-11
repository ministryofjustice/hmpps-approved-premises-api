package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable

/**
 *
 * @param notes
 * @param withdrawables
 */
data class Withdrawables(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("notes", required = true) val notes: kotlin.collections.List<kotlin.String>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("withdrawables", required = true) val withdrawables: kotlin.collections.List<Withdrawable>,
)
