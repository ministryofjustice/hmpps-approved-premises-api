package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

data class Cas1NewArrival(

  @Schema(example = "null", description = "This is deprecated. Instead use arrivalDate and arrivalTime")
  @Deprecated(message = "")
  @get:JsonProperty("arrivalDateTime") val arrivalDateTime: Instant? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: LocalDate? = null,

  @Schema(example = "23:15", description = "")
  @get:JsonProperty("arrivalTime") val arrivalTime: String? = null,
)
