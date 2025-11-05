package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ActiveOffence(

  @field:Schema(example = "7", required = true, description = "")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @get:JsonProperty("offenceDescription", required = true) val offenceDescription: kotlin.String,

  @field:Schema(example = "M1502750438", required = true, description = "")
  @get:JsonProperty("offenceId", required = true) val offenceId: kotlin.String,

  @field:Schema(example = "1502724704", required = true, description = "")
  @get:JsonProperty("convictionId", required = true) val convictionId: kotlin.Long,

  @get:JsonProperty("offenceDate") val offenceDate: java.time.LocalDate? = null,
)
