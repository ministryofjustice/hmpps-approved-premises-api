package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class LocalAuthorityArea(

  @field:Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @field:Schema(example = "LEEDS", required = true, description = "")
  @get:JsonProperty("identifier", required = true) val identifier: kotlin.String,

  @field:Schema(example = "Leeds City Council", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,
)
