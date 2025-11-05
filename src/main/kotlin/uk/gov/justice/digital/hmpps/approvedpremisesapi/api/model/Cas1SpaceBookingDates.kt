package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceBookingDates(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @field:Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
  @get:JsonProperty("canonicalArrivalDate", required = true) val canonicalArrivalDate: java.time.LocalDate,

  @field:Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
  @get:JsonProperty("canonicalDepartureDate", required = true) val canonicalDepartureDate: java.time.LocalDate,
)
