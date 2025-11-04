package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class DestinationPremises(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "New Place", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "NENEW1", required = true, description = "")
  @get:JsonProperty("apCode", required = true) val apCode: kotlin.String,

  @Schema(example = "Q061", required = true, description = "")
  @get:JsonProperty("legacyApCode", required = true) val legacyApCode: kotlin.String,

  @get:JsonProperty("probationArea", required = true) val probationArea: ProbationArea,
)
