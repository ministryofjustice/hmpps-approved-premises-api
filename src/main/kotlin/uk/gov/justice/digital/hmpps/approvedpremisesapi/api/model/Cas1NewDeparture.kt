package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewDeparture(

  val reasonId: java.util.UUID,

  @Schema(example = "null", description = "use seperate date/time fields")
  @Deprecated(message = "")
  val departureDateTime: java.time.Instant? = null,

  val departureDate: java.time.LocalDate? = null,

  @Schema(example = "23:15", description = "")
  val departureTime: kotlin.String? = null,

  val moveOnCategoryId: java.util.UUID? = null,

  val notes: kotlin.String? = null,
)
