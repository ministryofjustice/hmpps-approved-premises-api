package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Room(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("characteristics", required = true) val characteristics: List<Characteristic>,

  @Schema(example = "NEABC-4", description = "")
  @get:JsonProperty("code") val code: String? = null,

  @get:JsonProperty("notes") val notes: String? = null,

  @get:JsonProperty("beds") val beds: List<Bed>? = null,
)
