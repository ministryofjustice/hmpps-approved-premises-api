package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Cas2v2ApplicationStatusDetail(

  @get:JsonProperty("id", required = true) val id: UUID,

  @field:Schema(example = "changeOfCircumstances", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @field:Schema(example = "Change of Circumstances", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: String,
)
