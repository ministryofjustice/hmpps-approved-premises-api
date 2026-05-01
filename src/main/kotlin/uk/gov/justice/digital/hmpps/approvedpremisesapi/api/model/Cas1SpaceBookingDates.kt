package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceBookingDates(

  val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
  val canonicalArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
  val canonicalDepartureDate: java.time.LocalDate,
)
