package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Cas2v2StatusUpdateDetail(

  @get:JsonProperty("id", required = true) val id: UUID,

  @field:Schema(example = "moreInfoRequested", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @field:Schema(example = "More information requested", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: String,
)
