package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewArrival(

  @Schema(example = "null", description = "This is deprecated. Instead use arrivalDate and arrivalTime")
  @Deprecated(message = "")
  @get:JsonProperty("arrivalDateTime") val arrivalDateTime: java.time.Instant? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

  @Schema(example = "23:15", description = "")
  @get:JsonProperty("arrivalTime") val arrivalTime: kotlin.String? = null,
)
