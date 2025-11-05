package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas2StatusDetail(

  @field:Schema(example = "changeOfCircumstances", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @field:Schema(example = "Change of Circumstances", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: kotlin.String,
)
