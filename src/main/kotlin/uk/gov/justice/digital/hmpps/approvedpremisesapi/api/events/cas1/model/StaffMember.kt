package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class StaffMember(

  @field:Schema(example = "N54A999", required = true, description = "")
  @get:JsonProperty("staffCode", required = true) val staffCode: kotlin.String,

  @field:Schema(example = "John", required = true, description = "")
  @get:JsonProperty("forenames", required = true) val forenames: kotlin.String,

  @field:Schema(example = "Smith", required = true, description = "")
  @get:JsonProperty("surname", required = true) val surname: kotlin.String,

  @field:Schema(example = "JohnSmithNPS", description = "")
  @get:JsonProperty("username") val username: kotlin.String? = null,
)
