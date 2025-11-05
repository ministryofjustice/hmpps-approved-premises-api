package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class DestinationProvider(

  @field:Schema(example = "Ext - North East Region", required = true, description = "")
  @get:JsonProperty("description", required = true) val description: kotlin.String,

  @field:Schema(example = "f0703382-3e8f-49ff-82bc-b970c9fe1b35", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,
)
