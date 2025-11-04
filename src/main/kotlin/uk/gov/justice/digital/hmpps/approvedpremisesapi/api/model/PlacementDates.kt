package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PlacementDates(

  @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

  @get:JsonProperty("duration", required = true) val duration: kotlin.Int,
)
