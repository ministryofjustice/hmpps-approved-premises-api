package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ExternalUser(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @field:Schema(example = "CAS2_ASSESSOR_USER", required = true, description = "")
  @get:JsonProperty("username", required = true) val username: kotlin.String,

  @field:Schema(example = "Roger Smith", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @field:Schema(example = "roger@external.example.com", required = true, description = "")
  @get:JsonProperty("email", required = true) val email: kotlin.String,

  @field:Schema(example = "NACRO", description = "")
  @get:JsonProperty("origin") val origin: kotlin.String? = null,
)
