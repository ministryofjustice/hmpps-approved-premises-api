package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewArrival(

  @Schema(example = "null", description = "This is deprecated. Instead use arrivalDate and arrivalTime")
  @Deprecated(message = "")
  val arrivalDateTime: java.time.Instant? = null,

  val arrivalDate: java.time.LocalDate? = null,

  @Schema(example = "23:15", description = "")
  val arrivalTime: kotlin.String? = null,
)
